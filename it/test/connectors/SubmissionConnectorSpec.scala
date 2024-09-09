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
import connectors.SubmissionConnector.{GetFailure, StartUploadFailure, SubmitFailure, UploadFailedFailure, UploadSuccessFailure}
import models.submission.Submission.State.Ready
import models.submission.{StartSubmissionRequest, Submission, UploadFailedRequest}
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import uk.gov.hmrc.http.test.WireMockSupport

import java.time.Instant

class SubmissionConnectorSpec
  extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with GuiceOneAppPerSuite
    with ScalaFutures
    with IntegrationPatience
    with OptionValues {

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure(
      "appName"                                               -> "app",
      "microservice.services.digital-platform-reporting.port" -> wireMockPort
    )
    .build()

  private lazy val connector = app.injector.instanceOf[SubmissionConnector]

  private val now: Instant = Instant.now()
  private val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("auth")))

  "start" - {

    val platformOperatorId = "poid"
    val request = StartSubmissionRequest(platformOperatorId)

    val expectedSubmission = Submission(
      _id = "id",
      dprsId = "dprsId",
      state = Ready,
      created = now,
      updated = now
    )

    "must return a Submission when the service responds with CREATED" in {

      wireMockServer.stubFor(
        put(urlPathEqualTo("/digital-platform-reporting/submission/start"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .withRequestBody(equalToJson(Json.toJson(request).toString))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(Json.toJson(expectedSubmission).toString)
          )
      )

      val result = connector.start(platformOperatorId, None)(using hc).futureValue
      result mustEqual expectedSubmission
    }

    "must return a Submission when the service responds with OK" in {

      wireMockServer.stubFor(
        put(urlPathEqualTo("/digital-platform-reporting/submission/start"))
          .withQueryParam("id", equalTo("id"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .withRequestBody(equalToJson(Json.toJson(request).toString))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(Json.toJson(expectedSubmission).toString)
          )
      )

      val result = connector.start(platformOperatorId, Some("id"))(using hc).futureValue
      result mustEqual expectedSubmission
    }

    "must return an error when the service responds with another status" in {

      wireMockServer.stubFor(
        put(urlPathEqualTo("/digital-platform-reporting/submission/start"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .withRequestBody(equalToJson(Json.toJson(request).toString))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val result = connector.start(platformOperatorId, Some("id"))(using hc).failed.futureValue
      result mustBe a[SubmissionConnector.StartFailure]
    }
  }

  "get" - {

    val expectedSubmission = Submission(
      _id = "id",
      dprsId = "dprsId",
      state = Ready,
      created = now,
      updated = now
    )

    "must return a submission when the service responds with OK" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/submission/id"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(expectedSubmission).toString)
          )
      )

      val result = connector.get("id")(using hc).futureValue.value
      result mustEqual expectedSubmission
    }

    "must return None when the service responds wit NOT_FOUND" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/submission/id"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      val result = connector.get("id")(using hc).futureValue
      result mustBe None
    }

    "must return an error when the service responds with another status" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/submission/id"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val result = connector.get("id")(using hc).failed.futureValue
      result mustBe a[GetFailure]
    }
  }

  "startUpload" - {

    "must return successfully when the service returns OK" in {

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/id/start-upload"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      connector.startUpload("id")(using hc).futureValue
    }

    "must return a failure when the service returns another status" in {

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/id/start-upload"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val result = connector.startUpload("id")(using hc).failed.futureValue
      result mustBe a[StartUploadFailure]
    }
  }

  "uploadSuccess" - {

    "must return successfully when the service returns OK" in {

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/id/upload-success"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      connector.uploadSuccess("id")(using hc).futureValue
    }

    "must return a failure when the service returns another status" in {

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/id/upload-success"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val result = connector.uploadSuccess("id")(using hc).failed.futureValue
      result mustBe a[UploadSuccessFailure]
    }
  }

  "uploadFailed" - {

    "must return successfully when the service returns OK" in {

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/id/upload-failed"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .withRequestBody(equalToJson(Json.toJson(UploadFailedRequest("reason")).toString))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      connector.uploadFailed("id", "reason")(using hc).futureValue
    }

    "must return a failure when the service returns another status" in {

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/id/upload-failed"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .withRequestBody(equalToJson(Json.toJson(UploadFailedRequest("reason")).toString))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val result = connector.uploadFailed("id", "reason")(using hc).failed.futureValue
      result mustBe a[UploadFailedFailure]
    }
  }

  "submit" - {

    "must return successfully when the service returns OK" in {

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/id/submit"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      connector.submit("id")(using hc).futureValue
    }

    "must return a failure when the service returns another status" in {

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/id/submit"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val result = connector.submit("id")(using hc).failed.futureValue
      result mustBe a[SubmitFailure]
    }
  }
}
