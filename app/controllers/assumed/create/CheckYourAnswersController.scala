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
import models.audit.AddAssumedReportEvent
import models.requests.DataRequest
import models.submission.{AssumedReportSummary, AssumedReportingSubmissionRequest, CreateAssumedReportingSubmissionRequest, Submission}
import models.{CountriesList, UserAnswers}
import pages.assumed.AssumedSubmissionSentPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{AssumedReportSummaryQuery, PlatformOperatorSummaryQuery, SentAddAssumedReportingEmailsQuery}
import repositories.SessionRepository
import services.{AuditService, EmailService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.PlatformOperatorSummary
import viewmodels.checkAnswers.assumed.create.*
import viewmodels.govuk.summarylist.*
import views.html.assumed.create.CheckYourAnswersView

import java.time.{Clock, Instant}
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(override val messagesApi: MessagesApi,
                                           identify: IdentifierAction,
                                           getData: DataRetrievalActionProvider,
                                           requireData: DataRequiredAction,
                                           assumedSubmissionSentCheck: AssumedSubmissionSentCheckAction,
                                           checkAssumedReportingAllowed: CheckAssumedReportingAllowedAction,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: CheckYourAnswersView,
                                           assumedReportingConnector: AssumedReportingConnector,
                                           sessionRepository: SessionRepository,
                                           auditService: AuditService,
                                           emailService: EmailService,
                                           clock: Clock,
                                           countriesList: CountriesList)
                                          (using ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor with Logging {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen checkAssumedReportingAllowed andThen getData(operatorId) andThen requireData andThen assumedSubmissionSentCheck) {
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

  def onSubmit(operatorId: String): Action[AnyContent] =
    (identify andThen checkAssumedReportingAllowed andThen getData(operatorId) andThen requireData).async { implicit request =>
      getAnswerAsync(PlatformOperatorSummaryQuery) { platformOperator =>
        CreateAssumedReportingSubmissionRequest.build(request.userAnswers).fold(
          _ => Future.successful(Redirect(routes.MissingInformationController.onPageLoad(operatorId))),
          submissionRequest => (for {
            submission <- submit(submissionRequest.asAssumedReportingSubmissionRequest, platformOperator)
            summary <- AssumedReportSummary(request.userAnswers).map(Future.successful).getOrElse(Future.failed(Exception("unable to build an assumed report summary")))
            emptyAnswers = UserAnswers(request.userId, operatorId, Some(summary.reportingPeriod))
            emailsSentResult <- emailService.sendAddAssumedReportingEmails(platformOperator, submission.created, summary)
            answersWithSummary <- Future.fromTry(emptyAnswers.set(AssumedReportSummaryQuery, summary))
            answersWithEmailSentResult <- Future.fromTry(answersWithSummary.set(SentAddAssumedReportingEmailsQuery, emailsSentResult))
            _ <- sessionRepository.set(answersWithEmailSentResult)
            answersWithAssumedSubmissionSent <- Future.fromTry(request.userAnswers.set(AssumedSubmissionSentPage, true))
            _ <- sessionRepository.set(answersWithAssumedSubmissionSent)
          } yield Redirect(routes.SubmissionConfirmationController.onPageLoad(operatorId, summary.reportingPeriod))).recover {
            case error: SubmitAssumedReportingFailure => logger.warn("Failed to submit assumed reporting", error)
              throw error
          }
        )
      }
    }

  private def submit(submissionRequest: AssumedReportingSubmissionRequest,
                     platformOperator: PlatformOperatorSummary)
                    (using request: DataRequest[AnyContent]): Future[Submission] =
    assumedReportingConnector.submit(submissionRequest)
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
      dprsId = dprsId,
      operatorName = operatorName,
      submission = submission,
      statusCode = statusCode,
      processedAt = Instant.now(clock),
      conversationId = conversationId,
      countriesList = countriesList
    )

    auditService.audit(auditEvent)
  }
}