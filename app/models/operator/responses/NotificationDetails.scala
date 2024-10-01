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

package models.operator.responses


import models.DueDiligence
import models.DueDiligence.*
import models.operator.NotificationType
import models.operator.NotificationType.{Epo, Rpo}
import play.api.libs.json.{Json, OFormat}

import java.time.Instant

final case class NotificationDetails(notificationType: NotificationType,
                                     isActiveSeller: Option[Boolean],
                                     isDueDiligence: Option[Boolean],
                                     firstPeriod: Int,
                                     receivedDateTime: Instant) {

  lazy val dueDiligence: Seq[DueDiligence] = notificationType match {
    case Epo => Nil
    case Rpo =>
      val selectedOptions = Seq(
        if (isDueDiligence.contains(true)) Some(Extended) else None,
        if (isActiveSeller.contains(true)) Some(ActiveSeller) else None
      ).flatten

      if (selectedOptions.nonEmpty) selectedOptions else Seq(NoDueDiligence)
  }
}

object NotificationDetails {

  implicit lazy val defaultFormat: OFormat[NotificationDetails] = Json.format
}
