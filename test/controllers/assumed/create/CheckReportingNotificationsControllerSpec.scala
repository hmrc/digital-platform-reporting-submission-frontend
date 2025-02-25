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
import config.FrontendAppConfig
import connectors.PlatformOperatorConnector
import controllers.routes as baseRoutes
import forms.CheckReportingNotificationsFormProvider
import models.NormalMode
import models.operator.responses.{NotificationDetails, PlatformOperator}
import models.operator.{AddressDetails, ContactDetails, NotificationType}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.assumed.AssumedSubmissionSentPage
import pages.assumed.create.CheckReportingNotificationsPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PlatformOperatorSummaryQuery
import repositories.SessionRepository
import services.ConfirmedDetailsService
import support.builders.ConfirmedDetailsBuilder.aConfirmedDetails
import viewmodels.PlatformOperatorSummary
import viewmodels.govuk.SummaryListFluency
import views.html.assumed.create.CheckReportingNotificationsView

import java.time.Instant
import scala.concurrent.Future

class CheckReportingNotificationsControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach {

  private val form = CheckReportingNotificationsFormProvider()()
  private val mockPlatformOperatorConnector = mock[PlatformOperatorConnector]
  private val mockSessionRepository = mock[SessionRepository]
  private val mockConfirmedDetailsService = mock[ConfirmedDetailsService]
  private val operatorSummary = PlatformOperatorSummary("operatorId", "operatorName", "primaryContactName", "test@test.com", hasReportingNotifications = true)
  private val baseAnswers = emptyUserAnswers.set(PlatformOperatorSummaryQuery, operatorSummary).success.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockPlatformOperatorConnector, mockSessionRepository, mockConfirmedDetailsService)
    super.beforeEach()
  }

  "Check Reporting Notifications Controller" - {

    val notification = NotificationDetails(NotificationType.Epo, None, None, 2024, Instant.now)
    val operator = PlatformOperator(
      operatorId = "operatorId",
      operatorName = "operatorName",
      tinDetails = Nil,
      businessName = None,
      tradingName = None,
      primaryContactDetails = ContactDetails(None, "name", "email"),
      secondaryContactDetails = None,
      addressDetails = AddressDetails("line 1", None, None, None, None, Some("GB")),
      notifications = Seq(notification)
    )

    "must return OK and the correct view for a GET when there is at least one notification for this PO" in {

      when(mockPlatformOperatorConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckReportingNotificationsController.onPageLoad(operatorId).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckReportingNotificationsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, Seq(notification), "operatorId", "operatorName")(request, messages(application)).toString
      }
    }

    "must redirect to Reporting Notification Required for a GET when there are no notifications for this PO" in {

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

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckReportingNotificationsController.onPageLoad(operatorId).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ReportingNotificationRequiredController.onPageLoad(operatorId).url
      }
    }

    "must redirect to AssumedReportingDisabled for a GET when submissions are disabled" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .configure("features.submissions-enabled" -> false)
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckReportingNotificationsController.onPageLoad(operatorId).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
      }
    }

    "must redirect to AssumedSubmissionAlreadySent for a GET when AssumedSubmissionSentPage is true" in {

      val userAnswers = baseAnswers.set(AssumedSubmissionSentPage, true).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckReportingNotificationsController.onPageLoad(operatorId).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.assumed.routes.AssumedSubmissionAlreadySentController.onPageLoad().url
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val mockAppConfig = mock[FrontendAppConfig]
      val baseAnswers = emptyUserAnswers.set(CheckReportingNotificationsPage(mockAppConfig), true).success.value

      when(mockPlatformOperatorConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckReportingNotificationsController.onPageLoad(operatorId).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckReportingNotificationsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), Seq(notification), "operatorId", "operatorName")(request, messages(application)).toString
      }
    }

      "onSubmit(...)" - {
      "must return BadRequest and errors when an invalid answer is submitted" in {
        val notification = NotificationDetails(NotificationType.Epo, None, None, 2024, Instant.now)
        val operator = PlatformOperator(
          operatorId = "operatorId",
          operatorName = "operatorName",
          tinDetails = Nil,
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "name", "email"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", None, None, None, None, Some("GB")),
          notifications = Seq(notification)
        )

        when(mockPlatformOperatorConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.CheckReportingNotificationsController.onPageLoad(operatorId).url)
              .withFormUrlEncodedBody("value" -> "invalid value")

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckReportingNotificationsView]

          val formWithErrors = form.bind(Map("value" -> "invalid value"))

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(formWithErrors, Seq(notification), "operatorId", "operatorName")(request, messages(application)).toString
        }
      }

      "must redirect to add a reporting notification when `false` is submitted" in {
        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()
        val page = application.injector.instanceOf[CheckReportingNotificationsPage]
        val expectedAnswers = baseAnswers.set(page, false).success.value

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        running(application) {
          val request = FakeRequest(POST, routes.CheckReportingNotificationsController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "false")
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual appConfig.viewNotificationsUrl(operatorId)
        }

        verify(mockSessionRepository, times(1)).set(expectedAnswers)
        verify(mockConfirmedDetailsService, never()).confirmBusinessDetailsFor(any())(using any())
      }

      "must redirect to Check Platform Operator when 'true' is selected and business details have not been confirmed" in {
        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()
        val page = application.injector.instanceOf[CheckReportingNotificationsPage]
        val expectedAnswers = baseAnswers.set(page, true).success.value

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockConfirmedDetailsService.confirmReportingNotificationsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(businessDetails = false)))

        running(application) {
          val request = FakeRequest(POST, routes.CheckReportingNotificationsController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "true")
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckPlatformOperatorController.onPageLoad(operatorId).url
        }

        verify(mockSessionRepository, times(1)).set(expectedAnswers)
        verify(mockConfirmedDetailsService, times(1)).confirmReportingNotificationsFor(eqTo(operatorId))(using any())
      }

      "must redirect to Check Reporting Notifications when 'true' is selected and notifications have not been confirmed" in {
        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()
        val page = application.injector.instanceOf[CheckReportingNotificationsPage]
        val expectedAnswers = baseAnswers.set(page, true).success.value

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockConfirmedDetailsService.confirmReportingNotificationsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(reportingNotifications = false)))

        running(application) {
          val request = FakeRequest(POST, routes.CheckReportingNotificationsController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "true")
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckReportingNotificationsController.onPageLoad(operatorId).url
        }

        verify(mockSessionRepository, times(1)).set(expectedAnswers)
        verify(mockConfirmedDetailsService, times(1)).confirmReportingNotificationsFor(eqTo(operatorId))(using any())
      }

      "must redirect to Check Contact details when 'true' is selected and contact details have not been confirmed" in {
        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()
        val page = application.injector.instanceOf[CheckReportingNotificationsPage]
        val expectedAnswers = baseAnswers.set(page, true).success.value

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockConfirmedDetailsService.confirmReportingNotificationsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(yourContactDetails = false)))

        running(application) {
          val request = FakeRequest(POST, routes.CheckReportingNotificationsController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "true")
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckContactDetailsController.onPageLoad(operatorId).url
        }

        verify(mockSessionRepository, times(1)).set(expectedAnswers)
        verify(mockConfirmedDetailsService, times(1)).confirmReportingNotificationsFor(eqTo(operatorId))(using any())
      }

      "must redirect to Reporting Period when all details are confirmed" in {
        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()
        val page = application.injector.instanceOf[CheckReportingNotificationsPage]
        val expectedAnswers = baseAnswers.set(page, true).success.value

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockConfirmedDetailsService.confirmReportingNotificationsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(true, true, true)))

        running(application) {
          val request = FakeRequest(POST, routes.CheckReportingNotificationsController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "true")
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.ReportingPeriodController.onPageLoad(NormalMode, operatorId).url
        }

        verify(mockSessionRepository, times(1)).set(expectedAnswers)
        verify(mockConfirmedDetailsService, times(1)).confirmReportingNotificationsFor(eqTo(operatorId))(using any())
      }

      "must redirect to AssumedReportingDisabled when submissions are disabled" in {

        val application = applicationBuilder(userAnswers = Some(baseAnswers))
          .configure("features.submissions-enabled" -> false)
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckReportingNotificationsController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "true")
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
        }
      }
    }
  }
}
