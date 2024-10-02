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

package models.submission

import play.api.libs.json.*

import java.net.URL
import java.time.{Instant, Year}
import models.{urlFormat, yearFormat}

final case class Submission(
                             _id: String,
                             dprsId: String,
                             state: Submission.State,
                             created: Instant,
                             updated: Instant
                           )

object Submission {

  sealed trait State extends Product with Serializable

  object State {

    case object Ready extends State
    case object Uploading extends State
    final case class UploadFailed(reason: String) extends State
    final case class Validated(downloadUrl: URL, platformOperatorId: String, reportingPeriod: Year, fileName: String, checksum: String, size: Long) extends State
    final case class Submitted(fileName: String) extends State
    case object Approved extends State
    case object Rejected extends State

    private def singletonOFormat[A](a: A): OFormat[A] =
      OFormat(Reads.pure(a), OWrites[A](_ => Json.obj()))

    private implicit lazy val readyFormat: OFormat[Ready.type] = singletonOFormat(Ready)
    private implicit lazy val uploadFailedFormat: OFormat[UploadFailed] = Json.format
    private implicit lazy val uploadingFormat: OFormat[Uploading.type] = singletonOFormat(Uploading)
    private implicit lazy val validatedFormat: OFormat[Validated] = Json.format
    private implicit lazy val submittedFormat: OFormat[Submitted] = Json.format
    private implicit lazy val approvedFormat: OFormat[Approved.type] = singletonOFormat(Approved)
    private implicit lazy val rejectedFormat: OFormat[Rejected.type] = Json.format

    private implicit val jsonConfig: JsonConfiguration = JsonConfiguration(
      discriminator = "type",
      typeNaming = _.split("\\.").last
    )

    implicit lazy val format: OFormat[State] = Json.format
  }

  implicit lazy val format: OFormat[Submission] = Json.format
}