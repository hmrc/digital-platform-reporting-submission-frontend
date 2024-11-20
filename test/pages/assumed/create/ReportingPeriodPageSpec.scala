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
import models.{CheckMode, NormalMode, UserAnswers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import queries.SubmissionsExistQuery

class ReportingPeriodPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".nextPage" - {

    val emptyAnswers = UserAnswers("userId", "operatorId")

    "in Normal mode" - {

      "must go to Assuming Operator Name when no submissions exist" in {

        val answers = emptyAnswers.set(SubmissionsExistQuery, false).success.value
        ReportingPeriodPage.nextPage(NormalMode, answers) mustEqual routes.AssumingOperatorNameController.onPageLoad(NormalMode, "operatorId")
      }

      "must go to Submissions Exist when submissions exist" in {

        val answers = emptyAnswers.set(SubmissionsExistQuery, true).success.value
        ReportingPeriodPage.nextPage(NormalMode, answers) mustEqual routes.SubmissionsExistController.onPageLoad("operatorId")
      }
    }

    "in Check mode" - {

      "must go to Check Answers" in {

        ReportingPeriodPage.nextPage(CheckMode, emptyAnswers) mustEqual routes.CheckYourAnswersController.onPageLoad("operatorId")
      }
    }
  }
}
