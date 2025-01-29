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
import builders.AssumedReportingSubmissionSummaryBuilder.anAssumedReportingSubmissionSummary
import builders.ViewAssumedReportsFormDataBuilder.aViewAssumedReportsFormData
import builders.ViewPlatformOperatorsResponseBuilder.aViewPlatformOperatorsResponse
import connectors.{AssumedReportingConnector, PlatformOperatorConnector}
import controllers.routes as baseRoutes
import forms.assumed.ViewAssumedReportsFormProvider
import models.UserAnswers
import models.pageviews.assumed.ViewAssumedReportsViewModel
import models.submission.AssumedReportingSubmissionSummary
import org.mockito.ArgumentMatchers.any
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

import java.time.*
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

class ViewAssumedReportsControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockAssumedReportingConnector = mock[AssumedReportingConnector]
  private val mockPlatformOperatorConnector = mock[PlatformOperatorConnector]
  private val mockRepository = mock[SessionRepository]

  override def beforeEach(): Unit = {
    Mockito.reset(mockAssumedReportingConnector, mockPlatformOperatorConnector, mockRepository)
    super.beforeEach()
  }

  private val now = Instant.now
  private val clock: Clock = Clock.fixed(now, ZoneId.systemDefault)
  private val form = ViewAssumedReportsFormProvider()()

  "ViewAssumedReports Controller" - {
    "must save submission summaries and return OK and the correct view for a GET when assumed reports exist" in {
      val submissionSummary1 = anAssumedReportingSubmissionSummary.copy(
        submissionId = "submissionId1",
        operatorId = "operatorId1",
      )
      val submissionSummary2 = anAssumedReportingSubmissionSummary.copy(
        submissionId = "submissionId2",
        operatorId = "operatorId1",
      )
      val submissionSummary3 = anAssumedReportingSubmissionSummary.copy(
        submissionId = "submissionId3",
        operatorId = "operatorId2",
      )
      val summaries = Seq(submissionSummary1, submissionSummary2, submissionSummary3)
      val operatorsResponse = aViewPlatformOperatorsResponse
      val formData = aViewAssumedReportsFormData

      when(mockAssumedReportingConnector.list(using any())).thenReturn(Future.successful(summaries))
      when(mockPlatformOperatorConnector.viewPlatformOperators(using any())).thenReturn(Future.successful(operatorsResponse))
      when(mockRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = None).overrides(
        bind[AssumedReportingConnector].toInstance(mockAssumedReportingConnector),
        bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
        bind[SessionRepository].toInstance(mockRepository)
      ).build()

      running(application) {
        val request = FakeRequest(routes.ViewAssumedReportsController.onPageLoad())
        val result = route(application, request).value
        val view = application.injector.instanceOf[ViewAssumedReportsView]
        implicit val msgs: Messages = messages(application)
        val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        val viewModel = ViewAssumedReportsViewModel(
          platformOperators = operatorsResponse.platformOperators,
          assumedReportingSubmissionSummaries = summaries,
          currentYear = Year.now(clock),
          form = form.fill(formData)
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(viewModel)(request, implicitly).toString

        verify(mockAssumedReportingConnector, times(1)).list(using any())
        verify(mockPlatformOperatorConnector, times(1)).viewPlatformOperators(using any())
        verify(mockRepository, times(2)).set(answersCaptor.capture())

        val savedAnswers = answersCaptor.getAllValues.asScala
        savedAnswers.size mustEqual 2
        val operator1Answers = savedAnswers.find(_.operatorId == "operatorId1").value
        val operator2Answers = savedAnswers.find(_.operatorId == "operatorId2").value
        operator1Answers.get(AssumedReportSummariesQuery).value mustEqual Seq(submissionSummary1, submissionSummary2)
        operator2Answers.get(AssumedReportSummariesQuery).value mustEqual Seq(submissionSummary3)
      }
    }

    "must save submission summaries, return OK and the correct view when assumed reports exist and filtering" in {
      val submissionSummary1 = anAssumedReportingSubmissionSummary.copy(
        submissionId = "submissionId1",
        operatorId = "operatorId1",
      )
      val submissionSummary2 = anAssumedReportingSubmissionSummary.copy(
        submissionId = "submissionId2",
        operatorId = "operatorId1",
        reportingPeriod = Year.parse("2024")
      )
      val submissionSummary3 = anAssumedReportingSubmissionSummary.copy(
        submissionId = "submissionId3",
        operatorId = "operatorId2",
      )
      val summaries = Seq(submissionSummary1, submissionSummary2, submissionSummary3)
      val operatorsResponse = aViewPlatformOperatorsResponse
      val formData = aViewAssumedReportsFormData.copy(operatorId = Some("operatorId1"), reportingPeriod = Some(Year.now(clock)))

      when(mockAssumedReportingConnector.list(using any())).thenReturn(Future.successful(summaries))
      when(mockPlatformOperatorConnector.viewPlatformOperators(using any())).thenReturn(Future.successful(operatorsResponse))
      when(mockRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = None).overrides(
        bind[AssumedReportingConnector].toInstance(mockAssumedReportingConnector),
        bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
        bind[SessionRepository].toInstance(mockRepository)
      ).build()

      running(application) {
        val request =
          FakeRequest(GET, routes.ViewAssumedReportsController.onPageLoad().url + s"?operatorId=operatorId1&reportingPeriod=${Year.now(clock)}")
        val result = route(application, request).value
        val view = application.injector.instanceOf[ViewAssumedReportsView]
        implicit val msgs: Messages = messages(application)
        val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        val viewModel = ViewAssumedReportsViewModel(
          platformOperators = operatorsResponse.platformOperators,
          assumedReportingSubmissionSummaries = Seq(submissionSummary1),
          currentYear = Year.now(clock),
          form = form.fill(formData)
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(viewModel)(request, implicitly).toString

        verify(mockAssumedReportingConnector, times(1)).list(using any())
        verify(mockPlatformOperatorConnector, times(1)).viewPlatformOperators(using any())
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
      val operatorsResponse = aViewPlatformOperatorsResponse
      val formData = aViewAssumedReportsFormData

      when(mockAssumedReportingConnector.list(using any())).thenReturn(Future.successful(Nil))
      when(mockPlatformOperatorConnector.viewPlatformOperators(using any())).thenReturn(Future.successful(operatorsResponse))

      val application = applicationBuilder(userAnswers = None).overrides(
        bind[AssumedReportingConnector].toInstance(mockAssumedReportingConnector),
        bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
        bind[SessionRepository].toInstance(mockRepository)
      ).build()

      running(application) {
        val request = FakeRequest(routes.ViewAssumedReportsController.onPageLoad())
        val result = route(application, request).value
        val view = application.injector.instanceOf[ViewAssumedReportsView]
        implicit val msgs: Messages = messages(application)
        val viewModel = ViewAssumedReportsViewModel(
          platformOperators = operatorsResponse.platformOperators,
          assumedReportingSubmissionSummaries = Nil,
          currentYear = Year.now(clock),
          form = form.fill(formData)
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(viewModel)(request, implicitly).toString
        verify(mockAssumedReportingConnector).list(using any())
        verify(mockPlatformOperatorConnector, times(1)).viewPlatformOperators(using any())
        verify(mockRepository, never()).set(any())
      }
    }

    "must redirect to Journey Recovery page when there is error in the form" in {
      val application = applicationBuilder(userAnswers = None).overrides(
        bind[AssumedReportingConnector].toInstance(mockAssumedReportingConnector),
        bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
        bind[SessionRepository].toInstance(mockRepository)
      ).build()

      running(application) {
        val request = FakeRequest(GET, routes.ViewAssumedReportsController.onPageLoad().url + "?reportingPeriod=wrong-value")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }

      verify(mockAssumedReportingConnector, never).list(using any)
      verify(mockPlatformOperatorConnector, never).viewPlatformOperators(any)
      verify(mockRepository, never).set(any)
    }
  }
}
