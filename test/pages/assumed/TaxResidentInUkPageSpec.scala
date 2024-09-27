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

package pages.assumed

import controllers.assumed.routes
import models.{CheckMode, Country, NormalMode, UkTaxIdentifiers, UserAnswers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class TaxResidentInUkPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".nextPage" - {

    val emptyAnswers = UserAnswers("userId", "operatorId")

    "in Normal mode" - {

      "must go to UK Tax Identifiers when the answer is yes" in {

        val answers = emptyAnswers.set(TaxResidentInUkPage, true).success.value
        TaxResidentInUkPage.nextPage(NormalMode, answers) mustEqual routes.UkTaxIdentifiersController.onPageLoad(NormalMode, "operatorId")
      }

      "must go to Tax Residency Country when the answer is no" in {

        val answers = emptyAnswers.set(TaxResidentInUkPage, false).success.value
        TaxResidentInUkPage.nextPage(NormalMode, answers) mustEqual routes.TaxResidencyCountryController.onPageLoad(NormalMode, "operatorId")
      }
    }


    "in Check mode" - {

      "must go to Check Answers" - {

        "when the answer is yes and UK Tax Identifiers has been answered" in {

          val answers =
            emptyAnswers
              .set(TaxResidentInUkPage, true).success.value
              .set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value

          TaxResidentInUkPage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad("operatorId")
        }

        "when the answer is no and Tax Residency Country has been answered" in {

          val answers =
            emptyAnswers
              .set(TaxResidentInUkPage, false).success.value
              .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value

          TaxResidentInUkPage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad("operatorId")
        }
      }

      "must go to UK Tax Identifiers" - {

        "when the answer is yes and UK Tax Identifiers has not been answered" in {

          val answers = emptyAnswers.set(TaxResidentInUkPage, true).success.value
          TaxResidentInUkPage.nextPage(CheckMode, answers) mustEqual routes.UkTaxIdentifiersController.onPageLoad(CheckMode, "operatorId")
        }
      }

      "must go to Tax Residency Country" - {

        "when the answer is no and Tax Residency Country has not been answered" in {

          val answers = emptyAnswers.set(TaxResidentInUkPage, false).success.value
          TaxResidentInUkPage.nextPage(CheckMode, answers) mustEqual routes.TaxResidencyCountryController.onPageLoad(CheckMode, "operatorId")
        }
      }
    }
  }
}
