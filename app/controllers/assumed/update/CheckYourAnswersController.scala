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

package controllers.assumed.update

import com.google.inject.Inject
import connectors.SubmissionConnector
import controllers.actions.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.UserAnswersService
import services.UserAnswersService.BuildAssumedReportingSubmissionRequestFailure
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.assumed.update.*
import viewmodels.govuk.summarylist.*
import views.html.assumed.update.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalActionProvider,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            userAnswersService: UserAnswersService,
                                            submissionConnector: SubmissionConnector,
                                            sessionRepository: SessionRepository
                                          )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String, caseId: String): Action[AnyContent] =
    (identify andThen getData(operatorId, Some(caseId)) andThen requireData) {
      implicit request =>
  
        val list = SummaryListViewModel(
          rows = Seq(
            AssumingOperatorNameSummary.row(caseId, request.userAnswers),
            TaxResidentInUkSummary.row(caseId, request.userAnswers),
            HasUkTaxIdentifierSummary.row(caseId, request.userAnswers),
            UkTaxIdentifiersSummary.row(caseId, request.userAnswers),
            UtrSummary.row(caseId, request.userAnswers),
            CrnSummary.row(caseId, request.userAnswers),
            VrnSummary.row(caseId, request.userAnswers),
            EmprefSummary.row(caseId, request.userAnswers),
            ChrnSummary.row(caseId, request.userAnswers),
            TaxResidencyCountrySummary.row(caseId, request.userAnswers),
            HasInternationalTaxIdentifierSummary.row(caseId, request.userAnswers),
            InternationalTaxIdentifierSummary.row(caseId, request.userAnswers),
            RegisteredInUkSummary.row(caseId, request.userAnswers),
            UkAddressSummary.row(caseId, request.userAnswers),
            InternationalAddressSummary.row(caseId, request.userAnswers),
            ReportingPeriodSummary.row(caseId, request.userAnswers),
          ).flatten
        )
  
        Ok(view(list, operatorId, caseId))
    }

  def onSubmit(operatorId: String, caseId: String): Action[AnyContent] =
    (identify andThen getData(operatorId, Some(caseId)) andThen requireData).async {
      implicit request =>
  
        userAnswersService.toAssumedReportingSubmissionRequest(request.userAnswers)
          .map(Future.successful)
          .left.map(errors => Future.failed(BuildAssumedReportingSubmissionRequestFailure(errors)))
          .merge
          .flatMap { submissionRequest =>
            for {
              submission <- submissionConnector.submitAssumedReporting(submissionRequest)
              _          <- sessionRepository.clear(request.userId, operatorId, Some(caseId))
            } yield Redirect(routes.SubmissionConfirmationController.onPageLoad(operatorId, submission._id))
          }
    }
}