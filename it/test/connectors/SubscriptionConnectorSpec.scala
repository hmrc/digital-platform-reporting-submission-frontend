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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import connectors.SubscriptionConnector.GetSubscriptionFailure
import models.subscription.*
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import uk.gov.hmrc.http.test.WireMockSupport

class SubscriptionConnectorSpec extends AnyFreeSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with WireMockSupport
  with MockitoSugar
  with BeforeAndAfterEach
  with EitherValues {

  private lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.digital-platform-reporting.port" -> wireMockPort)
    .build()

  private lazy val connector = app.injector.instanceOf[SubscriptionConnector]

  ".getSubscription" - {
    
    "must return subscription info when the server returns OK" in {

      val responsePayload = Json.obj(
        "id" -> "DPRS123",
        "gbUser" -> true,
        "primaryContact" -> Json.obj(
          "individual" -> Json.obj(
            "firstName" -> "first",
            "lastName" -> "last"
          ),
          "email" -> "email"
        )
      )
      val expectedIndividual = IndividualContact(Individual("first", "last"), "email", None)
      val expectedResponse = SubscriptionInfo("DPRS123", true, None, expectedIndividual, None)
      val hc = HeaderCarrier(authorization = Some(Authorization("someAuthToken")))

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/subscribe"))
          .withHeader("Authorization", equalTo("someAuthToken"))
          .willReturn(ok(responsePayload.toString))
      )

      val result = connector.getSubscription(hc).futureValue

      result mustEqual expectedResponse
    }

    "must fail when the server returns an error" in {

      val hc = HeaderCarrier(authorization = Some(Authorization("someAuthToken")))

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/subscribe"))
          .withHeader("Authorization", equalTo("someAuthToken"))
          .willReturn(serverError())
      )

      val result = connector.getSubscription(hc).failed.futureValue
      result mustBe a[GetSubscriptionFailure]

      val getSubscriptionFailure = result.asInstanceOf[GetSubscriptionFailure]
      getSubscriptionFailure.status mustBe 500
    }
  }
}
