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
import connectors.AssumedReportingConnector.*
import models.operator.TinDetails
import models.operator.TinType.{Utr, Vrn}
import models.submission.*
import models.submission.Submission.State.Submitted
import models.submission.Submission.SubmissionType
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}

import java.time.{Instant, Year}

class AssumedReportingConnectorSpec
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

  private lazy val connector = app.injector.instanceOf[AssumedReportingConnector]

  private val now: Instant = Instant.now()
  private val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("auth")))

  "submit" - {

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

      val result = connector.submit(request)(using hc).futureValue
      result mustEqual expectedSubmission
    }

    "must return an error when the server response with another status" in {

      wireMockServer.stubFor(
        post(urlPathEqualTo("/digital-platform-reporting/submission/assumed/submit"))
          .withRequestBody(equalToJson(Json.toJson(request).toString))
          .withHeader("User-Agent", equalTo("app"))
          .willReturn(serverError())
      )

      val result = connector.submit(request)(using hc).failed.futureValue
      result mustBe SubmitAssumedReportingFailure
    }
  }

  "get" - {

    val assumingOperator = AssumingPlatformOperator(
      name = "assumingOperator",
      residentCountry = "US",
      tinDetails = Seq(
        TinDetails(
          tin = "tin3",
          tinType = Utr,
          issuedBy = "GB"
        )
      ),
      registeredCountry = "GB",
      address = "address"
    )

    val submission = AssumedReportingSubmission("operatorId", "operatorName", assumingOperator, Year.of(2024), false)

    "must return a submission when the server returns OK" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/submission/assumed/operatorId/2024"))
          .willReturn(ok(Json.toJson(submission).toString))
      )

      val result = connector.get("operatorId", Year.of(2024))(using hc).futureValue
      result.value mustEqual submission
    }

    "must return None when the server returns NOT_FOUND" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/submission/assumed/operatorId/2024"))
          .willReturn(notFound())
      )

      val result = connector.get("operatorId", Year.of(2024))(using hc).futureValue
      result must not be defined
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/submission/assumed/operatorId/2024"))
          .willReturn(serverError())
      )

      val result = connector.get("operatorId", Year.of(2024))(using hc).failed.futureValue
      result mustBe a[GetAssumedReportFailure.type]
    }
  }

  "delete" - {

    "must send a delete request to the backend" in {

      wireMockServer.stubFor(
        delete(urlPathEqualTo("/digital-platform-reporting/submission/assumed/operatorId/2024"))
          .willReturn(ok())
      )

      connector.delete("operatorId", Year.of(2024))(using hc).futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        delete(urlPathEqualTo("/digital-platform-reporting/submission/assumed/operatorId/2024"))
          .willReturn(serverError())
      )

      val result = connector.delete("operatorId", Year.of(2024))(using hc).failed.futureValue
      result mustBe a[DeleteAssumedReportFailure]

      val failure = result.asInstanceOf[DeleteAssumedReportFailure]
      failure.status mustEqual 500
    }
  }
  
  "list" - {
    
    "must return submission summaries when the server returns OK and some submissions" in {

      val submission = AssumedReportingSubmissionSummary(
        submissionId = "submissionId",
        fileName = "filename",
        operatorId = "operatorId",
        operatorName = "operatorName",
        reportingPeriod = Year.of(2024),
        submissionDateTime = now,
        submissionStatus = SubmissionStatus.Success,
        assumingReporterName = Some("assumedReporterName"),
        submissionCaseId = Some("submissionCaseId"),
        isDeleted = false
      )
      
      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/submission/assumed"))
          .withHeader("User-Agent", equalTo("app"))
          .willReturn(ok(Json.arr(Json.toJson(submission)).toString))
      )

      val result = connector.list(using hc).futureValue
      result mustEqual Seq(submission)
    }

    "must return an empty sequence when the server returns NOT_FOUND" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/submission/assumed"))
          .withHeader("User-Agent", equalTo("app"))
          .willReturn(notFound())
      )

      val result = connector.list(using hc).futureValue
      result mustEqual Nil
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlPathEqualTo("/digital-platform-reporting/submission/assumed"))
          .withHeader("User-Agent", equalTo("app"))
          .willReturn(serverError())
      )

      val result = connector.list(using hc).failed.futureValue
      result mustBe a[ListAssumedReportsFailure.type]
    }
  }
}
