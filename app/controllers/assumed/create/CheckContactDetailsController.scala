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

package controllers.assumed.create

import com.google.inject.Inject
import connectors.SubscriptionConnector
import controllers.actions.*
import forms.CheckContactDetailsFormProvider
import models.NormalMode
import models.confirmed.ConfirmedDetails
import models.subscription.{IndividualContact, OrganisationContact, SubscriptionInfo}
import pages.assumed.create.CheckContactDetailsPage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import services.ConfirmedDetailsService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.subscription.*
import views.html.assumed.create.CheckContactDetailsView

import scala.concurrent.{ExecutionContext, Future}

class CheckContactDetailsController @Inject()(override val messagesApi: MessagesApi,
                                              identify: IdentifierAction,
                                              getData: DataRetrievalActionProvider,
                                              requireData: DataRequiredAction,
                                              checkSubmissionsAllowed: CheckSubmissionsAllowedAction,
                                              val controllerComponents: MessagesControllerComponents,
                                              formProvider: CheckContactDetailsFormProvider,
                                              page: CheckContactDetailsPage,
                                              view: CheckContactDetailsView,
                                              subscriptionConnector: SubscriptionConnector,
                                              sessionRepository: SessionRepository,
                                              confirmedDetailsService: ConfirmedDetailsService)
                                             (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen checkSubmissionsAllowed andThen getData(operatorId) andThen requireData).async {
    implicit request =>
      subscriptionConnector.getSubscription.map { subscriptionInfo =>
        val list = summaryList(subscriptionInfo)
        val form = formProvider()
        Ok(view(form, list, operatorId))
      }
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen checkSubmissionsAllowed andThen getData(operatorId) andThen requireData).async { implicit request =>
    formProvider().bindFromRequest().fold(
      formWithErrors => {
        subscriptionConnector.getSubscription.map { subscriptionInfo =>
          val list = summaryList(subscriptionInfo)
          BadRequest(view(formWithErrors, list, operatorId))
        }
      },
      answer => (for {
        updatedAnswers <- Future.fromTry(request.userAnswers.set(page, answer))
        _ <- sessionRepository.set(updatedAnswers)
      } yield {
        if (answer) nextPage(operatorId)
        else Future.successful(page.nextPage(NormalMode, updatedAnswers))
      }).flatten.map(Redirect)
    )
  }

  private def nextPage(operatorId: String)(using HeaderCarrier): Future[Call] = confirmedDetailsService.confirmContactDetailsFor(operatorId).map {
    case ConfirmedDetails(true, true, true) => routes.ReportingPeriodController.onPageLoad(NormalMode, operatorId)
    case ConfirmedDetails(true, true, false) => routes.CheckContactDetailsController.onPageLoad(operatorId)
    case ConfirmedDetails(true, false, _) => routes.CheckReportingNotificationsController.onSubmit(operatorId)
    case ConfirmedDetails(false, _, _) => routes.CheckPlatformOperatorController.onPageLoad(operatorId)
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