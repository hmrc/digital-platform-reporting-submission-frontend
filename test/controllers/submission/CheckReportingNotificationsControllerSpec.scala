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
import config.FrontendAppConfig
import connectors.PlatformOperatorConnector
import forms.CheckReportingNotificationsFormProvider
import models.operator.responses.{NotificationDetails, PlatformOperator}
import models.operator.{AddressDetails, ContactDetails, NotificationType}
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PlatformOperatorSummaryQuery
import repositories.SessionRepository
import viewmodels.PlatformOperatorSummary
import viewmodels.checkAnswers.operator.*
import viewmodels.govuk.SummaryListFluency
import views.html.submission.CheckReportingNotificationsView

import java.time.Instant
import scala.concurrent.Future

class CheckReportingNotificationsControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach {

  private val form = CheckReportingNotificationsFormProvider()()
  private val mockConnector = mock[PlatformOperatorConnector]
  private val operatorSummary = PlatformOperatorSummary("operatorId", "operatorName", true)
  private val baseAnswers = emptyUserAnswers.set(PlatformOperatorSummaryQuery, operatorSummary).success.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector)
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

      when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[PlatformOperatorConnector].toInstance(mockConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckReportingNotificationsController.onPageLoad(operatorId).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ReportingNotificationRequiredController.onPageLoad(operatorId).url
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
          FakeRequest(POST, routes.CheckReportingNotificationsController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "invalid value")

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckReportingNotificationsView]

        val formWithErrors = form.bind(Map("value" -> "invalid value"))

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(formWithErrors, Seq(notification), "operatorId", "operatorName")(request, messages(application)).toString
      }
    }

    "must redirect to check contact details when `true` is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, routes.CheckReportingNotificationsController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckContactDetailsController.onPageLoad(operatorId).url
      }
    }

    "must redirect to add a reporting notification when `false` is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, routes.CheckReportingNotificationsController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "false")

        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual appConfig.manageHomepageUrl
      }
    }
  }
}
