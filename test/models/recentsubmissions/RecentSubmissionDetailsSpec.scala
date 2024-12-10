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

package models.recentsubmissions

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

import java.time.{LocalDate, ZoneOffset}

class RecentSubmissionDetailsSpec extends AnyFreeSpec with Matchers {

  private val validJson = Json.obj(
    "operatorDetails" -> Json.obj(
      "operator-id-one" -> Json.obj("operatorId" -> "operator-id-one", "businessDetailsCorrect" -> true),
      "operator-id-two" -> Json.obj("operatorId" -> "operator-id-two", "businessDetailsCorrect" -> false),
      "operator-id-three" -> Json.obj("operatorId" -> "operator-id-three", "businessDetailsCorrect" -> true, "reportingNotificationsCorrect" -> true)
    ),
    "yourContactDetailsCorrect" -> true
  )

  private val validModel = RecentSubmissionDetails(
    operatorDetails = Map(
      "operator-id-one" -> OperatorSubmissionDetails("operator-id-one", true, None),
      "operator-id-two" -> OperatorSubmissionDetails("operator-id-two", false, None),
      "operator-id-three" -> OperatorSubmissionDetails("operator-id-three", true, Some(true))
    ),
    yourContactDetailsCorrect = Some(true)
  )


  "RecentSubmissionDetails.format" - {
    "parse from json" in {
      validJson.as[RecentSubmissionDetails] mustBe validModel
    }

    "parse to json" in {
      Json.toJson(validModel) mustBe validJson
    }
  }
}
