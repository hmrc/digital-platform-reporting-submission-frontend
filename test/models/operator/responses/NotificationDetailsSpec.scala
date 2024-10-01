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
import models.DueDiligence.{ActiveSeller, Extended, NoDueDiligence}
import models.operator.NotificationType.{Epo, Rpo}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import java.time.Instant

class NotificationDetailsSpec extends AnyFreeSpec with Matchers {

  ".dueDiligence" - {

    val instant = Instant.now()
    val reportingPeriod = 2024

    "when the notification type is EPO" - {

      "must be an empty set" in {

        val notification = NotificationDetails(Epo, Some(true), Some(false), reportingPeriod, instant)
        notification.dueDiligence mustEqual Nil
      }
    }

    "when the notification type is RPO" - {

      "must contain ActiveSeller when activeSeller is true and isDueDiligence is empty" in {

        val notification = NotificationDetails(Rpo, Some(true), None, reportingPeriod, instant)
        notification.dueDiligence mustEqual Seq(ActiveSeller)
      }

      "must contain ActiveSeller when activeSeller is true and isDueDiligence is false" in {

        val notification = NotificationDetails(Rpo, Some(true), Some(false), reportingPeriod, instant)
        notification.dueDiligence mustEqual Seq(ActiveSeller)
      }

      "must contain Extended when activeSeller is empty and isDueDiligence is true" in {

        val notification = NotificationDetails(Rpo, None, Some(true), reportingPeriod, instant)
        notification.dueDiligence mustEqual Seq(Extended)
      }

      "must contain Extended when activeSeller is false and isDueDiligence is true" in {

        val notification = NotificationDetails(Rpo, Some(false), Some(true), reportingPeriod, instant)
        notification.dueDiligence mustEqual Seq(Extended)
      }

      "must contain Extended and ActiveSeller when isDueDiligence is true and activeSeller is true" in {

        val notification = NotificationDetails(Rpo, Some(true), Some(true), reportingPeriod, instant)
        notification.dueDiligence mustEqual Seq(Extended, ActiveSeller)
      }

      "must contain NoDueDiligence when isDueDiligence and activeSeller are false" in {

        val notification = NotificationDetails(Rpo, Some(false), Some(false), reportingPeriod, instant)
        notification.dueDiligence mustEqual Seq(NoDueDiligence)
      }

      "must contain NoDueDiligence when isDueDiligence and activeSeller are empty" in {

        val notification = NotificationDetails(Rpo, None, None, reportingPeriod, instant)
        notification.dueDiligence mustEqual Seq(NoDueDiligence)
      }
    }
  }
}
