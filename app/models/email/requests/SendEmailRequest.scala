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

import models.operator.responses.PlatformOperator
import models.submission.{AssumedReportSummary, AssumedReportingSubmission}
import models.subscription.SubscriptionInfo
import play.api.libs.json.{Json, OFormat}
import utils.DateTimeFormats.EmailDateTimeFormatter
import viewmodels.PlatformOperatorSummary

import java.time.Instant

sealed trait SendEmailRequest {
  def to: List[String]

  def templateId: String

  def parameters: Map[String, String]
}

object SendEmailRequest {
  implicit val format: OFormat[SendEmailRequest] = Json.format[SendEmailRequest]
}

final case class AddAssumedReportingUser(to: List[String],
                                         templateId: String,
                                         parameters: Map[String, String]) extends SendEmailRequest

object AddAssumedReportingUser {
  private val AddAssumedReportingUserTemplateId: String = "dprs_add_assumed_reporting_user"
  implicit val format: OFormat[AddAssumedReportingUser] = Json.format[AddAssumedReportingUser]

  def apply(subscriptionInfo: SubscriptionInfo,
            assumedReportSummary: AssumedReportSummary,
            createdInstant: Instant): AddAssumedReportingUser = AddAssumedReportingUser(
    to = List(subscriptionInfo.primaryContact.email),
    templateId = AddAssumedReportingUserTemplateId,
    parameters = Map(
      "userPrimaryContactName" -> subscriptionInfo.primaryContactName,
      "checksCompletedDateTime" -> EmailDateTimeFormatter.format(createdInstant),
      "assumingPlatformOperator" -> assumedReportSummary.assumingOperatorName,
      "poBusinessName" -> assumedReportSummary.operatorName,
      "reportingPeriod" -> assumedReportSummary.reportingPeriod.toString)
  )
}

final case class AddAssumedReportingPlatformOperator(to: List[String],
                                                     templateId: String,
                                                     parameters: Map[String, String]) extends SendEmailRequest

object AddAssumedReportingPlatformOperator {
  private val AddAssumedReportingPlatformOperatorTemplateId: String = "dprs_add_assumed_reporting_platform_operator"
  implicit val format: OFormat[AddAssumedReportingPlatformOperator] = Json.format[AddAssumedReportingPlatformOperator]

  def apply(platformOperatorSummary: PlatformOperatorSummary,
            assumedReportSummary: AssumedReportSummary,
            createdInstant: Instant): AddAssumedReportingPlatformOperator = AddAssumedReportingPlatformOperator(
    to = List(platformOperatorSummary.operatorPrimaryContactEmail),
    templateId = AddAssumedReportingPlatformOperatorTemplateId,
    parameters = Map(
      "poPrimaryContactName" -> platformOperatorSummary.operatorPrimaryContactName,
      "checksCompletedDateTime" -> EmailDateTimeFormatter.format(createdInstant),
      "assumingPlatformOperator" -> assumedReportSummary.assumingOperatorName,
      "poBusinessName" -> assumedReportSummary.operatorName,
      "reportingPeriod" -> assumedReportSummary.reportingPeriod.toString
    )
  )
}

final case class UpdateAssumedReportingUser(to: List[String],
                                            templateId: String,
                                            parameters: Map[String, String]) extends SendEmailRequest

object UpdateAssumedReportingUser {
  private val UpdateAssumedReportingUserTemplateId: String = "dprs_update_assumed_reporting_user"
  implicit val format: OFormat[UpdateAssumedReportingUser] = Json.format[UpdateAssumedReportingUser]

  def apply(subscriptionInfo: SubscriptionInfo,
            assumedReportSummary: AssumedReportSummary,
            updatedInstant: Instant): UpdateAssumedReportingUser = UpdateAssumedReportingUser(
    to = List(subscriptionInfo.primaryContact.email),
    templateId = UpdateAssumedReportingUserTemplateId,
    parameters = Map(
      "userPrimaryContactName" -> subscriptionInfo.primaryContactName,
      "checksCompletedDateTime" -> EmailDateTimeFormatter.format(updatedInstant),
      "assumingPlatformOperator" -> assumedReportSummary.assumingOperatorName,
      "poBusinessName" -> assumedReportSummary.operatorName,
      "reportingPeriod" -> assumedReportSummary.reportingPeriod.toString
    )
  )
}

