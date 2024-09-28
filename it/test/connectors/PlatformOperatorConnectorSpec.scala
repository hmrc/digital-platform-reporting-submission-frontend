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

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.PlatformOperatorConnector._
import models.operator.{AddressDetails, ContactDetails}
import models.operator.responses._
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import uk.gov.hmrc.http.test.WireMockSupport

class PlatformOperatorConnectorSpec extends AnyFreeSpec
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

  private lazy val connector = app.injector.instanceOf[PlatformOperatorConnector]

  private implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("authToken")))

  ".viewPlatformOperators" - {

    "must return platform operator details when the server returns OK" in {

      val serverResponse = ViewPlatformOperatorsResponse(platformOperators = Seq(
        PlatformOperator(
          operatorId = "operatorId",
          operatorName = "operatorName",
          tinDetails = Seq.empty,
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "primaryContactName", "primaryEmail"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line1", None, None, None, Some("postCode"), None),
          notifications = Seq.empty
        )
      ))

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/platform-operator"))
          .withHeader("Authorization", equalTo("authToken"))
          .willReturn(
            ok(Json.toJson(serverResponse).toString)
          )
      )

      val result = connector.viewPlatformOperators.futureValue
      result mustEqual serverResponse
    }

    "must return empty platform operator details when the server returns NOT_FOUND" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/platform-operator"))
          .withHeader("Authorization", equalTo("authToken"))
          .willReturn(notFound())
      )

      val result = connector.viewPlatformOperators.futureValue
      result mustEqual ViewPlatformOperatorsResponse(Seq.empty)
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/platform-operator"))
          .withHeader("Authorization", equalTo("authToken"))
          .willReturn(serverError())
      )

      val result = connector.viewPlatformOperators.failed.futureValue
      result mustBe a[ViewPlatformOperatorFailure]

      val failure = result.asInstanceOf[ViewPlatformOperatorFailure]
      failure.status mustEqual INTERNAL_SERVER_ERROR
    }
  }
}
