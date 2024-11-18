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

import play.api.libs.functional.syntax.*
import play.api.libs.json.{OWrites, __}

import java.time.Instant

final case class DeleteAssumedReportEvent(
                                           dprsId: String,
                                           operatorId: String,
                                           operatorName: String,
                                           conversationId: String,
                                           statusCode: Int,
                                           processedAt: Instant
                                         ) extends AuditEvent {

  override def auditType: String = "DeleteAssumedReport"
  
  private lazy val isSuccessful: Boolean = statusCode == 200
}

object DeleteAssumedReportEvent {
  
  given OWrites[DeleteAssumedReportEvent] = (
    (__ \ "digitalPlatformReportingId").write[String] and
    (__ \ "platformOperatorId").write[String] and
    (__ \ "platformOperator").write[String] and
    (__ \ "conversationId").write[String] and
    (__ \ "outcome" \ "isSuccessful").write[Boolean] and
    (__ \ "outcome" \ "statusCode").write[Int] and
    (__ \ "outcome" \ "processedAt").write[Instant]
  )(o => (o.dprsId, o.operatorId, o.operatorName, o.conversationId, o.isSuccessful, o.statusCode, o.processedAt))
}
