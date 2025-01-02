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

package viewmodels

import models.operator.responses.PlatformOperator
import play.api.libs.json.{Json, OFormat}

final case class PlatformOperatorSummary(
                                            operatorId: String,
                                            operatorName: String,
                                            operatorPrimaryContactName: String,
                                            operatorPrimaryContactEmail: String,
                                            hasReportingNotifications: Boolean
                                          )

object PlatformOperatorSummary {

  def apply(operator: PlatformOperator): PlatformOperatorSummary =
    PlatformOperatorSummary(
      operator.operatorId,
      operator.operatorName,
      operator.primaryContactDetails.contactName,
      operator.primaryContactDetails.emailAddress,
      operator.notifications.nonEmpty
    )

  implicit lazy val format: OFormat[PlatformOperatorSummary] = Json.format
}
