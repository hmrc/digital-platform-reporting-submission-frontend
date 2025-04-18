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
import connectors.PlatformOperatorConnector
import controllers.actions.*
import forms.CheckPlatformOperatorFormProvider
import models.confirmed.ConfirmedDetails
import models.operator.responses.PlatformOperator
import models.{CountriesList, NormalMode}
import pages.assumed.create.CheckPlatformOperatorPage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import services.ConfirmedDetailsService
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.operator.*
import viewmodels.govuk.summarylist.*
import views.html.assumed.create.CheckPlatformOperatorView

import scala.concurrent.{ExecutionContext, Future}

class CheckPlatformOperatorController @Inject()(
                                                 override val messagesApi: MessagesApi,
                                                 identify: IdentifierAction,
                                                 getData: DataRetrievalActionProvider,
                                                 assumedSubmissionSentCheck: AssumedSubmissionSentCheckAction,
                                                 requireData: DataRequiredAction,
                                                 checkAssumedReportingAllowed: CheckAssumedReportingAllowedAction,
                                                 val controllerComponents: MessagesControllerComponents,
                                                 connector: PlatformOperatorConnector,
                                                 formProvider: CheckPlatformOperatorFormProvider,
                                                 sessionRepository: SessionRepository,
                                                 confirmedDetailsService: ConfirmedDetailsService,
                                                 checkPlatformOperatorPage: CheckPlatformOperatorPage,
                                                 view: CheckPlatformOperatorView,
                                                 countriesList: CountriesList
                                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen checkAssumedReportingAllowed andThen getData(operatorId) andThen requireData andThen assumedSubmissionSentCheck).async { implicit request =>
    connector.viewPlatformOperator(operatorId).map { operator =>

      val form = formProvider()

      val preparedForm = request.userAnswers.get(checkPlatformOperatorPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }
      
      Ok(view(
        preparedForm,
        platformOperatorList(operator),
        primaryContactList(operator),
        secondaryContactList(operator),
        operator.operatorId,
        operator.operatorName
      ))
    }
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen checkAssumedReportingAllowed andThen getData(operatorId) andThen requireData).async { implicit request =>
    formProvider().bindFromRequest().fold(
      formWithErrors => {
        connector.viewPlatformOperator(operatorId).map { operator =>
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
      answer => (for {
        updatedAnswers <- Future.fromTry(request.userAnswers.set(checkPlatformOperatorPage, answer))
        _ <- sessionRepository.set(updatedAnswers)
      } yield {
        if (answer) nextPage(operatorId)
        else Future.successful(checkPlatformOperatorPage.nextPage(NormalMode, updatedAnswers))
      }).flatten.map(Redirect)
    )
  }

  private def nextPage(operatorId: String)(using HeaderCarrier): Future[Call] = confirmedDetailsService.confirmBusinessDetailsFor(operatorId).map {
    case ConfirmedDetails(true, true, true) => routes.ReportingPeriodController.onPageLoad(NormalMode, operatorId)
    case ConfirmedDetails(true, true, false) => routes.CheckContactDetailsController.onPageLoad(operatorId)
    case ConfirmedDetails(true, false, _) => routes.CheckReportingNotificationsController.onSubmit(operatorId)
    case ConfirmedDetails(false, _, _) => routes.CheckPlatformOperatorController.onPageLoad(operatorId)
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
