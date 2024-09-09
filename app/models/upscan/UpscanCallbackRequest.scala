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

import java.time.Instant

sealed trait UpscanCallbackRequest

object UpscanCallbackRequest {

  final case class Ready(
                          reference: String,
                          downloadUrl: String,
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
                                 failureReason: FailureReason,
                                 message: String
                               )

  sealed trait FailureReason extends EnumEntry with Uppercase

  object FailureReason extends PlayEnum[FailureReason] {

    override lazy val values: IndexedSeq[FailureReason] = findValues

    case object Quarantine extends FailureReason
    case object Rejected extends FailureReason
    case object Unknown extends FailureReason
    case object Duplicate extends FailureReason
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