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

import play.api.libs.json.{Json, OFormat}

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
  val AddAssumedReportingUserTemplateId: String = "dprs_add_assumed_reporting_user"
  implicit val format: OFormat[AddAssumedReportingUser] = Json.format[AddAssumedReportingUser]

  def apply(email: String,
            name: String,
            checksCompletedDateTime: String,
            assumingPlatformOperator: String,
            businessName: String,
            reportingPeriod: String): AddAssumedReportingUser = AddAssumedReportingUser(
    to = List(email),
    templateId = AddAssumedReportingUserTemplateId,
    parameters = Map(
      "userPrimaryContactName" -> name,
      "checksCompletedDateTime" -> checksCompletedDateTime,
      "assumingPlatformOperator" -> assumingPlatformOperator,
      "poBusinessName" -> businessName,
      "reportingPeriod" -> reportingPeriod)
  )
}

final case class AddAssumedReportingPlatformOperator(to: List[String],
                                                templateId: String,
                                                parameters: Map[String, String]) extends SendEmailRequest

object AddAssumedReportingPlatformOperator {
  val AddAssumedReportingPlatformOperatorTemplateId: String = "dprs_add_assumed_reporting_platform_operator"
  implicit val format: OFormat[AddAssumedReportingPlatformOperator] = Json.format[AddAssumedReportingPlatformOperator]

  def apply(email: String,
            platformOperatorContactName: String,
            checksCompletedDateTime: String,
            assumingPlatformOperator: String,
            businessName: String,
            reportingPeriod: String): AddAssumedReportingPlatformOperator = AddAssumedReportingPlatformOperator(
    to = List(email),
    templateId = AddAssumedReportingPlatformOperatorTemplateId,
    parameters = Map(
      "poPrimaryContactName" -> platformOperatorContactName,
      "checksCompletedDateTime" -> checksCompletedDateTime,
      "assumingPlatformOperator" -> assumingPlatformOperator,
      "poBusinessName" -> businessName,
      "reportingPeriod" -> reportingPeriod)
  )

}

final case class UpdateAssumedReportingUser(to: List[String],
                                         templateId: String,
                                         parameters: Map[String, String]) extends SendEmailRequest

object UpdateAssumedReportingUser {
  val UpdateAssumedReportingUserTemplateId: String = "dprs_update_assumed_reporting_user"
  implicit val format: OFormat[UpdateAssumedReportingUser] = Json.format[UpdateAssumedReportingUser]

  def apply(email: String,
            name: String,
            checksCompletedDateTime: String,
            assumingPlatformOperator: String,
            businessName: String,
            reportingPeriod: String): UpdateAssumedReportingUser = UpdateAssumedReportingUser(
    to = List(email),
    templateId = UpdateAssumedReportingUserTemplateId,
    parameters = Map(
      "userPrimaryContactName" -> name,
      "checksCompletedDateTime" -> checksCompletedDateTime,
      "assumingPlatformOperator" -> assumingPlatformOperator,
      "poBusinessName" -> businessName,
      "reportingPeriod" -> reportingPeriod)
  )
}

final case class UpdateAssumedReportingPlatformOperator(to: List[String],
                                                     templateId: String,
                                                     parameters: Map[String, String]) extends SendEmailRequest

object UpdateAssumedReportingPlatformOperator {
  val UpdateAssumedReportingPlatformOperatorTemplateId: String = "dprs_update_assumed_reporting_platform_operator"
  implicit val format: OFormat[UpdateAssumedReportingPlatformOperator] = Json.format[UpdateAssumedReportingPlatformOperator]

  def apply(email: String,
            platformOperatorContactName: String,
            checksCompletedDateTime: String,
            assumingPlatformOperator: String,
            businessName: String,
            reportingPeriod: String): UpdateAssumedReportingPlatformOperator = UpdateAssumedReportingPlatformOperator(
    to = List(email),
    templateId = UpdateAssumedReportingPlatformOperatorTemplateId,
    parameters = Map(
      "poPrimaryContactName" -> platformOperatorContactName,
      "checksCompletedDateTime" -> checksCompletedDateTime,
      "assumingPlatformOperator" -> assumingPlatformOperator,
      "poBusinessName" -> businessName,
      "reportingPeriod" -> reportingPeriod)
  )

}



