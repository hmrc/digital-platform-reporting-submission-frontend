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
import connectors.AssumedReportingConnector
import connectors.AssumedReportingConnector.SubmitAssumedReportingFailure
import controllers.AnswerExtractor
import controllers.actions.*
import models.{CountriesList, UserAnswers}
import models.audit.AddAssumedReportEvent
import models.requests.DataRequest
import models.submission.{AssumedReportSummary, AssumedReportingSubmissionRequest, Submission}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{AssumedReportSummaryQuery, PlatformOperatorSummaryQuery}
import repositories.SessionRepository
import services.{AuditService, UserAnswersService}
import services.UserAnswersService.BuildAssumedReportingSubmissionFailure
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.assumed.create.*
import viewmodels.govuk.summarylist.*
import viewmodels.PlatformOperatorSummary
import views.html.assumed.create.CheckYourAnswersView

import java.time.{Clock, Instant}
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalActionProvider,
                                            requireData: DataRequiredAction,
                                            checkSubmissionsAllowed: CheckSubmissionsAllowedAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            userAnswersService: UserAnswersService,
                                            connector: AssumedReportingConnector,
                                            sessionRepository: SessionRepository,
                                            auditService: AuditService,
                                            clock: Clock,
                                            countriesList: CountriesList
                                          )(using ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen checkSubmissionsAllowed andThen getData(operatorId) andThen requireData) {
    implicit request =>

      val list = SummaryListViewModel(
        rows = Seq(
          ReportingPeriodSummary.row(request.userAnswers),
          AssumingOperatorNameSummary.row(request.userAnswers),
          TaxResidentInUkSummary.row(request.userAnswers),
          HasUkTaxIdentifierSummary.row(request.userAnswers),
          UkTaxIdentifierSummary.row(request.userAnswers),
          TaxResidencyCountrySummary.row(request.userAnswers),
          HasInternationalTaxIdentifierSummary.row(request.userAnswers),
          InternationalTaxIdentifierSummary.row(request.userAnswers),
          RegisteredCountrySummary.row(request.userAnswers),
          AddressSummary.row(request.userAnswers),
        ).flatten
      )

      Ok(view(list, operatorId))
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen checkSubmissionsAllowed andThen getData(operatorId) andThen requireData).async {
    implicit request =>
      getAnswerAsync(PlatformOperatorSummaryQuery) { platformOperator =>
        
        userAnswersService.toAssumedReportingSubmission(request.userAnswers)
          .map(Future.successful)
          .left.map(errors => Future.failed(BuildAssumedReportingSubmissionFailure(errors)))
          .merge
          .flatMap { submissionRequest =>
            for {
              submission   <- submit(submissionRequest, platformOperator)
              summary      <- AssumedReportSummary(request.userAnswers).map(Future.successful).getOrElse(Future.failed(Exception("unable to build an assumed report summary")))
              emptyAnswers = UserAnswers(request.userId, operatorId, Some(summary.reportingPeriod))
              answers      <- Future.fromTry(emptyAnswers.set(AssumedReportSummaryQuery, summary))
              _            <- sessionRepository.set(answers)
            } yield Redirect(routes.SubmissionConfirmationController.onPageLoad(operatorId, summary.reportingPeriod))
          }
      }
  }
  
  private def submit(submissionRequest: AssumedReportingSubmissionRequest,
                     platformOperator: PlatformOperatorSummary)
                    (using request: DataRequest[AnyContent]): Future[Submission] =
    connector.submit(submissionRequest)
      .map { submission =>
        audit(request.dprsId, platformOperator.operatorName, 200, submissionRequest, Some(submission._id))
        submission
      }
      .recoverWith {
        case ex: SubmitAssumedReportingFailure =>
          audit(request.dprsId, platformOperator.operatorName, ex.status, submissionRequest, None)
          Future.failed(ex)
      }
  
  private def audit(dprsId: String, operatorName: String, statusCode: Int, submission: AssumedReportingSubmissionRequest, conversationId: Option[String])
                   (using request: DataRequest[AnyContent]): Unit = {
    val auditEvent = AddAssumedReportEvent(
      dprsId         = dprsId,
      operatorName   = operatorName,
      submission     = submission,
      statusCode     = statusCode,
      processedAt    = Instant.now(clock),
      conversationId = conversationId,
      countriesList  = countriesList
    )
    
    auditService.audit(auditEvent)
  }
}