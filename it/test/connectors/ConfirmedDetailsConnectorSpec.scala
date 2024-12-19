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
import connectors.ConfirmedDetailsConnector.*
import models.confirmed.ConfirmedContactDetails
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import support.builders.ConfirmedBusinessDetailsBuilder.aConfirmedBusinessDetails
import support.builders.ConfirmedReportingNotificationsBuilder.aConfirmedReportingNotifications
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

class ConfirmedDetailsConnectorSpec extends AnyFreeSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with WireMockSupport
  with GuiceOneAppPerSuite {

  implicit private val hc: HeaderCarrier = HeaderCarrier()


  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure("microservice.services.digital-platform-reporting.port" -> wireMockPort)
    .build()

  private lazy val underTest = app.injector.instanceOf[ConfirmedDetailsConnector]

  "ConfirmedBusinessDetails" - {
    ".businessDetails(...)" - {
      val operatorId = aConfirmedBusinessDetails.operatorId

      "must return confirmed business details when the server returns OK" in {
        val responsePayload = Json.toJsObject(aConfirmedBusinessDetails)

        wireMockServer.stubFor(get(urlMatching(s"/digital-platform-reporting/confirmed/business-details/$operatorId"))
          .willReturn(ok(responsePayload.toString())))

        underTest.businessDetails(operatorId).futureValue mustBe Some(aConfirmedBusinessDetails)
      }

      "must return None when the server returns NOT_FOUND" in {
        wireMockServer.stubFor(get(urlMatching(s"/digital-platform-reporting/confirmed/business-details/$operatorId"))
          .willReturn(notFound))

        underTest.businessDetails(operatorId).futureValue mustBe None
      }

      "must return GetConfirmedBusinessDetailsFailure when the server returns error" in {
        wireMockServer.stubFor(get(urlMatching(s"/digital-platform-reporting/confirmed/business-details/$operatorId"))
          .willReturn(serverError()))

        val result = underTest.businessDetails(operatorId).failed.futureValue
        result.asInstanceOf[GetConfirmedBusinessDetailsFailure].status mustBe 500
      }
    }

    ".save(confirmedBusinessDetails)" - {
      "must call BE to save confirmed business details and return success when OK" in {
        val responsePayload = Json.toJsObject(aConfirmedBusinessDetails)

        wireMockServer.stubFor(post(urlMatching("/digital-platform-reporting/confirmed/business-details"))
          .withRequestBody(equalToJson(responsePayload.toString))
          .willReturn(ok()))

        underTest.save(aConfirmedBusinessDetails).futureValue
      }

      "must return SaveConfirmedBusinessDetailsFailure whe the server returns error" in {
        val responsePayload = Json.toJsObject(aConfirmedBusinessDetails)

        wireMockServer.stubFor(post(urlMatching("/digital-platform-reporting/confirmed/business-details"))
          .withRequestBody(equalToJson(responsePayload.toString))
          .willReturn(serverError()))

        val result = underTest.save(aConfirmedBusinessDetails).failed.futureValue
        result.asInstanceOf[SaveConfirmedBusinessDetailsFailure].status mustBe 500
      }
    }
  }

  "ConfirmedReportingNotifications" - {
    ".reportingNotifications(...)" - {
      val operatorId = aConfirmedReportingNotifications.operatorId

      "must return confirmed reporting notifications when the server returns OK" in {
        val responsePayload = Json.toJsObject(aConfirmedReportingNotifications)

        wireMockServer.stubFor(get(urlMatching(s"/digital-platform-reporting/confirmed/reporting-notifications/$operatorId"))
          .willReturn(ok(responsePayload.toString())))

        underTest.reportingNotifications(operatorId).futureValue mustBe Some(aConfirmedReportingNotifications)
      }

      "must return None when the server returns NOT_FOUND" in {
        wireMockServer.stubFor(get(urlMatching(s"/digital-platform-reporting/confirmed/reporting-notifications/$operatorId"))
          .willReturn(notFound))

        underTest.reportingNotifications(operatorId).futureValue mustBe None
      }

      "must return GetConfirmedReportingNotificationsFailure when the server returns error" in {
        wireMockServer.stubFor(get(urlMatching(s"/digital-platform-reporting/confirmed/reporting-notifications/$operatorId"))
          .willReturn(serverError()))

        val result = underTest.reportingNotifications(operatorId).failed.futureValue
        result.asInstanceOf[GetConfirmedReportingNotificationsFailure].status mustBe 500
      }
    }

    ".save(confirmedReportingNotifications)" - {
      "must call BE to save confirmed reporting notifications and return success when OK" in {
        val responsePayload = Json.toJsObject(aConfirmedReportingNotifications)

        wireMockServer.stubFor(post(urlMatching("/digital-platform-reporting/confirmed/reporting-notifications"))
          .withRequestBody(equalToJson(responsePayload.toString))
          .willReturn(ok()))

        underTest.save(aConfirmedReportingNotifications).futureValue
      }

      "must return SaveConfirmedReportingNotificationsFailure whe the server returns error" in {
        val responsePayload = Json.toJsObject(aConfirmedReportingNotifications)

        wireMockServer.stubFor(post(urlMatching("/digital-platform-reporting/confirmed/reporting-notifications"))
          .withRequestBody(equalToJson(responsePayload.toString))
          .willReturn(serverError()))

        val result = underTest.save(aConfirmedReportingNotifications).failed.futureValue
        result.asInstanceOf[SaveConfirmedReportingNotificationsFailure].status mustBe 500
      }
    }
  }

  "ConfirmedContactDetails" - {
    ".contactDetails(...)" - {
      "must return confirmed contact details when the server returns OK" in {
        val responsePayload = Json.toJsObject(ConfirmedContactDetails())

        wireMockServer.stubFor(get(urlMatching(s"/digital-platform-reporting/confirmed/contact-details"))
          .willReturn(ok(responsePayload.toString())))

        underTest.contactDetails().futureValue mustBe Some(ConfirmedContactDetails())
      }

      "must return None when the server returns NOT_FOUND" in {
        wireMockServer.stubFor(get(urlMatching(s"/digital-platform-reporting/confirmed/contact-details"))
          .willReturn(notFound))

        underTest.contactDetails().futureValue mustBe None
      }

      "must return GetConfirmedContactDetailsFailure when the server returns error" in {
        wireMockServer.stubFor(get(urlMatching(s"/digital-platform-reporting/confirmed/contact-details"))
          .willReturn(serverError()))

        val result = underTest.contactDetails().failed.futureValue
        result.asInstanceOf[GetConfirmedContactDetailsFailure].status mustBe 500
      }
    }

    ".saveContactDetails()" - {
      "must call BE to save confirmed contact details and return success when OK" in {
        wireMockServer.stubFor(post(urlMatching("/digital-platform-reporting/confirmed/contact-details"))
          .willReturn(ok()))

        underTest.saveContactDetails().futureValue
      }

      "must return SaveConfirmedContactDetailsFailure whe the server returns error" in {
        wireMockServer.stubFor(post(urlMatching("/digital-platform-reporting/confirmed/contact-details"))
          .willReturn(serverError()))

        val result = underTest.saveContactDetails().failed.futureValue
        result.asInstanceOf[SaveConfirmedContactDetailsFailure].status mustBe 500
      }
    }
  }
}