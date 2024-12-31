/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.submission

import config.FrontendAppConfig
import connectors.{SubmissionConnector, SubscriptionConnector}
import controllers.AnswerExtractor
import controllers.actions.*
import forms.CheckContactDetailsFormProvider
import models.confirmed.ConfirmedDetails
import models.subscription.{IndividualContact, OrganisationContact, SubscriptionInfo}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PlatformOperatorSummaryQuery
import services.ConfirmedDetailsService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.subscription.*
import views.html.submission.CheckContactDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckContactDetailsController @Inject()(override val messagesApi: MessagesApi,
                                              identify: IdentifierAction,
                                              getData: DataRetrievalActionProvider,
                                              requireData: DataRequiredAction,
                                              checkSubmissionsAllowed: CheckSubmissionsAllowedAction,
                                              val controllerComponents: MessagesControllerComponents,
                                              formProvider: CheckContactDetailsFormProvider,
                                              view: CheckContactDetailsView,
                                              connector: SubscriptionConnector,
                                              submissionConnector: SubmissionConnector,
                                              confirmedDetailsService: ConfirmedDetailsService,
                                              appConfig: FrontendAppConfig)
                                             (implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor with Logging {

  def onPageLoad(operatorId: String): Action[AnyContent] =
    (identify andThen checkSubmissionsAllowed andThen getData(operatorId) andThen requireData).async { implicit request =>
      connector.getSubscription.map { subscriptionInfo =>
        val list = summaryList(subscriptionInfo)
        val form = formProvider()
        Ok(view(form, list, operatorId))
      }
    }

  def onSubmit(operatorId: String): Action[AnyContent] =
    (identify andThen checkSubmissionsAllowed andThen getData(operatorId) andThen requireData).async { implicit request =>
      getAnswerAsync(PlatformOperatorSummaryQuery) { summary =>
        formProvider().bindFromRequest().fold(
          formWithErrors => {
            connector.getSubscription.map { subscriptionInfo =>
              val list = summaryList(subscriptionInfo)
              BadRequest(view(formWithErrors, list, operatorId))
            }
          },
          answer => if (answer) {
            confirmedDetailsService.confirmContactDetailsFor(operatorId).flatMap {
              case ConfirmedDetails(true, true, true) =>
                submissionConnector.start(operatorId, summary.operatorName, None).map { submission =>
                  Redirect(routes.UploadController.onPageLoad(operatorId, submission._id))
                }
              case ConfirmedDetails(true, true, false) => Future.successful(Redirect(routes.CheckContactDetailsController.onPageLoad(operatorId)))
              case ConfirmedDetails(true, false, _) => Future.successful(Redirect(routes.CheckReportingNotificationsController.onSubmit(operatorId)))
              case ConfirmedDetails(false, _, _) => Future.successful(Redirect(routes.CheckPlatformOperatorController.onPageLoad(operatorId)))
            }
          }
          else {
            Future.successful(Redirect(appConfig.manageHomepageUrl))
          }
        )
      }
    }

  private def summaryList(subscription: SubscriptionInfo)(implicit messages: Messages): SummaryList = {
    subscription.primaryContact match {
      case primaryContact: OrganisationContact =>
        SummaryList(rows =
          Seq(
            PrimaryContactNameSummary.row(primaryContact),
            PrimaryContactEmailSummary.row(primaryContact),
            CanPhonePrimaryContactSummary.row(primaryContact),
            PrimaryContactPhoneSummary.row(primaryContact),
            HasSecondaryContactSummary.row(subscription.secondaryContact),
            SecondaryContactNameSummary.row(subscription.secondaryContact),
            SecondaryContactEmailSummary.row(subscription.secondaryContact),
            CanPhoneSecondaryContactSummary.row(subscription.secondaryContact),
            SecondaryContactPhoneSummary.row(subscription.secondaryContact)
          ).flatten
        )

      case contact: IndividualContact =>
        SummaryList(rows =
          Seq(
            IndividualEmailSummary.row(contact),
            CanPhoneIndividualSummary.row(contact),
            IndividualPhoneNumberSummary.row(contact)
          ).flatten
        )
    }
  }
}
