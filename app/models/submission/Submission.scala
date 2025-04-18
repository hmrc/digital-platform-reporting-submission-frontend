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

import models.submission.Submission.SubmissionType
import models.submission.Submission.UploadFailureReason.SchemaValidationError
import models.upscan.UpscanCallbackRequest.UpscanFailureReason
import models.{urlFormat, yearFormat}
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*

import java.net.URL
import java.time.{Instant, Year}

final case class Submission(
                             _id: String,
                             submissionType: SubmissionType,
                             dprsId: String,
                             operatorId: String,
                             operatorName: String,
                             assumingOperatorName: Option[String],
                             state: Submission.State,
                             created: Instant,
                             updated: Instant
                           )

object Submission {

  sealed trait SubmissionType extends Product with Serializable

  object SubmissionType {

    case object Xml extends SubmissionType
    case object ManualAssumedReport extends SubmissionType

    given Format[SubmissionType] = {

      val reads: Reads[SubmissionType] =
        __.read[String].flatMap {
          case "Xml" => Reads.pure(Xml)
          case "ManualAssumedReport" => Reads.pure(ManualAssumedReport)
          case _ => Reads.failed("Invalid submission type")
        }

      val writes: Writes[SubmissionType] =
        Writes { submissionType =>
          JsString(submissionType.toString)
        }

      Format(reads, writes)
    }
  }

  sealed trait UploadFailureReason {
    val errorMessageKey: String
  }

  object UploadFailureReason {

    case object NotXml extends UploadFailureReason {
      val errorMessageKey = "uploadFailed.error.NotXml"
    }

    final case class SchemaValidationError(errors: Seq[SchemaValidationError.Error], moreErrors: Boolean) extends UploadFailureReason {
      val errorMessageKey = "uploadFailed.error.SchemaValidationError"
    }

    case object ManualAssumedReportExists extends UploadFailureReason {
      val errorMessageKey = "uploadFailed.error.ManualAssumedReportExists"
    }

    case object PlatformOperatorIdMissing extends UploadFailureReason {
      val errorMessageKey = "uploadFailed.error.PlatformOperatorIdMissing"
    }

    final case class PlatformOperatorIdMismatch(expectedId: String, actualId: String) extends UploadFailureReason {
      val errorMessageKey = "uploadFailed.error.PlatformOperatorIdMismatch"
    }

    case object ReportingPeriodInvalid extends UploadFailureReason {
      val errorMessageKey = "uploadFailed.error.ReportingPeriodInvalid"
    }

    final case class UpscanError(failureReason: UpscanFailureReason) extends UploadFailureReason {
      val errorMessageKey = s"uploadFailed.error.${failureReason.entryName}"
    }

    case object EntityTooLarge extends UploadFailureReason {
      val errorMessageKey = "uploadFailed.error.EntityTooLarge"
    }

    case object EntityTooSmall extends UploadFailureReason {
      val errorMessageKey = "uploadFailed.error.EntityTooSmall"
    }

    case object InvalidArgument extends UploadFailureReason {
      val errorMessageKey = "uploadFailed.error.InvalidArgument"
    }

    case object UnknownFailure extends UploadFailureReason {
      val errorMessageKey = "uploadFailed.error.UnknownFailure"
    }

    case object InvalidFileNameExtension extends UploadFailureReason {
      val errorMessageKey = "uploadFailed.error.NotXml"
    }

    case object FileNameTooLong extends UploadFailureReason {
      val errorMessageKey = "uploadFailed.error.long.filename"
    }  

    object SchemaValidationError {

      final case class Error(line: Int, col: Int, message: String)

      given OFormat[Error] = Json.format

      given OFormat[SchemaValidationError] = {

        val reads = (
          (__ \ "errors").readWithDefault(Seq.empty[Error]) ~
            (__ \ "moreErrors").readWithDefault(false)
          )(SchemaValidationError.apply)

        val writes = Json.writes[SchemaValidationError]

        OFormat(reads, writes)
      }
    }

    private given OFormat[NotXml.type] = singletonOFormat(NotXml)
    private given OFormat[ManualAssumedReportExists.type] = singletonOFormat(ManualAssumedReportExists)
    private given OFormat[PlatformOperatorIdMissing.type] = singletonOFormat(PlatformOperatorIdMissing)
    private given OFormat[PlatformOperatorIdMismatch] = Json.format
    private given OFormat[ReportingPeriodInvalid.type] = singletonOFormat(ReportingPeriodInvalid)
    private given OFormat[UpscanError] = Json.format
    private given OFormat[EntityTooSmall.type] = singletonOFormat(EntityTooSmall)
    private given OFormat[EntityTooLarge.type] = singletonOFormat(EntityTooLarge)
    private given OFormat[InvalidArgument.type] = singletonOFormat(InvalidArgument)
    private given OFormat[UnknownFailure.type] = singletonOFormat(UnknownFailure)
    private given OFormat[InvalidFileNameExtension.type] = singletonOFormat(InvalidFileNameExtension)
    private given OFormat[FileNameTooLong.type] = singletonOFormat(FileNameTooLong)

    private given JsonConfiguration = JsonConfiguration(
      discriminator = "type",
      typeNaming = _.split("\\.").last
    )

    given OFormat[UploadFailureReason] = Json.format
  }

  sealed trait State extends Product with Serializable

  object State {

    case object Ready extends State
    case object Uploading extends State
    final case class UploadFailed(reason: UploadFailureReason, fileName: Option[String]) extends State
    final case class Validated(downloadUrl: URL, reportingPeriod: Year, fileName: String, checksum: String, size: Long) extends State
    final case class Submitted(fileName: String, reportingPeriod: Year) extends State
    final case class Approved(fileName: String, reportingPeriod: Year) extends State
    final case class Rejected(fileName: String, reportingPeriod: Year) extends State

    private given OFormat[Ready.type] = singletonOFormat(Ready)
    private given OFormat[UploadFailed] = Json.format
    private given OFormat[Uploading.type] = singletonOFormat(Uploading)
    private given OFormat[Validated] = Json.format
    private given OFormat[Submitted] = Json.format
    private given OFormat[Approved] = Json.format
    private given OFormat[Rejected] = Json.format

    private implicit val jsonConfig: JsonConfiguration = JsonConfiguration(
      discriminator = "type",
      typeNaming = _.split("\\.").last
    )

    given OFormat[State] = Json.format
  }

  given OFormat[Submission] = Json.format

  private def singletonOFormat[A](a: A): OFormat[A] =
    OFormat(Reads.pure(a), OWrites[A](_ => Json.obj()))
}