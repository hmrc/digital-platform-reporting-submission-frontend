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

import base.SpecBase
import connectors.SubmissionConnector
import controllers.routes as baseRoutes
import forms.ReportingPeriodFormProvider
import models.submission.SubmissionStatus.{Pending, Success}
import models.submission.*
import models.{NormalMode, UserAnswers, yearFormat}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.assumed.create.ReportingPeriodPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.SubmissionsExistQuery
import repositories.SessionRepository
import views.html.assumed.create.ReportingPeriodView

import java.time.{Clock, Instant, Year, ZoneId}
import scala.concurrent.Future

class ReportingPeriodControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val instant = Instant.now()
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val formProvider = new ReportingPeriodFormProvider(stubClock)
  private val form = formProvider()

  private val validAnswer = Year.of(2024)

  private lazy val reportingPeriodRoute = routes.ReportingPeriodController.onPageLoad(NormalMode, operatorId).url

  private val mockConnector = mock[SubmissionConnector]
  private val mockSessionRepository = mock[SessionRepository]

  override def beforeEach(): Unit = {
    reset(mockConnector, mockSessionRepository)
    super.beforeEach()
  }

  "ReportingPeriod Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, reportingPeriodRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ReportingPeriodView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, operatorId)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(ReportingPeriodPage, validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, reportingPeriodRoute)

        val view = application.injector.instanceOf[ReportingPeriodView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAnswer), NormalMode, operatorId)(request, messages(application)).toString
      }
    }

    "must redirect to SubmissionsDisabled for a GET when submissions are disabled" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.submissions-enabled" -> false)
          .build()

      running(application) {
        val request = FakeRequest(GET, reportingPeriodRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.SubmissionsDisabledController.onPageLoad().url
      }
    }

    "must record whether any XML submissions exist for this PO and year, then redirect to the next page when valid data is submitted" - {

      val expectedViewSubmissionsRequest = ViewSubmissionsRequest(
        assumedReporting = false,
        pageNumber = 1,
        sortBy = SortBy.SubmissionDate,
        sortOrder = SortOrder.Descending,
        reportingPeriod = Some(validAnswer.getValue),
        operatorId = Some(operatorId),
        statuses = SubmissionStatus.values
      )
      
      "when there are no XML submissions at all for this user" in {

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockConnector.listDeliveredSubmissions(any())(using any())).thenReturn(Future.successful(None))
        when(mockConnector.listUndeliveredSubmissions(using any())).thenReturn(Future.successful(Nil))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[SubmissionConnector].toInstance(mockConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, reportingPeriodRoute)
              .withFormUrlEncodedBody(("value", validAnswer.toString))
          
          val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
          val answers =
            emptyUserAnswers
              .set(ReportingPeriodPage, validAnswer).success.value
              .set(SubmissionsExistQuery, false).success.value

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual ReportingPeriodPage.nextPage(NormalMode, answers).url
          verify(mockConnector).listDeliveredSubmissions(eqTo(expectedViewSubmissionsRequest))(using(any()))
          verify(mockConnector).listUndeliveredSubmissions(using any())
          verify(mockSessionRepository).set(answersCaptor.capture())

          val savedAnswers = answersCaptor.getValue
          savedAnswers.get(SubmissionsExistQuery).value mustEqual false
          savedAnswers.get(ReportingPeriodPage).value mustEqual validAnswer
        }
      }

      "when there are delivered XML submissions for this operator and reportable period" in {

        val deliveredSubmission = SubmissionSummary(
          submissionId = "submissionId",
          fileName = "filename",
          operatorId = operatorId,
          operatorName = operatorName,
          reportingPeriod = validAnswer,
          submissionDateTime = instant,
          submissionStatus = Success,
          assumingReporterName = None,
          submissionCaseId = Some("caseId"),
          localDataExists = true
        )
        val submissionsSummary = SubmissionsSummary(Seq(deliveredSubmission), 1, true, 0)

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockConnector.listDeliveredSubmissions(any())(using any())).thenReturn(Future.successful(Some(submissionsSummary)))
        when(mockConnector.listUndeliveredSubmissions(using any())).thenReturn(Future.successful(Nil))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[SubmissionConnector].toInstance(mockConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, reportingPeriodRoute)
              .withFormUrlEncodedBody(("value", validAnswer.toString))

          val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
          val answers =
            emptyUserAnswers
              .set(ReportingPeriodPage, validAnswer).success.value
              .set(SubmissionsExistQuery, true).success.value

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual ReportingPeriodPage.nextPage(NormalMode, answers).url
          verify(mockConnector).listDeliveredSubmissions(eqTo(expectedViewSubmissionsRequest))(using (any()))
          verify(mockConnector).listUndeliveredSubmissions(using any())
          verify(mockSessionRepository).set(answersCaptor.capture())

          val savedAnswers = answersCaptor.getValue
          savedAnswers.get(SubmissionsExistQuery).value mustEqual true
          savedAnswers.get(ReportingPeriodPage).value mustEqual validAnswer
        }
      }

      "when there are undelivered XML submissions for this operator and reportable period" in {

        val undeliveredSubmission = SubmissionSummary(
          submissionId = "submissionId",
          fileName = "filename",
          operatorId = operatorId,
          operatorName = operatorName,
          reportingPeriod = validAnswer,
          submissionDateTime = instant,
          submissionStatus = Pending,
          assumingReporterName = None,
          submissionCaseId = Some("caseId"),
          localDataExists = true
        )

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockConnector.listDeliveredSubmissions(any())(using any())).thenReturn(Future.successful(None))
        when(mockConnector.listUndeliveredSubmissions(using any())).thenReturn(Future.successful(Seq(undeliveredSubmission)))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[SubmissionConnector].toInstance(mockConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, reportingPeriodRoute)
              .withFormUrlEncodedBody(("value", validAnswer.toString))

          val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
          val answers =
            emptyUserAnswers
              .set(ReportingPeriodPage, validAnswer).success.value
              .set(SubmissionsExistQuery, true).success.value

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual ReportingPeriodPage.nextPage(NormalMode, answers).url
          verify(mockConnector).listDeliveredSubmissions(eqTo(expectedViewSubmissionsRequest))(using (any()))
          verify(mockConnector).listUndeliveredSubmissions(using any())
          verify(mockSessionRepository).set(answersCaptor.capture())

          val savedAnswers = answersCaptor.getValue
          savedAnswers.get(SubmissionsExistQuery).value mustEqual true
          savedAnswers.get(ReportingPeriodPage).value mustEqual validAnswer
        }
      }

      "when there are delivered XML submissions, but they are for a different operator or reportable period" in {

        val deliveredSubmission1 = SubmissionSummary(
          submissionId = "submissionId",
          fileName = "filename",
          operatorId = "different operator id",
          operatorName = operatorName,
          reportingPeriod = validAnswer,
          submissionDateTime = instant,
          submissionStatus = Success,
          assumingReporterName = None,
          submissionCaseId = Some("caseId"),
          localDataExists = true
        )
        val deliveredSubmission2 = SubmissionSummary(
          submissionId = "submissionId",
          fileName = "filename",
          operatorId = operatorId,
          operatorName = operatorName,
          reportingPeriod = validAnswer.plusYears(1),
          submissionDateTime = instant,
          submissionStatus = Success,
          assumingReporterName = None,
          submissionCaseId = Some("caseId"),
          localDataExists = true
        )
        val submissionsSummary = SubmissionsSummary(Seq(deliveredSubmission1, deliveredSubmission2), 0, true, 0)

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockConnector.listDeliveredSubmissions(any())(using any())).thenReturn(Future.successful(Some(submissionsSummary)))
        when(mockConnector.listUndeliveredSubmissions(using any())).thenReturn(Future.successful(Nil))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[SubmissionConnector].toInstance(mockConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, reportingPeriodRoute)
              .withFormUrlEncodedBody(("value", validAnswer.toString))

          val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
          val answers =
            emptyUserAnswers
              .set(ReportingPeriodPage, validAnswer).success.value
              .set(SubmissionsExistQuery, false).success.value

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual ReportingPeriodPage.nextPage(NormalMode, answers).url
          verify(mockConnector).listDeliveredSubmissions(eqTo(expectedViewSubmissionsRequest))(using any())
          verify(mockConnector).listUndeliveredSubmissions(using any())
          verify(mockSessionRepository).set(answersCaptor.capture())

          val savedAnswers = answersCaptor.getValue
          savedAnswers.get(SubmissionsExistQuery).value mustEqual false
          savedAnswers.get(ReportingPeriodPage).value mustEqual validAnswer
        }
      }

      "when there are undelivered XML submissions, but they are for a different operator or reportable period" ignore {

        val undeliveredSubmission1 = SubmissionSummary(
          submissionId = "submissionId",
          fileName = "filename",
          operatorId = "different operator id",
          operatorName = operatorName,
          reportingPeriod = validAnswer,
          submissionDateTime = instant,
          submissionStatus = Pending,
          assumingReporterName = None,
          submissionCaseId = Some("caseId"),
          localDataExists = true
        )
        val undeliveredSubmission2 = SubmissionSummary(
          submissionId = "submissionId",
          fileName = "filename",
          operatorId = operatorId,
          operatorName = operatorName,
          reportingPeriod = validAnswer.plusYears(1),
          submissionDateTime = instant,
          submissionStatus = Pending,
          assumingReporterName = None,
          submissionCaseId = Some("caseId"),
          localDataExists = true
        )

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockConnector.listDeliveredSubmissions(any())(using any())).thenReturn(Future.successful(None))
        when(mockConnector.listUndeliveredSubmissions(using any())).thenReturn(Future.successful(Seq(undeliveredSubmission1, undeliveredSubmission2)))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[SubmissionConnector].toInstance(mockConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, reportingPeriodRoute)
              .withFormUrlEncodedBody(("value", validAnswer.toString))

          val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
          val answers =
            emptyUserAnswers
              .set(ReportingPeriodPage, validAnswer).success.value
              .set(SubmissionsExistQuery, false).success.value

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual ReportingPeriodPage.nextPage(NormalMode, answers).url
          verify(mockConnector).listDeliveredSubmissions(eqTo(expectedViewSubmissionsRequest))(using any())
          verify(mockConnector).listUndeliveredSubmissions(using any())
          verify(mockSessionRepository).set(answersCaptor.capture())

          val savedAnswers = answersCaptor.getValue
          savedAnswers.get(SubmissionsExistQuery).value mustEqual false
          savedAnswers.get(ReportingPeriodPage).value mustEqual validAnswer
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, reportingPeriodRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[ReportingPeriodView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, operatorId)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, reportingPeriodRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, reportingPeriodRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Submissions Disabled for a POST when submissions are disabled" in {

      val application =
        applicationBuilder(userAnswers = None)
          .configure("features.submissions-enabled" -> false)
          .build()

      running(application) {
        val request =
          FakeRequest(POST, reportingPeriodRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.SubmissionsDisabledController.onPageLoad().url
      }
    }
  }
}
