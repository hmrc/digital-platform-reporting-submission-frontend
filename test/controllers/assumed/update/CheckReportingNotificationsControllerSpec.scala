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

package controllers.assumed.update

import base.SpecBase
import connectors.PlatformOperatorConnector
import controllers.routes as baseRoutes
import forms.CheckReportingNotificationsFormProvider
import models.UserAnswers
import models.operator.responses.{NotificationDetails, PlatformOperator}
import models.operator.{AddressDetails, ContactDetails, NotificationType}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.assumed.AssumedSubmissionSentPage
import pages.assumed.update.CheckReportingNotificationsPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PlatformOperatorSummaryQuery
import repositories.SessionRepository
import viewmodels.PlatformOperatorSummary
import viewmodels.govuk.SummaryListFluency
import views.html.assumed.update.CheckReportingNotificationsView

import java.time.{Instant, Year}
import scala.concurrent.Future

class CheckReportingNotificationsControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach {

  private val reportingPeriod = Year.of(2024)
  private val form = CheckReportingNotificationsFormProvider()()
  private val mockConnector = mock[PlatformOperatorConnector]
  private val mockRepository = mock[SessionRepository]
  private val operatorSummary = PlatformOperatorSummary("operatorId", "operatorName", "primaryContactName", "test@test.com", hasReportingNotifications = true)
  private val baseAnswers = emptyUserAnswers.copy(reportingPeriod = Some(reportingPeriod)).set(PlatformOperatorSummaryQuery, operatorSummary).success.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector, mockRepository)
    super.beforeEach()
  }

  "Check Reporting Notifications Controller" - {

    "must return OK and the correct view for a GET when there is at least one notification for this PO" in {

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

      when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[PlatformOperatorConnector].toInstance(mockConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckReportingNotificationsController.onPageLoad(operatorId, reportingPeriod).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckReportingNotificationsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, Seq(notification), "operatorId", reportingPeriod, "operatorName")(request, messages(application)).toString
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

      when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[PlatformOperatorConnector].toInstance(mockConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckReportingNotificationsController.onPageLoad(operatorId, reportingPeriod).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ReportingNotificationRequiredController.onPageLoad(operatorId, reportingPeriod).url
      }
    }

    "must redirect to AssumedReportingDisabled for a GET when submissions are disabled" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .configure("features.submissions-enabled" -> false)
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckReportingNotificationsController.onPageLoad(operatorId, reportingPeriod).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
      }
    }

    "must redirect to AssumedSubmissionAlreadySent for a GET when AssumedSubmissionSentPage is true" in {

      val userAnswers = baseAnswers.set(AssumedSubmissionSentPage, true).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckReportingNotificationsController.onPageLoad(operatorId, reportingPeriod).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.assumed.routes.AssumedSubmissionAlreadySentController.onPageLoad().url
      }
    }

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

      when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[PlatformOperatorConnector].toInstance(mockConnector))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.CheckReportingNotificationsController.onPageLoad(operatorId, reportingPeriod).url)
            .withFormUrlEncodedBody("value" -> "invalid value")

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckReportingNotificationsView]

        val formWithErrors = form.bind(Map("value" -> "invalid value"))

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(formWithErrors, Seq(notification), "operatorId", reportingPeriod, "operatorName")(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      when(mockRepository.set(any())).thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[SessionRepository].toInstance(mockRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.CheckReportingNotificationsController.onPageLoad(operatorId, reportingPeriod).url)
            .withFormUrlEncodedBody("value" -> "true")

        val page = application.injector.instanceOf[CheckReportingNotificationsPage]
        val expectedAnswers = baseAnswers.set(page, true).success.value
        val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual page.nextPage(reportingPeriod, expectedAnswers).url
        verify(mockRepository, times(1)).set(answersCaptor.capture())

        val answers = answersCaptor.getValue
        answers.get(page).value mustEqual true
      }
    }

    "must redirect to AssumedReportingDisabled when submissions are disabled" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .configure("features.submissions-enabled" -> false)
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.CheckReportingNotificationsController.onPageLoad(operatorId, reportingPeriod).url)
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
      }
    }
  }
}
