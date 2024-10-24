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

package controllers.assumed

import base.SpecBase
import connectors.SubmissionConnector
import models.UserAnswers
import models.submission.{SubmissionStatus, SubmissionSummary, SubmissionsSummary, ViewSubmissionsRequest}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.AssumedReportSummariesQuery
import repositories.SessionRepository
import views.html.assumed.ViewAssumedReportsView

import java.time.Instant
import scala.jdk.CollectionConverters.*
import scala.concurrent.Future

class ViewAssumedReportsControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[SubmissionConnector]
  private val mockRepository = mock[SessionRepository]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector, mockRepository)
    super.beforeEach()
  }
  
  private val now = Instant.now
  
  "ViewAssumedReports Controller" - {

    "must save submission summaries and return OK and the correct view for a GET when assumed reports exist" in {

      val submissionSummary1 = SubmissionSummary(
        submissionId = "submissionId1",
        fileName = "filename",
        operatorId = "operatorId1",
        operatorName = "operatorName",
        reportingPeriod = "2024",
        submissionDateTime = now,
        submissionStatus = SubmissionStatus.Success,
        assumingReporterName = Some("name"),
        submissionCaseId = Some("reportingPeriod1")
      )
      val submissionSummary2 = SubmissionSummary(
        submissionId = "submissionId2",
        fileName = "filename2",
        operatorId = "operatorId1",
        operatorName = "operatorName",
        reportingPeriod = "2024",
        submissionDateTime = now,
        submissionStatus = SubmissionStatus.Success,
        assumingReporterName = Some("name"),
        submissionCaseId = Some("reportingPeriod1")
      )
      val submissionSummary3 = SubmissionSummary(
        submissionId = "submissionId3",
        fileName = "filename3",
        operatorId = "operatorId2",
        operatorName = "operatorName",
        reportingPeriod = "2024",
        submissionDateTime = now,
        submissionStatus = SubmissionStatus.Success,
        assumingReporterName = Some("name"),
        submissionCaseId = None
      )
      val summary = SubmissionsSummary(Seq(submissionSummary1, submissionSummary2), Seq(submissionSummary3))

      when(mockConnector.list(any())(using any())).thenReturn(Future.successful(Some(summary)))
      when(mockRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[SubmissionConnector].toInstance(mockConnector),
          bind[SessionRepository].toInstance(mockRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(routes.ViewAssumedReportsController.onPageLoad())

        val result = route(application, request).value

        val view = application.injector.instanceOf[ViewAssumedReportsView]
        implicit val msgs: Messages = messages(application)
        val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        val expectedRequest = ViewSubmissionsRequest(assumedReporting = true)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(Some(summary))(request, implicitly).toString
        verify(mockConnector, times(1)).list(eqTo(expectedRequest))(using any())
        verify(mockRepository, times(2)).set(answersCaptor.capture())

        val savedAnswers = answersCaptor.getAllValues.asScala
        savedAnswers.size mustEqual 2
        val operator1Answers = savedAnswers.find(_.operatorId == "operatorId1").value
        val operator2Answers = savedAnswers.find(_.operatorId == "operatorId2").value
        operator1Answers.get(AssumedReportSummariesQuery).value mustEqual Seq(submissionSummary1, submissionSummary2)
        operator2Answers.get(AssumedReportSummariesQuery).value mustEqual Seq(submissionSummary3)
      }
    }

    "must return OK and the correct view for a GET when no assumed reports exist" in {

      when(mockConnector.list(any())(using any())).thenReturn(Future.successful(None))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[SubmissionConnector].toInstance(mockConnector),
          bind[SessionRepository].toInstance(mockRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(routes.ViewAssumedReportsController.onPageLoad())

        val result = route(application, request).value

        val view = application.injector.instanceOf[ViewAssumedReportsView]
        implicit val msgs: Messages = messages(application)
        val expectedRequest = ViewSubmissionsRequest(assumedReporting = true)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(None)(request, implicitly).toString
        verify(mockConnector, times(1)).list(eqTo(expectedRequest))(using any())
        verify(mockRepository, never()).set(any())
      }
    }
  }
}
