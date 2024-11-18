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

import models.submission.AssumedReportingSubmissionRequest
import play.api.libs.json.{Json, JsObject, OWrites}

import java.time.Instant

final case class AddAssumedReportEvent(
                                        dprsId: String,
                                        operatorName: String,
                                        submission: AssumedReportingSubmissionRequest,
                                        statusCode: Int,
                                        processedAt: Instant,
                                        conversationId: String
                                      ) extends AuditEvent {

  override def auditType: String = "AddAssumedReport"
}

object AddAssumedReportEvent {
  
  given OWrites[AddAssumedReportEvent] = new OWrites[AddAssumedReportEvent] {
    override def writes(o: AddAssumedReportEvent): JsObject = {
      
      val taxIdentifierJson = o.submission.assumingOperator.residentCountry match {
        case "GB" =>
          Json.obj(
            "ukTaxResident" -> true,
            "countryOfTaxResidence" -> "GB",
            "hasUkTaxIdentificationNumber" -> o.submission.assumingOperator.tinDetails.nonEmpty
          ) ++ o.submission.assumingOperator.tinDetails.headOption.map { tin =>
            Json.obj("ukTaxIdentificationNumber" -> tin.tin)
          }.getOrElse(Json.obj())

        case country =>
          Json.obj(
            "ukTaxResident" -> false,
            "countryOfTaxResidence" -> country,
            "hasInternationalTaxIdentificationNumber" -> o.submission.assumingOperator.tinDetails.nonEmpty
          ) ++ o.submission.assumingOperator.tinDetails.headOption.map { tin =>
            Json.obj("internationalTaxIdentificationNumber" -> tin.tin)
          }.getOrElse(Json.obj())
      }
      
      Json.obj(
        "platformOperator" -> o.operatorName,
        "platformOperatorId" -> o.submission.operatorId,
        "digitalPlatformReportingId" -> o.dprsId,
        "reportingPeriod" -> o.submission.reportingPeriod.toString,
        "assumingPlatformOperatorName" -> o.submission.assumingOperator.name,
        "registeredAddress" -> o.submission.assumingOperator.address,
        "outcome" -> Json.obj(
          "isSuccessful" -> (o.statusCode == 200),
          "statusCode" -> o.statusCode,
          "conversationId" -> o.conversationId,
          "processedAt" -> o.processedAt
        )
      ) ++ taxIdentifierJson
    }
  }
}
