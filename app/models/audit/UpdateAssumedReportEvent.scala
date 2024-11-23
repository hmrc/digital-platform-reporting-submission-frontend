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

import models.submission.{AssumedReportingSubmission, AssumedReportingSubmissionRequest, AssumingPlatformOperator}
import play.api.libs.json.{JsObject, Json, OWrites}

final case class UpdateAssumedReportEvent(
                                           dprsId: String,
                                           original: AssumedReportingSubmission,
                                           updated: AssumedReportingSubmissionRequest
                                         ) extends AuditEvent {

  override def auditType: String = "UpdateAssumedReport"
}

object UpdateAssumedReportEvent {

  given OWrites[UpdateAssumedReportEvent] = new OWrites[UpdateAssumedReportEvent] {
    override def writes(o: UpdateAssumedReportEvent): JsObject = {

      val originalJson = toJson(o.original.assumingOperator)
      val updatedJson = toJson(o.updated.assumingOperator)

      val changedFieldsInOriginal = JsObject(originalJson.fieldSet.diff(updatedJson.fieldSet).toSeq)
      val changedFieldsInUpdated = JsObject(updatedJson.fieldSet.diff(originalJson.fieldSet).toSeq)

      Json.obj(
        "digitalPlatformReportingId" -> o.dprsId,
        "platformOperatorId" -> o.original.operatorId,
        "platformOperator" -> o.original.operatorName,
        "from" -> changedFieldsInOriginal,
        "to" -> changedFieldsInUpdated
      )
    }
  }

  private def toJson(assumingOperator: AssumingPlatformOperator): JsObject = {

    val taxIdentifierJson = assumingOperator.residentCountry match {
      case "GB" =>
        Json.obj(
          "isUkTaxResident" -> true,
          "countryOfTaxResidence" -> "GB",
          "hasUkTaxIdentificationNumber" -> assumingOperator.tinDetails.nonEmpty
        ) ++ assumingOperator.tinDetails.headOption.map { tin =>
          Json.obj("ukTaxIdentificationNumber" -> tin.tin)
        }.getOrElse(Json.obj())

      case country =>
        Json.obj(
          "isUkTaxResident" -> false,
          "countryOfTaxResidence" -> country,
          "hasInternationalTaxIdentificationNumber" -> assumingOperator.tinDetails.nonEmpty
        ) ++ assumingOperator.tinDetails.headOption.map { tin =>
          Json.obj("internationalTaxIdentificationNumber" -> tin.tin)
        }.getOrElse(Json.obj())
    }

    Json.obj(
      "assumingPlatformOperatorName" -> assumingOperator.name,
      "registeredAddress" -> assumingOperator.address
    ) ++ taxIdentifierJson
  }
}
