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
import connectors.SubmissionConnector.*
import models.operator.TinDetails
import models.operator.TinType.{Utr, Vrn}
import models.submission.*
import models.submission.Submission.State.{Ready, Submitted}
import models.submission.Submission.SubmissionType
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{CONFLICT, CREATED, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, StringContextOps}

import java.time.{Instant, Year}

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

    val expectedSubmission = Submission(
      _id = "id",
      submissionType = SubmissionType.Xml,
      dprsId = "dprsId",
      operatorId = "operatorId",
      operatorName = "operatorName",
      assumingOperatorName = None,
      state = Ready,
      created = now,
      updated = now
    )

    "must return a Submission when the service responds with CREATED" in {

      wireMockServer.stubFor(
        put(urlPathEqualTo("/digital-platform-reporting/submission/start"))
          .withRequestBody(equalToJson(Json.toJson(StartSubmissionRequest("operatorId", "operatorName")).toString))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(Json.toJson(expectedSubmission).toString)
          )
      )

      val result = connector.start("operatorId", "operatorName", None)(using hc).futureValue
      result mustEqual expectedSubmission
    }

    "must return a Submission when the service responds with OK" in {

      wireMockServer.stubFor(
        put(urlPathEqualTo("/digital-platform-reporting/submission/start"))
          .withRequestBody(equalToJson(Json.toJson(StartSubmissionRequest("operatorId", "operatorName")).toString))
          .withQueryParam("id", equalTo("id"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withBody(Json.toJson(expectedSubmission).toString)
          )
      )

      val result = connector.start("operatorId", "operatorName", Some("id"))(using hc).futureValue
      result mustEqual expectedSubmission
    }

    "must return an error when the service responds with another status" in {

      wireMockServer.stubFor(
        put(urlPathEqualTo("/digital-platform-reporting/submission/start"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val result = connector.start("operatorId", "operatorName", Some("id"))(using hc).failed.futureValue
      result mustBe a[SubmissionConnector.StartFailure]
    }
  }

  "get" - {

    val expectedSubmission = Submission(
      _id = "id",
      submissionType = SubmissionType.Xml,
      operatorId = "operatorId",
      operatorName = "operatorName",
      assumingOperatorName = None,
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

    "must return successfully when the service returns CONFLICT (occurs in some cases if the file has already returned a success/failure from upload)" in {

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/id/start-upload"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .willReturn(
            aResponse()
              .withStatus(CONFLICT)
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
    
    val request = UploadSuccessRequest(
      dprsId = "dprsId",
      downloadUrl = url"http://example.com/test.xml",
      fileName = "test.xml",
      checksum = "checksum",
      size = 1337L
    )

    "must return successfully when the service returns OK" in {

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/id/upload-success"))
          .withRequestBody(equalToJson(Json.toJson(request).toString))
          .withHeader("User-Agent", equalTo("app"))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      connector.uploadSuccess("id", request).futureValue
    }

    "must return a failure when the service returns another status" in {

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/id/upload-success"))
          .withRequestBody(equalToJson(Json.toJson(request).toString))
          .withHeader("User-Agent", equalTo("app"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val result = connector.uploadSuccess("id", request).failed.futureValue
      result mustBe a[UploadSuccessFailure]
    }
  }

  "uploadFailed" - {

    "must return successfully when the service returns OK" in {

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/id/upload-failed"))
          .withHeader("User-Agent", equalTo("app"))
          .withRequestBody(equalToJson(Json.toJson(UploadFailedRequest("dprsId", "reason")).toString))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      connector.uploadFailed("dprsId", "id", "reason").futureValue
    }

    "must return a failure when the service returns another status" in {

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/id/upload-failed"))
          .withHeader("User-Agent", equalTo("app"))
          .withRequestBody(equalToJson(Json.toJson(UploadFailedRequest("dprsId", "reason")).toString))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val result = connector.uploadFailed("dprsId", "id", "reason").failed.futureValue
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

  "getErrors" - {

    given Materializer = app.materializer

    val dprsId = "dprsId"
    val submissionId = "submissionId"
    val now = Instant.now()

    "must return CADX errors when the server response with OK" in {

      val error1 = CadxValidationError.FileError(submissionId = submissionId, dprsId = dprsId, code = "001", detail = Some("some detail\n"), created = now)
      val error2 = CadxValidationError.RowError(submissionId = submissionId, dprsId = dprsId, code = "001", docRef = "docRef", detail = Some("some detail"), created = now)

      val body = Seq(error1, error2).map(Json.toJson).mkString("\n")

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/submission/submissionId/errors"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .willReturn(ok(body))
      )

      val source = connector.getErrors(submissionId)(using hc).futureValue
      val result = source.runWith(Sink.fold(Seq.empty[CadxValidationError])(_ :+ _)).futureValue

      result must contain only(error1, error2)
    }

    "must fail when the server responds with NOT_FOUND" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/submission/submissionId/errors"))
          .withHeader("User-Agent", equalTo("app"))
          .withHeader("Authorization", equalTo("auth"))
          .willReturn(notFound())
      )

      connector.getErrors(submissionId)(using hc).failed.futureValue
    }
  }
  
  "list" - {
    
    val request = ViewSubmissionsRequest(assumedReporting = true)

    "must return a submission summary when the server returns OK" in {

      val submissionsSummary = SubmissionsSummary(Nil, Nil)
      val responsePayload = Json.obj(
        "deliveredSubmissions" -> Json.arr(),
        "localSubmissions" -> Json.arr()
      )

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/list"))
          .withRequestBody(equalToJson(Json.toJson(request).toString))
          .withHeader("User-Agent", equalTo("app"))
          .willReturn(ok(responsePayload.toString))
      )

      val result = connector.list(request)(using hc).futureValue
      result.value mustEqual submissionsSummary
    }

    "must return None when the server returns NOT_FOUND" in {

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/list"))
          .withRequestBody(equalToJson(Json.toJson(request).toString))
          .withHeader("User-Agent", equalTo("app"))
          .willReturn(notFound())
      )

      val result = connector.list(request)(using hc).futureValue
      result must not be defined
    }
  }

  "submitAssumedReporting" - {

    val assumingOperator = AssumingPlatformOperator(
      name = "assumingOperator",
      residentCountry = "US",
      tinDetails = Seq(
        TinDetails(
          tin = "tin3",
          tinType = Utr,
          issuedBy = "GB"
        ),
        TinDetails(
          tin = "tin4",
          tinType = Vrn,
          issuedBy = "GB"
        )
      ),
      registeredCountry = "GB",
      address = "address"
    )

    val request = AssumedReportingSubmissionRequest(
      operatorId = "operatorId",
      assumingOperator = assumingOperator,
      reportingPeriod = Year.of(2024)
    )

    "must submit the expected data and return the created submission" in {

      val expectedSubmission = Submission(
        _id = "id",
        submissionType = SubmissionType.ManualAssumedReport,
        operatorId = "operatorId",
        operatorName = "operatorName",
        assumingOperatorName = Some("assumedOperatorName"),
        dprsId = "dprsId",
        state = Submitted(fileName = "test.xml", Year.of(2024)),
        created = now,
        updated = now
      )

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/assumed/submit"))
          .withRequestBody(equalToJson(Json.toJson(request).toString))
          .withHeader("User-Agent", equalTo("app"))
          .willReturn(ok(Json.toJson(expectedSubmission).toString))
      )

      val result = connector.submitAssumedReporting(request)(using hc).futureValue
      result mustEqual expectedSubmission
    }

    "must return an error when the server response with another status" in {

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/assumed/submit"))
          .withRequestBody(equalToJson(Json.toJson(request).toString))
          .withHeader("User-Agent", equalTo("app"))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      val result = connector.submitAssumedReporting(request)(using hc).failed.futureValue
      result mustBe SubmitAssumedReportingFailure
    }
  }
}
