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

package controllers.assumed.remove

import connectors.AssumedReportingConnector
import connectors.AssumedReportingConnector.DeleteAssumedReportFailure
import controllers.actions.*
import controllers.assumed.remove.routes.AssumedReportRemovedController
import controllers.assumed.routes.ViewAssumedReportsController
import controllers.routes.JourneyRecoveryController
import controllers.{AnswerExtractor, routes as baseRoutes}
import forms.RemoveAssumedReportFormProvider
import models.audit.DeleteAssumedReportEvent
import models.submission.{AssumedReportingSubmission, AssumedReportingSubmissionSummary}
import pages.assumed.AssumedSubmissionDeletedPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{AssumedReportSummariesQuery, SentDeleteAssumedReportingEmailsQuery}
import repositories.SessionRepository
import services.{AuditService, EmailService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.assumed.remove.AssumedReportSummaryList
import views.html.assumed.remove.RemoveAssumedReportView

import java.time.{Clock, Instant, Year}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveAssumedReportController @Inject()(override val messagesApi: MessagesApi,
                                              val controllerComponents: MessagesControllerComponents,
                                              identify: IdentifierAction,
                                              getData: DataRetrievalActionProvider,
                                              requireData: DataRequiredAction,
                                              assumedSubmissionDeletedCheck: AssumedSubmissionDeletedCheckAction,
                                              checkAssumedReportingAllowed: CheckAssumedReportingAllowedAction,
                                              formProvider: RemoveAssumedReportFormProvider,
                                              view: RemoveAssumedReportView,
                                              assumedReportingConnector: AssumedReportingConnector,
                                              auditService: AuditService,
                                              emailService: EmailService,
                                              sessionRepository: SessionRepository,
                                              clock: Clock)
                                             (using ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor with Logging {

  def onPageLoad(operatorId: String, reportingPeriod: Year): Action[AnyContent] = (identify andThen checkAssumedReportingAllowed andThen getData(operatorId) andThen requireData andThen assumedSubmissionDeletedCheck) { implicit request =>
    getAnswer(AssumedReportSummariesQuery) { summaries =>
      summaries.find(_.reportingPeriod == reportingPeriod).map { summary =>
        val summaryList = AssumedReportSummaryList.list(summary)
        Ok(view(formProvider(), summaryList, operatorId, summary.operatorName, reportingPeriod))
      }.getOrElse(NotFound)
    }
  }

  def onSubmit(operatorId: String, reportingPeriod: Year): Action[AnyContent] = (identify andThen checkAssumedReportingAllowed andThen getData(operatorId) andThen requireData).async { implicit request =>
    getAnswerAsync(AssumedReportSummariesQuery) { summaries =>
      summaries.find(_.reportingPeriod == reportingPeriod).map { summary =>
        formProvider().bindFromRequest().fold(
          formWithErrors => {
            val summaryList = AssumedReportSummaryList.list(summary)
            Future.successful(BadRequest(view(formWithErrors, summaryList, operatorId, summary.operatorName, reportingPeriod)))
          },
          answer =>
            if (answer) {
              assumedReportingConnector.get(operatorId, reportingPeriod).flatMap {
                case Some(assumedReportingSubmission) => assumedReportingConnector.delete(operatorId, reportingPeriod).flatMap { _ =>
                  val deletionInstant = Instant.now(clock)
                  val auditEvent = DeleteAssumedReportEvent(
                    dprsId = request.dprsId,
                    operatorId = operatorId,
                    operatorName = summary.operatorName,
                    conversationId = summary.submissionId,
                    statusCode = 200,
                    processedAt = deletionInstant
                  )

                  auditService.audit(auditEvent)

                  for {
                    emailsSentResult <- emailService.sendDeleteAssumedReportingEmails(operatorId, assumedReportingSubmission, deletionInstant)
                    answersWithSentEmails <- Future.fromTry(request.userAnswers.set(SentDeleteAssumedReportingEmailsQuery, emailsSentResult))
                    answersWithAssumedSubmissionDeleted <- Future.fromTry(answersWithSentEmails.set(AssumedSubmissionDeletedPage, true))
                    _ <- sessionRepository.set(answersWithAssumedSubmissionDeleted)
                  } yield {
                    Redirect(AssumedReportRemovedController.onPageLoad(operatorId, reportingPeriod))
                  }
                }.recoverWith {
                  case ex: DeleteAssumedReportFailure =>
                    auditService.audit(DeleteAssumedReportEvent(
                      dprsId = request.dprsId,
                      operatorId = operatorId,
                      operatorName = summary.operatorName,
                      conversationId = summary.submissionId,
                      statusCode = ex.status,
                      processedAt = Instant.now(clock)
                    ))

                    Future.failed(ex)
                }

                case None => Future.successful(Redirect(JourneyRecoveryController.onPageLoad()))
              }
            } else {
              Future.successful(Redirect(ViewAssumedReportsController.onPageLoad()))
            }
        )
      }.getOrElse(Future.successful(NotFound))
    }
  }
}
