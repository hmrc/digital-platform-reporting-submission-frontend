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
import models.{CheckMode, Country, InternationalAddress, NormalMode, UkAddress, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}

class RegisteredInUkPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private val emptyAnswers = UserAnswers("id", "operatorId")

  ".nextPage" - {

    "in Normal Mode" - {

      "must go to UK Address when the answer is yes" in {

        val answers = emptyAnswers.set(RegisteredInUkPage, true).success.value
        RegisteredInUkPage.nextPage(NormalMode, answers) mustEqual routes.UkAddressController.onPageLoad(NormalMode, "operatorId")
      }

      "must go to International Address when the answer is no" in {

        val answers = emptyAnswers.set(RegisteredInUkPage, false).success.value
        RegisteredInUkPage.nextPage(NormalMode, answers) mustEqual routes.InternationalAddressController.onPageLoad(NormalMode, "operatorId")
      }
    }

    "in Check Mode" - {

      "must go to Check Answers" - {

        "when the answer is yes and UK Address has been answered" in {

          val answers =
            emptyAnswers
              .set(RegisteredInUkPage, true).success.value
              .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value

          RegisteredInUkPage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad("operatorId")
        }

        "when the answer is no and International Address has been answered" in {

          val answers =
            emptyAnswers
              .set(RegisteredInUkPage, false).success.value
              .set(InternationalAddressPage, InternationalAddress("line 1", None, "city", None, "zip", Country.internationalCountries.head)).success.value

          RegisteredInUkPage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad("operatorId")
        }
      }

      "must go to UK Address when the answer is yes and UK Address has not been answered" in {

        val answers = emptyAnswers.set(RegisteredInUkPage, true).success.value
        RegisteredInUkPage.nextPage(CheckMode, answers) mustEqual routes.UkAddressController.onPageLoad(CheckMode, "operatorId")
      }

      "must go to International Address when the answer is no and International Address has not been answered" in {

        val answers = emptyAnswers.set(RegisteredInUkPage, false).success.value
        RegisteredInUkPage.nextPage(CheckMode, answers) mustEqual routes.InternationalAddressController.onPageLoad(CheckMode, "operatorId")
      }
    }
  }

  ".cleanup" - {

    "must remove UK address when the answer is no" in {

      val answers =
        emptyAnswers
          .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
          .set(InternationalAddressPage, InternationalAddress("line 1", None, "city", None, "zip", Country.internationalCountries.head)).success.value

      val result = answers.set(RegisteredInUkPage, false).success.value

      result.get(UkAddressPage) must not be defined
      result.get(InternationalAddressPage) mustBe defined
    }


    "must remove International address when the answer is yes" in {

      val answers =
        emptyAnswers
          .set(UkAddressPage, UkAddress("line 1", None, "town", None, "AA1 1AA", Country.ukCountries.head)).success.value
          .set(InternationalAddressPage, InternationalAddress("line 1", None, "city", None, "zip", Country.internationalCountries.head)).success.value

      val result = answers.set(RegisteredInUkPage, true).success.value

      result.get(UkAddressPage) mustBe defined
      result.get(InternationalAddressPage) must not be defined
    }
  }
}