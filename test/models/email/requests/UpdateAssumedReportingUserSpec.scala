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

class UpdateAssumedReportingUserSpec extends SpecBase {

  ".apply(...)" - {
    "must create UpdateAssumedReportingUser object" in {
      UpdateAssumedReportingUser.apply(
        email = "some.email@example.com",
        name = "some-primary-contact-name",
        checksCompletedDateTime = "9:15am (GMT) on 17th November 2024",
        assumingPlatformOperator = "some-platform-operator",
        businessName = "some-business-name",
        reportingPeriod = "2024"
      ) mustBe UpdateAssumedReportingUser(
        to = List("some.email@example.com"),
        templateId = "dprs_update_assumed_reporting_user",
        parameters = Map(
          "userPrimaryContactName" -> "some-primary-contact-name",
          "checksCompletedDateTime" -> "9:15am (GMT) on 17th November 2024",
          "assumingPlatformOperator" -> "some-platform-operator",
          "poBusinessName" -> "some-business-name",
          "reportingPeriod" -> "2024"
        )
      )
    }
  }

}
