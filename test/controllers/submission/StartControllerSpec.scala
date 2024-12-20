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

package controllers.submission

import base.SpecBase
import connectors.PlatformOperatorConnector.PlatformOperatorNotFoundFailure
import connectors.{PlatformOperatorConnector, SubmissionConnector}
import controllers.routes as baseRoutes
import models.UserAnswers
import models.operator.responses.PlatformOperator
import models.operator.{AddressDetails, ContactDetails}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PlatformOperatorSummaryQuery
import repositories.SessionRepository
import services.ConfirmedDetailsService
import support.builders.ConfirmedDetailsBuilder.aConfirmedDetails
import support.builders.SubmissionBuilder.aSubmission
import support.builders.UserAnswersBuilder.aUserAnswers
import viewmodels.PlatformOperatorSummary
import views.html.submission.StartPageView

import scala.concurrent.Future

class StartControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSubmissionConnector = mock[SubmissionConnector]
  private val mockPlatformOperatorConnector = mock[PlatformOperatorConnector]
  private val mockSessionRepository = mock[SessionRepository]
  private val mockConfirmedDetailsService = mock[ConfirmedDetailsService]
  private val platformOperatorSummary = PlatformOperatorSummary("operatorId", "operatorName", "primaryContactName", "test@test.com", hasReportingNotifications = true)
  private val baseAnswers = aUserAnswers.set(PlatformOperatorSummaryQuery, platformOperatorSummary).success.value

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(
      mockSubmissionConnector,
      mockPlatformOperatorConnector,
      mockSessionRepository,
      mockConfirmedDetailsService
    )
  }

  "StartPage Controller" - {
    "onPageLoad" - {
      "must return OK and the correct view for a GET when user answers exist" in {
        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[SessionRepository].toInstance(mockSessionRepository)
        ).build()

        running(application) {
          val request = FakeRequest(routes.StartController.onPageLoad(operatorId))
          val result = route(application, request).value
          val view = application.injector.instanceOf[StartPageView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(operatorId)(request, messages(application)).toString
        }

        verify(mockPlatformOperatorConnector, never()).viewPlatformOperator(any())(any())
        verify(mockSessionRepository, never()).set(any())
      }

      "must redirect to SubmissionsDisabled for a GET when submissions are disabled" in {
        val application = applicationBuilder(userAnswers = Some(baseAnswers))
          .configure("features.submissions-enabled" -> false)
          .build()

        running(application) {
          val request = FakeRequest(routes.StartController.onPageLoad(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual baseRoutes.SubmissionsDisabledController.onPageLoad().url
        }
      }

      "must save a platform operator summary and return OK and the correct view for a GET when user answers do not exist" in {
        val operator = PlatformOperator(
          operatorId = operatorId,
          operatorName = "operatorName",
          tinDetails = Nil,
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "name", "email"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", None, None, None, None, Some("GB")),
          notifications = Nil
        )

        when(mockPlatformOperatorConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = None).overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[SessionRepository].toInstance(mockSessionRepository)
        ).build()

        running(application) {
          val request = FakeRequest(routes.StartController.onPageLoad(operatorId))
          val result = route(application, request).value
          val view = application.injector.instanceOf[StartPageView]
          val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
          val expectedSummary = PlatformOperatorSummary(operator.operatorId, operator.operatorName, "name", "email", hasReportingNotifications = false)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(operatorId)(request, messages(application)).toString

          verify(mockPlatformOperatorConnector, times(1)).viewPlatformOperator(eqTo(operator.operatorId))(any())
          verify(mockSessionRepository, times(1)).set(answersCaptor.capture())

          val answers = answersCaptor.getValue
          answers.get(PlatformOperatorSummaryQuery).value mustEqual expectedSummary
        }
      }

      "must redirect to Select Platform Operator when user answers do not exist and the platform operator cannot be found" in {
        when(mockPlatformOperatorConnector.viewPlatformOperator(any())(any())).thenReturn(Future.failed(PlatformOperatorNotFoundFailure))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = None).overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[SessionRepository].toInstance(mockSessionRepository)
        ).build()

        running(application) {
          val request = FakeRequest(routes.StartController.onPageLoad(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url // TODO change this when the select PO pages exist

          verify(mockSessionRepository, never()).set(any())
        }
      }
    }

    "onSubmit" - {
      "must redirect to Check Platform Operator when business details have not been confirmed" in {
        val application = applicationBuilder(userAnswers = Some(aUserAnswers)).overrides(
          bind[SubmissionConnector].toInstance(mockSubmissionConnector),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()

        when(mockConfirmedDetailsService.confirmedDetailsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(businessDetails = false)))

        running(application) {
          val request = FakeRequest(routes.StartController.onSubmit(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckPlatformOperatorController.onPageLoad(operatorId).url
        }

        verify(mockSubmissionConnector, never()).start(any(), any(), any())(using any())
      }

      "must redirect to Check Reporting Notifications when reporting notifications not confirmed" in {
        val application = applicationBuilder(userAnswers = Some(aUserAnswers)).overrides(
          bind[SubmissionConnector].toInstance(mockSubmissionConnector),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()

        when(mockConfirmedDetailsService.confirmedDetailsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(reportingNotifications = false)))

        running(application) {
          val request = FakeRequest(routes.StartController.onSubmit(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckReportingNotificationsController.onPageLoad(operatorId).url
        }

        verify(mockSubmissionConnector, never()).start(any(), any(), any())(using any())
      }

      "must redirect to Check Contact details when contact details not confirmed" in {
        val application = applicationBuilder(userAnswers = Some(aUserAnswers)).overrides(
          bind[SubmissionConnector].toInstance(mockSubmissionConnector),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()

        when(mockConfirmedDetailsService.confirmedDetailsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(yourContactDetails = false)))

        running(application) {
          val request = FakeRequest(routes.StartController.onSubmit(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckContactDetailsController.onPageLoad(operatorId).url
        }

        verify(mockSubmissionConnector, never()).start(any(), any(), any())(using any())
      }

      "must redirect to Upload page when all details confirmed" in {
        val userAnswers = aUserAnswers.set(PlatformOperatorSummaryQuery, platformOperatorSummary).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).overrides(
          bind[SubmissionConnector].toInstance(mockSubmissionConnector),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()

        when(mockConfirmedDetailsService.confirmedDetailsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(true, true, true)))
        when(mockSubmissionConnector.start(any(), any(), any())(using any())).thenReturn(Future.successful(aSubmission))

        running(application) {
          val request = FakeRequest(routes.StartController.onSubmit(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UploadController.onPageLoad(operatorId, aSubmission._id).url
        }

        verify(mockSubmissionConnector, times(1)).start(eqTo(operatorId), eqTo(platformOperatorSummary.operatorName), eqTo(None))(using any())
        verify(mockConfirmedDetailsService, times(1)).confirmedDetailsFor(eqTo(operatorId))(using any())
      }

      "must redirect to SubmissionsDisabled when submissions are disabled" in {
        
        val application = applicationBuilder(userAnswers = None)
          .configure("features.submissions-enabled" -> false)
          .build()

        running(application) {
          val request = FakeRequest(routes.StartController.onSubmit(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual baseRoutes.SubmissionsDisabledController.onPageLoad().url
        }
      }
    }
  }
}
