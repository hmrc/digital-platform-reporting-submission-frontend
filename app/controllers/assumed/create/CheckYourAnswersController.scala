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
import connectors.SubmissionConnector
import controllers.actions.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.UserAnswersService
import services.UserAnswersService.BuildAssumedReportingSubmissionRequestFailure
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.assumed.create.*
import viewmodels.govuk.summarylist.*
import views.html.assumed.create.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalActionProvider,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            userAnswersService: UserAnswersService,
                                            submissionConnector: SubmissionConnector
                                          )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(operatorId) andThen requireData) {
    implicit request =>

      val list = SummaryListViewModel(
        rows = Seq(
          AssumingOperatorNameSummary.row(request.userAnswers),
          HasTaxIdentifierSummary.row(request.userAnswers),
          TaxResidentInUkSummary.row(request.userAnswers),
          UkTaxIdentifiersSummary.row(request.userAnswers),
          UtrSummary.row(request.userAnswers),
          CrnSummary.row(request.userAnswers),
          VrnSummary.row(request.userAnswers),
          EmprefSummary.row(request.userAnswers),
          ChrnSummary.row(request.userAnswers),
          TaxResidencyCountrySummary.row(request.userAnswers),
          InternationalTaxIdentifierSummary.row(request.userAnswers),
          RegisteredInUkSummary.row(request.userAnswers),
          UkAddressSummary.row(request.userAnswers),
          InternationalAddressSummary.row(request.userAnswers),
          ReportingPeriodSummary.row(request.userAnswers),
        ).flatten
      )

      Ok(view(list, operatorId))
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen getData(operatorId) andThen requireData).async {
    implicit request =>

      userAnswersService.toAssumedReportingSubmissionRequest(request.userAnswers)
        .map(Future.successful)
        .left.map(errors => Future.failed(BuildAssumedReportingSubmissionRequestFailure(errors)))
        .merge
        .flatMap { submissionRequest =>
          submissionConnector.submitAssumedReporting(submissionRequest).map { submission =>
            Redirect(routes.SubmissionConfirmationController.onPageLoad(operatorId, submission._id))
          }
        }
  }
}