final case class UpdateAssumedReportingPlatformOperator(to: List[String],
                                                        templateId: String,
                                                        parameters: Map[String, String]) extends SendEmailRequest

object UpdateAssumedReportingPlatformOperator {
  private val UpdateAssumedReportingPlatformOperatorTemplateId: String = "dprs_update_assumed_reporting_platform_operator"
  implicit val format: OFormat[UpdateAssumedReportingPlatformOperator] = Json.format[UpdateAssumedReportingPlatformOperator]

  def apply(platformOperator: PlatformOperator,
            assumedReportSummary: AssumedReportSummary,
            updatedInstant: Instant): UpdateAssumedReportingPlatformOperator = UpdateAssumedReportingPlatformOperator(
    to = List(platformOperator.primaryContactDetails.emailAddress),
    templateId = UpdateAssumedReportingPlatformOperatorTemplateId,
    parameters = Map(
      "poPrimaryContactName" -> platformOperator.primaryContactDetails.contactName,
      "checksCompletedDateTime" -> EmailDateTimeFormatter.format(updatedInstant),
      "assumingPlatformOperator" -> assumedReportSummary.assumingOperatorName,
      "poBusinessName" -> assumedReportSummary.operatorName,
      "reportingPeriod" -> assumedReportSummary.reportingPeriod.toString
    )
  )
}

final case class DeleteAssumedReportingUser(to: List[String],
                                            templateId: String,
                                            parameters: Map[String, String]) extends SendEmailRequest

object DeleteAssumedReportingUser {
  private val DeleteAssumedReportingUserTemplateId: String = "dprs_delete_assumed_reporting_user"
  implicit val format: OFormat[DeleteAssumedReportingUser] = Json.format[DeleteAssumedReportingUser]

  def apply(subscriptionInfo: SubscriptionInfo,
            assumedReportingSubmission: AssumedReportingSubmission,
            deletedInstant: Instant): DeleteAssumedReportingUser = DeleteAssumedReportingUser(
    to = List(subscriptionInfo.primaryContact.email),
    templateId = DeleteAssumedReportingUserTemplateId,
    parameters = Map(
      "userPrimaryContactName" -> subscriptionInfo.primaryContactName,
      "checksCompletedDateTime" -> EmailDateTimeFormatter.format(deletedInstant),
      "assumingPlatformOperator" -> assumedReportingSubmission.assumingOperator.name,
      "poBusinessName" -> assumedReportingSubmission.operatorName,
      "reportingPeriod" -> assumedReportingSubmission.reportingPeriod.toString
    )
  )
}

final case class DeleteAssumedReportingPlatformOperator(to: List[String],
                                                        templateId: String,
                                                        parameters: Map[String, String]) extends SendEmailRequest

object DeleteAssumedReportingPlatformOperator {
  private val DeleteAssumedReportingPlatformOperatorTemplateId: String = "dprs_delete_assumed_reporting_platform_operator"
  implicit val format: OFormat[DeleteAssumedReportingPlatformOperator] = Json.format[DeleteAssumedReportingPlatformOperator]

  def apply(platformOperator: PlatformOperator,
            assumedReportingSubmission: AssumedReportingSubmission,
            deletedInstant: Instant): DeleteAssumedReportingPlatformOperator = DeleteAssumedReportingPlatformOperator(
    to = List(platformOperator.primaryContactDetails.emailAddress),
    templateId = DeleteAssumedReportingPlatformOperatorTemplateId,
    parameters = Map(
      "poPrimaryContactName" -> platformOperator.primaryContactDetails.contactName,
      "checksCompletedDateTime" -> EmailDateTimeFormatter.format(deletedInstant),
      "assumingPlatformOperator" -> assumedReportingSubmission.assumingOperator.name,
      "poBusinessName" -> assumedReportingSubmission.operatorName,
      "reportingPeriod" -> assumedReportingSubmission.reportingPeriod.toString
    )
  )
}
