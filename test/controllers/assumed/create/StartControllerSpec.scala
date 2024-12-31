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
import connectors.PlatformOperatorConnector
import connectors.PlatformOperatorConnector.PlatformOperatorNotFoundFailure
import controllers.routes as baseRoutes
import models.operator.responses.PlatformOperator
import models.operator.{AddressDetails, ContactDetails}
import models.{NormalMode, UserAnswers}
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
import viewmodels.PlatformOperatorSummary
import views.html.assumed.create.StartView

import scala.concurrent.Future

class StartControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockPlatformOperatorConnector = mock[PlatformOperatorConnector]
  private val mockSessionRepository = mock[SessionRepository]
  private val mockConfirmedDetailsService = mock[ConfirmedDetailsService]
  private val platformOperatorSummary = PlatformOperatorSummary("operatorId", "operatorName", true)
  private val baseAnswers = emptyUserAnswers.set(PlatformOperatorSummaryQuery, platformOperatorSummary).success.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockPlatformOperatorConnector, mockSessionRepository, mockConfirmedDetailsService)
    super.beforeEach()
  }

  "Start Controller" - {

    "must return OK and the correct view for a GET when user answers exist" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartController.onPageLoad(operatorId).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[StartView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(operatorId)(request, messages(application)).toString
        verify(mockPlatformOperatorConnector, never()).viewPlatformOperator(any())(any())
        verify(mockSessionRepository, never()).set(any())
      }
    }

    "must redirect to AssumedReportingDisabled for a GET when submissions are disabled" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .configure("features.submissions-enabled" -> false)
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartController.onPageLoad(operatorId).url)

        val result = route(application, request).value
        
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
      }
    }

    "must save a platform operator summary and return OK and the correct view for a GET when user answers do not exist" in {

      val operator = PlatformOperator(
        operatorId = "operatorId",
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

      val application =
        applicationBuilder(userAnswers = None)
          .overrides(
            bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartController.onPageLoad(operatorId).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[StartView]
        val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        val expectedSummary = PlatformOperatorSummary(operator.operatorId, operator.operatorName, false)

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

      val application =
        applicationBuilder(userAnswers = None)
          .overrides(bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartController.onPageLoad(operatorId).url)

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SelectPlatformOperatorController.onPageLoad.url
      }
    }

    "onSubmit(...)" - {
      "must redirect to Check Platform Operator when business details have not been confirmed" in {
        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()

        when(mockConfirmedDetailsService.confirmedDetailsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(businessDetails = false)))

        running(application) {
          val request = FakeRequest(POST, routes.StartController.onSubmit(operatorId).url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckPlatformOperatorController.onPageLoad(operatorId).url
        }
      }

      "must redirect to Check Reporting Notifications when reporting notifications not confirmed" in {
        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()

        when(mockConfirmedDetailsService.confirmedDetailsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(reportingNotifications = false)))

        running(application) {
          val request = FakeRequest(routes.StartController.onSubmit(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckReportingNotificationsController.onSubmit(operatorId).url
        }
      }

      "must redirect to Check Contact details when contact details not confirmed" in {
        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()

        when(mockConfirmedDetailsService.confirmedDetailsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(yourContactDetails = false)))

        running(application) {
          val request = FakeRequest(routes.StartController.onSubmit(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckContactDetailsController.onSubmit(operatorId).url
        }
      }

      "must redirect to Reporting Period when all details are confirmed" in {
        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()

        when(mockConfirmedDetailsService.confirmedDetailsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(true, true, true)))

        running(application) {
          val request = FakeRequest(routes.StartController.onSubmit(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.ReportingPeriodController.onSubmit(NormalMode, operatorId).url
        }
      }

      "must redirect to AssumedReportingDisabled when submissions are disabled" in {
        
        val application = applicationBuilder(userAnswers = Some(baseAnswers))
          .configure("features.submissions-enabled" -> false)
          .build()

        running(application) {
          val request = FakeRequest(routes.StartController.onSubmit(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
        }
      }
    }
  }
}
