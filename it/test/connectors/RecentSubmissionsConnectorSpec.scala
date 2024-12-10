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
import connectors.PendingEnrolmentConnector.{GetRecentSubmissionFailure, SaveRecentSubmissionFailure}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import support.builders.RecentSubmissionDetailsBuilder.aRecentSubmissionDetails
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

class RecentSubmissionsConnectorSpec extends AnyFreeSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with WireMockSupport
  with GuiceOneAppPerSuite {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure("microservice.services.digital-platform-reporting.port" -> wireMockPort)
    .build()

  private lazy val underTest = app.injector.instanceOf[RecentSubmissionsConnector]

  ".getRecentUserSubmissions" - {
    "must return recent submission details when the server returns OK" in {
      val responsePayload = Json.toJsObject(aRecentSubmissionDetails)

      wireMockServer.stubFor(get(urlMatching("/digital-platform-reporting/recent-submissions"))
        .willReturn(ok(responsePayload.toString())))

      underTest.getRecentUserSubmissions().futureValue mustBe Some(aRecentSubmissionDetails)
    }

    "must return None when the server returns NOT_FOUND" in {
      wireMockServer.stubFor(get(urlMatching("/digital-platform-reporting/recent-submissions"))
        .willReturn(notFound))

      underTest.getRecentUserSubmissions().futureValue mustBe None
    }

    "must return GetRecentSubmissionFailure when the server returns error" in {
      wireMockServer.stubFor(get(urlMatching("/digital-platform-reporting/recent-submissions"))
        .willReturn(serverError()))

      val result = underTest.getRecentUserSubmissions().failed.futureValue
      result.asInstanceOf[GetRecentSubmissionFailure].status mustBe 500
    }
  }

  ".save" - {
    "must save recent submission details when the server returns OK" in {
      val responsePayload = Json.toJsObject(aRecentSubmissionDetails)

      wireMockServer.stubFor(post(urlMatching("/digital-platform-reporting/recent-submissions"))
        .withRequestBody(equalToJson(responsePayload.toString))
        .willReturn(ok()))

      underTest.save(aRecentSubmissionDetails).futureValue
    }

    "must return SaveRecentSubmissionFailure when the server returns error" in {
      val responsePayload = Json.toJsObject(aRecentSubmissionDetails)

      wireMockServer.stubFor(post(urlMatching("/digital-platform-reporting/recent-submissions"))
        .withRequestBody(equalToJson(responsePayload.toString))
        .willReturn(serverError()))

      val result = underTest.save(aRecentSubmissionDetails).failed.futureValue
      result.asInstanceOf[SaveRecentSubmissionFailure].status mustBe 500
    }
  }
}