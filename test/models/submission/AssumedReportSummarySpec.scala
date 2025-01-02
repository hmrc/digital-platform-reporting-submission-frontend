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

import models.{UserAnswers, yearFormat}
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.assumed.update.AssumingOperatorNamePage
import queries.{PlatformOperatorNameQuery, PlatformOperatorSummaryQuery, ReportingPeriodQuery}
import viewmodels.PlatformOperatorSummary

import java.time.Year

class AssumedReportSummarySpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  "apply" - {
    
    val emptyAnswers = UserAnswers("id", "operatorId", None)

    "must return a summary when all required answers, including PO name, are present" in {

      val answers =
        emptyAnswers
          .set(AssumingOperatorNamePage, "assumingOperator").success.value
          .set(PlatformOperatorNameQuery, "operatorName").success.value
          .set(ReportingPeriodQuery, Year.of(2024)).success.value

      val expectedSummary = AssumedReportSummary("operatorId", "operatorName", "assumingOperator", Year.of(2024))

      AssumedReportSummary(answers).value mustEqual expectedSummary
    }

    "must return a summary when all required answers, including PO summary, are present" in {

      val answers =
        emptyAnswers
          .set(AssumingOperatorNamePage, "assumingOperator").success.value
          .set(PlatformOperatorSummaryQuery, PlatformOperatorSummary("operatorId", "operatorName", "primaryContactName", "test@test.com", hasReportingNotifications = true)).success.value
          .set(ReportingPeriodQuery, Year.of(2024)).success.value

      val expectedSummary = AssumedReportSummary("operatorId", "operatorName", "assumingOperator", Year.of(2024))

      AssumedReportSummary(answers).value mustEqual expectedSummary
    }
    
    "must return None when answers are missing" in {
      
      AssumedReportSummary(emptyAnswers) must not be defined
    }
  }
}
