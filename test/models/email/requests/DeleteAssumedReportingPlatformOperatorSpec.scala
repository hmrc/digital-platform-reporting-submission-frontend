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

class DeleteAssumedReportingPlatformOperatorSpec extends SpecBase {

  ".apply(...)" - {
    "must create DeleteAssumedReportingPlatformOperator object" in {
      DeleteAssumedReportingPlatformOperator.apply(
        email = "some.email@example.com",
        platformOperatorContactName = "some-contact-name",
        checksCompletedDateTime = "9:15am (GMT) on 17th November 2024",
        assumingPlatformOperator = "some-assuming-platform-operator",
        businessName = "some-business-name",
        reportingPeriod = "2024"
      ) mustBe DeleteAssumedReportingPlatformOperator(
        to = List("some.email@example.com"),
        templateId = "dprs_delete_assumed_reporting_platform_operator",
        parameters = Map(
          "poPrimaryContactName" -> "some-contact-name",
          "checksCompletedDateTime" -> "9:15am (GMT) on 17th November 2024",
          "assumingPlatformOperator" -> "some-assuming-platform-operator",
          "poBusinessName" -> "some-business-name",
          "reportingPeriod" -> "2024"
        )
      )
    }
  }

}
