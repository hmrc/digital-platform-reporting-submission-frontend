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

package models.email.requests

import base.SpecBase
import builders.AssumedReportSummaryBuilder.anAssumedReportSummary
import builders.PlatformOperatorBuilder.aPlatformOperator

import java.time.Instant

class UpdateAssumedReportingPlatformOperatorSpec extends SpecBase {

  private val completedDateTime: Instant = Instant.parse("2100-12-31T00:01:00Z")
  private val completedDateTimeString = "12:01am GMT on 31 December 2100"

  ".apply(...)" - {
    "must create UpdateAssumedReportingPlatformOperator object" in {
      UpdateAssumedReportingPlatformOperator.apply(
        platformOperator = aPlatformOperator,
        assumedReportSummary = anAssumedReportSummary,
        updatedInstant = completedDateTime
      ) mustBe UpdateAssumedReportingPlatformOperator(
        to = List(aPlatformOperator.primaryContactDetails.emailAddress),
        templateId = "dprs_update_assumed_reporting_platform_operator",
        parameters = Map(
          "poPrimaryContactName" -> aPlatformOperator.primaryContactDetails.contactName,
          "checksCompletedDateTime" -> completedDateTimeString,
          "assumingPlatformOperator" -> anAssumedReportSummary.assumingOperatorName,
          "poBusinessName" -> anAssumedReportSummary.operatorName,
          "reportingPeriod" -> anAssumedReportSummary.reportingPeriod.toString
        )
      )
    }
  }

}
