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

import models.operator.TinDetails
import models.operator.TinType.Other
import models.submission.{AssumedReportingSubmission, AssumedReportingSubmissionRequest, AssumingPlatformOperator}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

import java.time.Year

class UpdateAssumedReportEventSpec extends AnyFreeSpec with Matchers {

  private val operatorId = "operatorId"
  private val operatorName = "operatorName"
  private val reportingPeriod = Year.of(2024)
  private val baseAssumingOperator = AssumingPlatformOperator(
    name = "name",
    residentCountry = "GB",
    tinDetails = Nil,
    registeredCountry = "GB",
    address = "address"
  )
  
  "must write details to the correct json structure" - {
    
    "when basic details have changed" in {
      
      val original = AssumedReportingSubmission(operatorId, operatorName, baseAssumingOperator, reportingPeriod, false)
      val newAssumingOperator = baseAssumingOperator.copy(
        name = "new name",
        address = "new address"
      )
      val updated = AssumedReportingSubmissionRequest(operatorId, newAssumingOperator, reportingPeriod)
      
      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "assumingPlatformOperatorName" -> "name",
          "registeredAddress" -> "address"
        ),
        "to" -> Json.obj(
          "assumingPlatformOperatorName" -> "new name",
          "registeredAddress" -> "new address"
        )
      )
      
      val event = UpdateAssumedReportEvent(original, updated)
      
      Json.toJson(event) mustEqual expectedJson
    }

    "when a UK tax identifier has been added" in {

      val original = AssumedReportingSubmission(operatorId, operatorName, baseAssumingOperator, reportingPeriod, false)
      val newAssumingOperator = baseAssumingOperator.copy(
        tinDetails = Seq(TinDetails("tin", Other, "GB"))
      )
      val updated = AssumedReportingSubmissionRequest(operatorId, newAssumingOperator, reportingPeriod)

      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "hasUkTaxIdentificationNumber" -> false
        ),
        "to" -> Json.obj(
          "hasUkTaxIdentificationNumber" -> true,
          "ukTaxIdentificationNumber" -> "tin"
        )
      )

      val event = UpdateAssumedReportEvent(original, updated)

      Json.toJson(event) mustEqual expectedJson
    }

    "when a UK tax identifier has been removed" in {

      val oldAssumingOperator = baseAssumingOperator.copy(
        tinDetails = Seq(TinDetails("tin", Other, "GB"))
      )
      val original = AssumedReportingSubmission(operatorId, operatorName, oldAssumingOperator, reportingPeriod, false)
      val updated = AssumedReportingSubmissionRequest(operatorId, baseAssumingOperator, reportingPeriod)

      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "hasUkTaxIdentificationNumber" -> true,
          "ukTaxIdentificationNumber" -> "tin"
        ),
        "to" -> Json.obj(
          "hasUkTaxIdentificationNumber" -> false
        )
      )

      val event = UpdateAssumedReportEvent(original, updated)

      Json.toJson(event) mustEqual expectedJson
    }

    "when the assuming operator has changed from being UK tax resident to internationally tax resident" in {

      val original = AssumedReportingSubmission(operatorId, operatorName, baseAssumingOperator, reportingPeriod, false)
      val newAssumingOperator = baseAssumingOperator.copy(
        residentCountry = "US"
      )
      val updated = AssumedReportingSubmissionRequest(operatorId, newAssumingOperator, reportingPeriod)

      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "ukTaxResident" -> true,
          "countryOfTaxResidence" -> "GB",
          "hasUkTaxIdentificationNumber" -> false
        ),
        "to" -> Json.obj(
          "ukTaxResident" -> false,
          "countryOfTaxResidence" -> "US",
          "hasInternationalTaxIdentificationNumber" -> false
        )
      )

      val event = UpdateAssumedReportEvent(original, updated)

      Json.toJson(event) mustEqual expectedJson
    }

    "when an international tax identifier has been added" in {
      
      val oldAssumingOperator = baseAssumingOperator.copy(
        residentCountry = "US"
      )
      val newAssumingOperator = baseAssumingOperator.copy(
        residentCountry = "US",
        tinDetails = Seq(TinDetails("tin", Other, "US"))
      )
      val original = AssumedReportingSubmission(operatorId, operatorName, oldAssumingOperator, reportingPeriod, false)
      val updated = AssumedReportingSubmissionRequest(operatorId, newAssumingOperator, reportingPeriod)

      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "hasInternationalTaxIdentificationNumber" -> false
        ),
        "to" -> Json.obj(
          "hasInternationalTaxIdentificationNumber" -> true,
          "internationalTaxIdentificationNumber" -> "tin"
        )
      )

      val event = UpdateAssumedReportEvent(original, updated)

      Json.toJson(event) mustEqual expectedJson
    }

    "when an international tax identifier has been removed" in {

      val oldAssumingOperator = baseAssumingOperator.copy(
        residentCountry = "US",
        tinDetails = Seq(TinDetails("tin", Other, "US"))
      )
      val newAssumingOperator = baseAssumingOperator.copy(
        residentCountry = "US"
      )
      val original = AssumedReportingSubmission(operatorId, operatorName, oldAssumingOperator, reportingPeriod, false)
      val updated = AssumedReportingSubmissionRequest(operatorId, newAssumingOperator, reportingPeriod)

      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "hasInternationalTaxIdentificationNumber" -> true,
          "internationalTaxIdentificationNumber" -> "tin"
        ),
        "to" -> Json.obj(
          "hasInternationalTaxIdentificationNumber" -> false
        )
      )

      val event = UpdateAssumedReportEvent(original, updated)

      Json.toJson(event) mustEqual expectedJson
    }

    "when the assuming operator has changed from being internationally tax resident to UK tax resident" in {
      
      val oldAssumingOperator = baseAssumingOperator.copy(
        residentCountry = "US"
      )
      val original = AssumedReportingSubmission(operatorId, operatorName, oldAssumingOperator, reportingPeriod, false)
      val updated = AssumedReportingSubmissionRequest(operatorId, baseAssumingOperator, reportingPeriod)

      val expectedJson = Json.obj(
        "from" -> Json.obj(
          "ukTaxResident" -> false,
          "countryOfTaxResidence" -> "US",
          "hasInternationalTaxIdentificationNumber" -> false
        ),
        "to" -> Json.obj(
          "ukTaxResident" -> true,
          "countryOfTaxResidence" -> "GB",
          "hasUkTaxIdentificationNumber" -> false
        )
      )

      val event = UpdateAssumedReportEvent(original, updated)

      Json.toJson(event) mustEqual expectedJson
    }
  }
}
