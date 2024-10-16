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
import models.{CheckMode, Country, NormalMode, UkTaxIdentifiers, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}

class HasInternationalTaxIdentifierPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private val emptyAnswers = UserAnswers("userId", "operatorId")
  
  ".nextPage" - {

    "in Normal mode" - {

      "must go to International Tax Identifier when the answer is yes" in {

        val answers = emptyAnswers.set(HasInternationalTaxIdentifierPage, true).success.value
        HasInternationalTaxIdentifierPage.nextPage(NormalMode, answers) mustEqual routes.InternationalTaxIdentifierController.onPageLoad(NormalMode, "operatorId")
      }

      "must go to Registered in UK when the answer is no" in {

        val answers = emptyAnswers.set(HasInternationalTaxIdentifierPage, false).success.value
        HasInternationalTaxIdentifierPage.nextPage(NormalMode, answers) mustEqual routes.RegisteredInUkController.onPageLoad(NormalMode, "operatorId")
      }
    }

    "in Check mode" - {

      "must go to Check Answers" - {

        "when the answer is yes and International Tax Identifier has been answered" in {

          val answers =
            emptyAnswers
              .set(HasInternationalTaxIdentifierPage, true).success.value
              .set(InternationalTaxIdentifierPage, "foo").success.value

          HasInternationalTaxIdentifierPage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad("operatorId")
        }

        "when the answer is no" in {

          val answers = emptyAnswers.set(HasInternationalTaxIdentifierPage, false).success.value
          HasInternationalTaxIdentifierPage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad("operatorId")
        }
      }

      "must go to International Tax identifier" - {

        "when the answer is yes and international tax identifier has not been answered" in {

          val answers = emptyAnswers.set(HasInternationalTaxIdentifierPage, true).success.value
          HasInternationalTaxIdentifierPage.nextPage(CheckMode, answers) mustEqual routes.InternationalTaxIdentifierController.onPageLoad(CheckMode, "operatorId")
        }
      }
    }
  }
  
  ".cleanup" - {

    "must remove international tax identifier when the answer is no" in {

      val answers = emptyAnswers.set(InternationalTaxIdentifierPage, "foo").success.value

      val result = answers.set(HasInternationalTaxIdentifierPage, false).success.value

      result.get(InternationalTaxIdentifierPage) must not be defined
    }

    "must not remove international tax identifier when the answer is yes" in {

      val answers = emptyAnswers.set(InternationalTaxIdentifierPage, "foo").success.value

      val result = answers.set(HasInternationalTaxIdentifierPage, true).success.value

      result.get(InternationalTaxIdentifierPage) mustBe defined
    }
  }
}
