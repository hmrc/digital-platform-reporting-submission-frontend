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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

import java.util.UUID
import java.time.Instant

class DeleteAssumedReportEventSpec extends AnyFreeSpec with Matchers {

  "must write to the correct json structure" - {

    "with a successful status code" in {

      val conversationId = UUID.randomUUID().toString
      val now = Instant.now()

      val event = DeleteAssumedReportEvent(
        dprsId = "dprsId",
        operatorId = "operatorId",
        operatorName = "operatorName",
        conversationId = conversationId,
        statusCode = 200,
        processedAt = now
      )

      val expectedJson = Json.obj(
        "digitalPlatformReportingId" -> "dprsId",
        "platformOperatorId" -> "operatorId",
        "platformOperator" -> "operatorName",
        "conversationId" -> conversationId,
        "outcome" -> Json.obj(
          "isSuccessful" -> true,
          "statusCode" -> 200,
          "processedAt" -> now
        )
      )

      Json.toJson(event) mustEqual expectedJson
    }

    "with an error status code" in {

      val conversationId = UUID.randomUUID().toString
      val now = Instant.now()

      val event = DeleteAssumedReportEvent(
        dprsId = "dprsId",
        operatorId = "operatorId",
        operatorName = "operatorName",
        conversationId = conversationId,
        statusCode = 500,
        processedAt = now
      )

      val expectedJson = Json.obj(
        "digitalPlatformReportingId" -> "dprsId",
        "platformOperatorId" -> "operatorId",
        "platformOperator" -> "operatorName",
        "conversationId" -> conversationId,
        "outcome" -> Json.obj(
          "isSuccessful" -> false,
          "statusCode" -> 500,
          "processedAt" -> now
        )
      )

      Json.toJson(event) mustEqual expectedJson
    }
  }
}
