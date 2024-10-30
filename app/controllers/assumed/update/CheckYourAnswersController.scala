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
import connectors.AssumedReportingConnector
import controllers.actions.*
import controllers.routes as baseRoutes
import models.UserAnswers
import models.submission.AssumedReportSummary
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.AssumedReportSummaryQuery
import repositories.SessionRepository
import services.UserAnswersService
import services.UserAnswersService.BuildAssumedReportingSubmissionFailure
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.assumed.update.*
import viewmodels.govuk.summarylist.*
import views.html.assumed.update.CheckYourAnswersView

import java.time.Year
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalActionProvider,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            userAnswersService: UserAnswersService,
                                            connector: AssumedReportingConnector,
                                            sessionRepository: SessionRepository
                                          )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String, reportingPeriod: Year): Action[AnyContent] =
    (identify andThen getData(operatorId, Some(reportingPeriod)) andThen requireData) {
      implicit request =>
  
        val list = SummaryListViewModel(
          rows = Seq(
            ReportingPeriodSummary.row(reportingPeriod, request.userAnswers),
            PlatformOperatorSummary.row(reportingPeriod, request.userAnswers),
            AssumingOperatorNameSummary.row(reportingPeriod, request.userAnswers),
            TaxResidentInUkSummary.row(reportingPeriod, request.userAnswers),
            HasUkTaxIdentifierSummary.row(reportingPeriod, request.userAnswers),
            UkTaxIdentifierSummary.row(reportingPeriod, request.userAnswers),
            TaxResidencyCountrySummary.row(reportingPeriod, request.userAnswers),
            HasInternationalTaxIdentifierSummary.row(reportingPeriod, request.userAnswers),
            InternationalTaxIdentifierSummary.row(reportingPeriod, request.userAnswers),
            RegisteredCountrySummary.row(reportingPeriod, request.userAnswers),
            AddressSummary.row(reportingPeriod, request.userAnswers),
          ).flatten
        )
  
        Ok(view(list, operatorId, reportingPeriod))
    }

  def onSubmit(operatorId: String, reportingPeriod: Year): Action[AnyContent] =
    (identify andThen getData(operatorId, Some(reportingPeriod)) andThen requireData).async {
      implicit request =>
  
        userAnswersService.toAssumedReportingSubmission(request.userAnswers)
          .map(Future.successful)
          .left.map(errors => Future.failed(BuildAssumedReportingSubmissionFailure(errors)))
          .merge
          .flatMap { submissionRequest =>
            for {
              submission   <- connector.submit(submissionRequest)
              summary      <- AssumedReportSummary(request.userAnswers).map(Future.successful).getOrElse(Future.failed(Exception("unable to build an assumed report summary")))
              emptyAnswers = UserAnswers(request.userId, operatorId, Some(reportingPeriod))
              answers      <- Future.fromTry(emptyAnswers.set(AssumedReportSummaryQuery, summary))
              _            <- sessionRepository.set(answers)
            } yield Redirect(routes.SubmissionConfirmationController.onPageLoad(operatorId, reportingPeriod))
          }
    }

  def initialise(operatorId: String, reportingPeriod: Year): Action[AnyContent] = identify.async {
    implicit request =>
      connector.get(operatorId, reportingPeriod)
        .flatMap(_.map { submission =>
          for {
            userAnswers <- Future.fromTry(userAnswersService.fromAssumedReportingSubmission(request.userId, submission))
            _           <- sessionRepository.set(userAnswers)
          } yield Redirect(routes.CheckYourAnswersController.onPageLoad(operatorId, reportingPeriod))
        }.getOrElse(Future.successful(Redirect(baseRoutes.JourneyRecoveryController.onPageLoad()))))
  }
}
