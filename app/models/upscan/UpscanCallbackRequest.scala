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

import enumeratum.{EnumEntry, PlayEnum}
import enumeratum.EnumEntry.Uppercase
import play.api.libs.json.*

import java.net.URL
import java.time.Instant
import models.urlFormat

sealed trait UpscanCallbackRequest {
  def reference: String
}

object UpscanCallbackRequest {

  final case class Ready(
                          reference: String,
                          downloadUrl: URL,
                          uploadDetails: UploadDetails
                        ) extends UpscanCallbackRequest

  final case class Failed(
                           reference: String,
                           failureDetails: ErrorDetails
                         ) extends UpscanCallbackRequest

  final case class UploadDetails(
                                  uploadTimestamp: Instant,
                                  checksum: String,
                                  fileMimeType: String,
                                  fileName: String,
                                  size: Long
                                )

  final case class ErrorDetails(
                                 failureReason: UpscanFailureReason,
                                 message: String
                               )

  sealed trait UpscanFailureReason extends EnumEntry with Uppercase

  object UpscanFailureReason extends PlayEnum[UpscanFailureReason] {

    override lazy val values: IndexedSeq[UpscanFailureReason] = findValues

    case object Quarantine extends UpscanFailureReason
    case object Rejected extends UpscanFailureReason
    case object Unknown extends UpscanFailureReason
    case object Duplicate extends UpscanFailureReason
  }

  given OFormat[UploadDetails] = Json.format
  given OFormat[ErrorDetails] = Json.format
  given OFormat[Ready] = Json.format
  given OFormat[Failed] = Json.format

  given OFormat[UpscanCallbackRequest] = {
    given JsonConfiguration = JsonConfiguration(
      discriminator = "fileStatus",
      typeNaming = _.split("\\.").last.toUpperCase
    )
    Json.format
  }
}
