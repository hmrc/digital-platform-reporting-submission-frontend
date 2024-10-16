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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class AssumingOperatorNamePageSpec extends AnyFreeSpec with Matchers {

  ".nextPage" - {

    val emptyAnswers = UserAnswers("userId", "operatorId")

    "in Normal mode" - {

      "must go to Has Tax Identifier" in {

        AssumingOperatorNamePage.nextPage(NormalMode, emptyAnswers) mustEqual routes.TaxResidentInUkController.onPageLoad(NormalMode, "operatorId")
      }
    }

    "in Check mode" - {

      "must go to Check Answers" in {

        AssumingOperatorNamePage.nextPage(CheckMode, emptyAnswers) mustEqual routes.CheckYourAnswersController.onPageLoad("operatorId")
      }
    }
  }
}
