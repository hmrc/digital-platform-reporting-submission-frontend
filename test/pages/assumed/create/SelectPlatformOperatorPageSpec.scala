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

package pages.assumed.create

import controllers.assumed.create.routes
import models.{NormalMode, UserAnswers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import queries.PlatformOperatorSummaryQuery
import viewmodels.PlatformOperatorSummary

class SelectPlatformOperatorPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".nextPage" - {
    
    val emptyAnswers = UserAnswers("userId", "operatorId")

    "must go to Start when there are notifications for this operator" in {

      val answers = emptyAnswers.set(PlatformOperatorSummaryQuery, PlatformOperatorSummary("operatorId", "operatorName", "primaryContactName", "test@test.com", hasReportingNotifications = true)).success.value
      SelectPlatformOperatorPage.nextPage(NormalMode, answers) mustEqual routes.StartController.onPageLoad("operatorId")
    }

    "must go to Reporting Notification required when there are no notifications for this operator" in {

      val answers = emptyAnswers.set(PlatformOperatorSummaryQuery, PlatformOperatorSummary("operatorId", "operatorName", "primaryContactName", "test@test.com", hasReportingNotifications = false)).success.value
      SelectPlatformOperatorPage.nextPage(NormalMode, answers) mustEqual routes.ReportingNotificationRequiredController.onPageLoad("operatorId")
    }
  }
}
