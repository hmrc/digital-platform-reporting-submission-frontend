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

import base.SpecBase
import builders.AssumedReportingSubmissionBuilder.anAssumedReportingSubmission
import connectors.AssumedReportingConnector
import connectors.AssumedReportingConnector.DeleteAssumedReportFailure
import controllers.assumed.routes as assumedRoutes
import controllers.routes as baseRoutes
import forms.RemoveAssumedReportFormProvider
import models.audit.DeleteAssumedReportEvent
import models.email.EmailsSentResult
import models.submission.{AssumedReportingSubmissionSummary, SubmissionStatus}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.assumed.AssumedSubmissionDeletedPage
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AssumedReportSummariesQuery, SentDeleteAssumedReportingEmailsQuery}
import repositories.SessionRepository
import services.{AuditService, EmailService}
import viewmodels.checkAnswers.assumed.remove.AssumedReportSummaryList
import views.html.assumed.remove.RemoveAssumedReportView

import java.time.{Clock, Instant, Year, ZoneId}
import scala.concurrent.Future

class RemoveAssumedReportControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val now = Instant.now()
  private val stubClock: Clock = Clock.fixed(now, ZoneId.systemDefault)
  private val currentYear: Year = Year.of(2024)
  private val submission1 = AssumedReportingSubmissionSummary("submissionId1", "file1", "operatorId", "operatorName", currentYear, now, SubmissionStatus.Success, Some("assuming"), Some("caseId1"), isDeleted = false)
  private val submission2 = AssumedReportingSubmissionSummary("submissionId2", "file2", "operatorId", "operatorName", currentYear.plusYears(1), now, SubmissionStatus.Success, Some("assuming"), Some("caseId2"), isDeleted = false)

  private val baseAnswers = emptyUserAnswers.set(AssumedReportSummariesQuery, Seq(submission1, submission2)).success.value

  private val form = RemoveAssumedReportFormProvider()()

  private val mockAssumedReportingConnector = mock[AssumedReportingConnector]
  private val mockAuditService = mock[AuditService]
  private val mockEmailService = mock[EmailService]
  private val mockSessionRepository = mock[SessionRepository]

  override def beforeEach(): Unit = {
    Mockito.reset(mockAssumedReportingConnector, mockAuditService, mockEmailService, mockSessionRepository)
    super.beforeEach()
  }

  "Remove Assumed Report Controller" - {

    "for a GET" - {

      "must return OK and the correct view for a known reportable period" in {

        val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

        running(application) {
          given Messages = messages(application)

          val request = FakeRequest(routes.RemoveAssumedReportController.onPageLoad("operatorId", currentYear))

          val view = application.injector.instanceOf[RemoveAssumedReportView]
          val summaryList = AssumedReportSummaryList.list(submission1)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, summaryList, "operatorId", "operatorName", currentYear)(request, implicitly).toString
        }
      }

      "must return 404 for an unknown reportable period" in {

        val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

        running(application) {
          val request = FakeRequest(routes.RemoveAssumedReportController.onPageLoad("operatorId", Year.of(2000)))

          val result = route(application, request).value

          status(result) mustEqual NOT_FOUND
        }
      }

      "must redirect to AssumedReportingDisabled for a GET when submissions are disabled" in {

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .configure("features.submissions-enabled" -> false)
            .build()

        running(application) {

          val request = FakeRequest(routes.RemoveAssumedReportController.onPageLoad("operatorId", Year.of(2024)))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
        }
      }

      "must redirect to AssumedSubmissionAlreadyDeleted for a GET when AssumedSubmissionDeletedPage is true" in {

        val userAnswers = baseAnswers.set(AssumedSubmissionDeletedPage, true).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(routes.RemoveAssumedReportController.onPageLoad("operatorId", Year.of(2024)))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.assumed.routes.AssumedSubmissionAlreadyDeletedController.onPageLoad().url
        }
      }

    }

    "for a POST" - {
      "must delete the submission, send an audit event, send emails, and redirect to Assumed Report Removed when the answer is yes" in {
        when(mockAssumedReportingConnector.get(any(), any())(using any())).thenReturn(Future.successful(Some(anAssumedReportingSubmission)))
        when(mockAssumedReportingConnector.delete(any(), any())(using any())).thenReturn(Future.successful(Done))
        when(mockEmailService.sendDeleteAssumedReportingEmails(any(), any(), any())(using any())).thenReturn(Future.successful(EmailsSentResult(true, None)))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val expectedAuditEvent = DeleteAssumedReportEvent("dprsId", operatorId, operatorName, "submissionId1", OK, now)
        val expectedUserAnswer = baseAnswers.set(SentDeleteAssumedReportingEmailsQuery, EmailsSentResult(true, None)).success.value
          .set(AssumedSubmissionDeletedPage, true).success.value

        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[AssumedReportingConnector].toInstance(mockAssumedReportingConnector),
          bind[AuditService].toInstance(mockAuditService),
          bind[EmailService].toInstance(mockEmailService),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[Clock].toInstance(stubClock)
        ).build()

        running(application) {
          val request = FakeRequest(routes.RemoveAssumedReportController.onSubmit(operatorId, currentYear))
            .withFormUrlEncodedBody("value" -> "true")
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.AssumedReportRemovedController.onPageLoad(operatorId, currentYear).url
        }

        verify(mockAssumedReportingConnector).get(eqTo(operatorId), eqTo(currentYear))(using any())
        verify(mockAssumedReportingConnector).delete(eqTo(operatorId), eqTo(currentYear))(using any())
        verify(mockAuditService).audit(eqTo(expectedAuditEvent))(using any(), any())
        verify(mockEmailService).sendDeleteAssumedReportingEmails(eqTo(operatorId), eqTo(anAssumedReportingSubmission), eqTo(now))(using any())
        verify(mockSessionRepository).set(expectedUserAnswer)
      }

      "must not delete the submission, or audit the event, and redirect to View Submissions when the answer is no" in {
        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[AssumedReportingConnector].toInstance(mockAssumedReportingConnector),
          bind[AuditService].toInstance(mockAuditService),
          bind[Clock].toInstance(stubClock)
        ).build()

        running(application) {
          val request = FakeRequest(routes.RemoveAssumedReportController.onSubmit("operatorId", currentYear))
            .withFormUrlEncodedBody("value" -> "false")
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual assumedRoutes.ViewAssumedReportsController.onPageLoad().url
        }

        verify(mockAssumedReportingConnector, never()).delete(any(), any())(using any())
        verify(mockAuditService, never()).audit(any())(using any(), any())
      }

      "must return Bad Request and display the view when invalid data is submitted" in {
        val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

        running(application) {
          given Messages = messages(application)

          val request = FakeRequest(routes.RemoveAssumedReportController.onSubmit("operatorId", currentYear))
            .withFormUrlEncodedBody("value" -> "")

          val view = application.injector.instanceOf[RemoveAssumedReportView]
          val summaryList = AssumedReportSummaryList.list(submission1)
          val boundForm = form.bind(Map("value" -> ""))
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, summaryList, "operatorId", "operatorName", currentYear)(request, implicitly).toString
        }
      }

      "must audit the event with an appropriate error status when submitting the deletion fails" in {
        when(mockAssumedReportingConnector.get(any(), any())(using any())).thenReturn(Future.successful(Some(anAssumedReportingSubmission)))
        when(mockAssumedReportingConnector.delete(any(), any())(using any())).thenReturn(Future.failed(DeleteAssumedReportFailure(INTERNAL_SERVER_ERROR)))

        val expectedAuditEvent = DeleteAssumedReportEvent("dprsId", operatorId, operatorName, "submissionId1", INTERNAL_SERVER_ERROR, now)

        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[AssumedReportingConnector].toInstance(mockAssumedReportingConnector),
          bind[AuditService].toInstance(mockAuditService),
          bind[EmailService].toInstance(mockEmailService),
          bind[Clock].toInstance(stubClock)
        ).build()

        running(application) {
          val request = FakeRequest(routes.RemoveAssumedReportController.onSubmit(operatorId, currentYear))
            .withFormUrlEncodedBody("value" -> "true")

          route(application, request).value.failed.futureValue
        }

        verify(mockAssumedReportingConnector).get(eqTo(operatorId), eqTo(currentYear))(using any())
        verify(mockAssumedReportingConnector).delete(eqTo(operatorId), eqTo(currentYear))(using any())
        verify(mockAuditService).audit(eqTo(expectedAuditEvent))(using any(), any())
        verify(mockEmailService, never()).sendDeleteAssumedReportingEmails(any(), any(), any())(using any())
      }

      "must redirect to AssumedReportingDisabled when submissions are disabled" in {

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .configure("features.submissions-enabled" -> false)
            .build()

        running(application) {
          val request =
            FakeRequest(routes.RemoveAssumedReportController.onSubmit("operatorId", Year.of(2024)))
              .withFormUrlEncodedBody("value" -> "")

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
        }
      }
    }
  }
}
