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

package models.audit

import models.operator.{TinDetails, TinType}
import models.submission.{AssumedReportingSubmissionRequest, AssumingPlatformOperator}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

import java.time.{Instant, Year}
import java.util.UUID

class AddAssumedReportEventSpec extends AnyFreeSpec with Matchers {

  "must write to the correct json structure" - {

    val dprsId = "dprsId"
    val operatorId = "operatorId"
    val operatorName = "operatorName"
    val assumingOperatorName = "assumingOperatorName"
    val address = "address"
    val successfulStatusCode = 200
    val errorStatusCode = 500
    val processedAt = Instant.now()
    val conversationId = UUID.randomUUID().toString
    val baseSubmission = AssumedReportingSubmissionRequest(
      operatorId = operatorId,
      assumingOperator = AssumingPlatformOperator(
        name = assumingOperatorName,
        residentCountry = "GB",
        tinDetails = Nil,
        registeredCountry = "GB",
        address = address
      ),
      reportingPeriod = Year.of(2024)
    )
    
    "for a UK tax resident assuming operator with a tax identifier" in {
      
      val submission = baseSubmission.copy(
        assumingOperator = baseSubmission.assumingOperator.copy(
          tinDetails = Seq(TinDetails("tin", TinType.Other, "GB"))
        )
      )
      
      val event = AddAssumedReportEvent(dprsId, operatorName, submission, successfulStatusCode, processedAt, Some(conversationId))
      val expectedJson = Json.obj(
        "platformOperator" -> operatorName,
        "platformOperatorId" -> operatorId,
        "digitalPlatformReportingId" -> dprsId,
        "reportingPeriod" -> "2024",
        "assumingPlatformOperatorName" -> assumingOperatorName,
        "ukTaxResident" -> true,
        "countryOfTaxResidence" -> "GB",
        "hasUkTaxIdentificationNumber" -> true,
        "ukTaxIdentificationNumber" -> "tin",
        "registeredAddress" -> address,
        "outcome" -> Json.obj(
          "isSuccessful" -> true,
          "statusCode" -> successfulStatusCode,
          "conversationId" -> conversationId,
          "processedAt" -> processedAt
        )
      )

      Json.toJson(event) mustEqual expectedJson
    }
    
    "for a UK tax resident assuming operator without a tax identifier" in {

      val event = AddAssumedReportEvent(dprsId, operatorName, baseSubmission, errorStatusCode, processedAt, None)
      val expectedJson = Json.obj(
        "platformOperator" -> operatorName,
        "platformOperatorId" -> operatorId,
        "digitalPlatformReportingId" -> dprsId,
        "reportingPeriod" -> "2024",
        "assumingPlatformOperatorName" -> assumingOperatorName,
        "ukTaxResident" -> true,
        "countryOfTaxResidence" -> "GB",
        "hasUkTaxIdentificationNumber" -> false,
        "registeredAddress" -> address,
        "outcome" -> Json.obj(
          "isSuccessful" -> false,
          "statusCode" -> errorStatusCode,
          "processedAt" -> processedAt
        )
      )

      Json.toJson(event) mustEqual expectedJson
    }

    "for an internationally tax resident assuming operator with a tax identifier" in {
      
      val submission = baseSubmission.copy(
        assumingOperator = baseSubmission.assumingOperator.copy(
          residentCountry = "US",
          tinDetails = Seq(TinDetails("tin", TinType.Other, "US"))
        )
      )

      val event = AddAssumedReportEvent(dprsId, operatorName, submission, successfulStatusCode, processedAt, Some(conversationId))
      val expectedJson = Json.obj(
        "platformOperator" -> operatorName,
        "platformOperatorId" -> operatorId,
        "digitalPlatformReportingId" -> dprsId,
        "reportingPeriod" -> "2024",
        "assumingPlatformOperatorName" -> assumingOperatorName,
        "ukTaxResident" -> false,
        "countryOfTaxResidence" -> "US",
        "hasInternationalTaxIdentificationNumber" -> true,
        "internationalTaxIdentificationNumber" -> "tin",
        "registeredAddress" -> address,
        "outcome" -> Json.obj(
          "isSuccessful" -> true,
          "statusCode" -> successfulStatusCode,
          "conversationId" -> conversationId,
          "processedAt" -> processedAt
        )
      )

      Json.toJson(event) mustEqual expectedJson
    }

    "for an internationally tax resident assuming operator without a tax identifier" in {

      val submission = baseSubmission.copy(
        assumingOperator = baseSubmission.assumingOperator.copy(
          residentCountry = "US"
        )
      )

      val event = AddAssumedReportEvent(dprsId, operatorName, submission, successfulStatusCode, processedAt, Some(conversationId))
      val expectedJson = Json.obj(
        "platformOperator" -> operatorName,
        "platformOperatorId" -> operatorId,
        "digitalPlatformReportingId" -> dprsId,
        "reportingPeriod" -> "2024",
        "assumingPlatformOperatorName" -> assumingOperatorName,
        "ukTaxResident" -> false,
        "countryOfTaxResidence" -> "US",
        "hasInternationalTaxIdentificationNumber" -> false,
        "registeredAddress" -> address,
        "outcome" -> Json.obj(
          "isSuccessful" -> true,
          "statusCode" -> successfulStatusCode,
          "conversationId" -> conversationId,
          "processedAt" -> processedAt
        )
      )

      Json.toJson(event) mustEqual expectedJson
    }
  }
}
