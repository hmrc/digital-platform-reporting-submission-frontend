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
import connectors.{PlatformOperatorConnector, SubmissionConnector}
import controllers.AnswerExtractor
import controllers.actions.*
import forms.CheckPlatformOperatorFormProvider
import models.CountriesList
import models.confirmed.ConfirmedDetails
import models.operator.responses.PlatformOperator
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PlatformOperatorSummaryQuery
import services.ConfirmedDetailsService
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.PlatformOperatorSummary
import viewmodels.checkAnswers.operator.*
import viewmodels.govuk.summarylist.*
import views.html.submission.CheckPlatformOperatorView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckPlatformOperatorController @Inject()(override val messagesApi: MessagesApi,
                                                identify: IdentifierAction,
                                                getData: DataRetrievalActionProvider,
                                                requireData: DataRequiredAction,
                                                checkSubmissionsAllowed: CheckSubmissionsAllowedAction,
                                                val controllerComponents: MessagesControllerComponents,
                                                platformOperatorConnector: PlatformOperatorConnector,
                                                submissionConnector: SubmissionConnector,
                                                confirmedDetailsService: ConfirmedDetailsService,
                                                formProvider: CheckPlatformOperatorFormProvider,
                                                view: CheckPlatformOperatorView,
                                                appConfig: FrontendAppConfig,
                                                countriesList: CountriesList)
                                               (implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String): Action[AnyContent] =
    (identify andThen checkSubmissionsAllowed andThen getData(operatorId) andThen requireData).async { implicit request =>
      platformOperatorConnector.viewPlatformOperator(operatorId).map { operator =>
        Ok(view(
          formProvider(),
          platformOperatorList(operator),
          primaryContactList(operator),
          secondaryContactList(operator),
          operator.operatorId,
          operator.operatorName
        ))
      }
    }

  def onSubmit(operatorId: String): Action[AnyContent] =
    (identify andThen checkSubmissionsAllowed andThen getData(operatorId) andThen requireData).async { implicit request =>
      formProvider().bindFromRequest().fold(
        formWithErrors => {
          platformOperatorConnector.viewPlatformOperator(operatorId).map { operator =>
            BadRequest(view(
              formWithErrors,
              platformOperatorList(operator),
              primaryContactList(operator),
              secondaryContactList(operator),
              operator.operatorId,
              operator.operatorName
            ))
          }
        },
        answer => if (answer) {
          confirmedDetailsService.confirmBusinessDetailsFor(operatorId).flatMap {
            case ConfirmedDetails(true, true, true) => getAnswerAsync(PlatformOperatorSummaryQuery) { summary =>
              submissionConnector.start(operatorId, summary.operatorName, None).map { submission =>
                Redirect(routes.UploadController.onPageLoad(operatorId, submission._id))
              }
            }
            case ConfirmedDetails(true, true, false) => Future.successful(Redirect(routes.CheckContactDetailsController.onPageLoad(operatorId)))
            case ConfirmedDetails(true, false, _) => Future.successful(Redirect(routes.CheckReportingNotificationsController.onSubmit(operatorId)))
            case ConfirmedDetails(false, _, _) => Future.successful(Redirect(routes.CheckPlatformOperatorController.onPageLoad(operatorId)))
          }
        } else {
          Future.successful(Redirect(appConfig.updateOperatorUrl(operatorId)))
        }
      )
    }

  private def platformOperatorList(operator: PlatformOperator)(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        OperatorIdSummary.row(operator),
        BusinessNameSummary.row(operator),
        HasTradingNameSummary.row(operator),
        TradingNameSummary.row(operator),
        HasTaxIdentifierSummary.row(operator),
        TaxResidentInUkSummary.row(operator),
        UkTaxIdentifiersSummary.row(operator),
        UtrSummary.row(operator),
        CrnSummary.row(operator),
        VrnSummary.row(operator),
        EmprefSummary.row(operator),
        ChrnSummary.row(operator),
        TaxResidencyCountrySummary.row(operator, countriesList),
        InternationalTaxIdentifierSummary.row(operator),
        RegisteredInUkSummary.row(operator, countriesList),
        AddressSummary.row(operator, countriesList)
      ).flatten
    )

  private def primaryContactList(operator: PlatformOperator)(implicit messages: Messages): SummaryList =
    if (operator.secondaryContactDetails.isEmpty) {
      SummaryListViewModel(
        rows = Seq(
          PrimaryContactNameSummary.row(operator),
          PrimaryContactEmailSummary.row(operator),
          CanPhonePrimaryContactSummary.row(operator),
          PrimaryContactPhoneSummary.row(operator),
          HasSecondaryContactSummary.row(operator)
        ).flatten
      )
    } else {
      SummaryListViewModel(
        rows = Seq(
          PrimaryContactNameSummary.row(operator),
          PrimaryContactEmailSummary.row(operator),
          CanPhonePrimaryContactSummary.row(operator),
          PrimaryContactPhoneSummary.row(operator)
        ).flatten
      )
    }

  private def secondaryContactList(operator: PlatformOperator)(implicit messages: Messages): Option[SummaryList] =
    if (operator.secondaryContactDetails.isDefined) {
      Some(SummaryListViewModel(
        rows = Seq(
          HasSecondaryContactSummary.row(operator),
          SecondaryContactNameSummary.row(operator),
          SecondaryContactEmailSummary.row(operator),
          CanPhoneSecondaryContactSummary.row(operator),
          SecondaryContactPhoneSummary.row(operator),
        ).flatten
      ))
    } else {
      None
    }
}
