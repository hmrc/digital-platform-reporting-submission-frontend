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
import models.NormalMode
import models.operator.responses.PlatformOperator
import models.operator.{AddressDetails, ContactDetails}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.assumed.update.ReportingNotificationRequiredView

import java.time.Year
import scala.concurrent.Future

class ReportingNotificationRequiredControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val reportingPeriod = Year.of(2024)
  private val mockConnector = mock[PlatformOperatorConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector)
    super.beforeEach()
  }

  "Reporting Notification Required Controller" - {

    "must return OK and the correct view for a GET" in {

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
        applicationBuilder(userAnswers = None)
          .overrides(bind[PlatformOperatorConnector].toInstance(mockConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.ReportingNotificationRequiredController.onPageLoad(operatorId, reportingPeriod).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ReportingNotificationRequiredView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(operatorId, operator.operatorName)(request, messages(application)).toString
      }
    }
  }
}
