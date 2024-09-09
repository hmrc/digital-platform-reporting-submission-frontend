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

package models.upscan

import models.upscan.UpscanCallbackRequest.FailureReason.Rejected
import models.upscan.UpscanCallbackRequest.{ErrorDetails, UploadDetails}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

import java.time.Instant

class UpscanCallbackRequestSpec extends AnyFreeSpec with Matchers {

  val now = Instant.now()

  "Ready" - {

    val callback = UpscanCallbackRequest.Ready(
      reference = "reference",
      downloadUrl = "downloadUrl",
      uploadDetails = UploadDetails(
        uploadTimestamp = now,
        checksum = "checksum",
        fileMimeType = "mimetype",
        fileName = "filename",
        size = 1024
      )
    )

    val json = Json.obj(
      "fileStatus" -> "READY",
      "reference" -> "reference",
      "downloadUrl" -> "downloadUrl",
      "uploadDetails" -> Json.obj(
        "uploadTimestamp" -> now,
        "checksum" -> "checksum",
        "fileMimeType" -> "mimetype",
        "fileName" -> "filename",
        "size" -> 1024
      )
    )

    "must read from json" in {
      Json.fromJson[UpscanCallbackRequest](json).get mustEqual callback
    }

    "must write to json" in {
      Json.toJsObject[UpscanCallbackRequest](callback) mustEqual json
    }
  }

  "Failed" - {

    val callback = UpscanCallbackRequest.Failed(
      reference = "reference",
      failureDetails = ErrorDetails(
        failureReason = Rejected,
        message = "message"
      )
    )

    val json = Json.obj(
      "fileStatus" -> "FAILED",
      "reference" -> "reference",
      "failureDetails" -> Json.obj(
        "failureReason" -> "REJECTED",
        "message" -> "message"
      )
    )

    "must read from json" in {
      Json.fromJson[UpscanCallbackRequest](json).get mustEqual callback
    }

    "must write to json" in {
      Json.toJsObject[UpscanCallbackRequest](callback) mustEqual json
    }
  }
}
