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
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class TaxResidentInUkPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private val emptyAnswers = UserAnswers("userId", "operatorId")
  
  ".nextPage" - {

    "in Normal mode" - {

      "must go to Has UK Tax Identifier when the answer is yes" in {

        val answers = emptyAnswers.set(TaxResidentInUkPage, true).success.value
        TaxResidentInUkPage.nextPage(NormalMode, answers) mustEqual routes.HasUkTaxIdentifierController.onPageLoad(NormalMode, "operatorId")
      }

      "must go to Tax Residency Country when the answer is no" in {

        val answers = emptyAnswers.set(TaxResidentInUkPage, false).success.value
        TaxResidentInUkPage.nextPage(NormalMode, answers) mustEqual routes.TaxResidencyCountryController.onPageLoad(NormalMode, "operatorId")
      }
    }

    "in Check mode" - {

      "must go to Check Answers" - {

        "when the answer is yes and Has UK Tax Identifier has been answered" in {

          val answers =
            emptyAnswers
              .set(TaxResidentInUkPage, true).success.value
              .set(HasUkTaxIdentifierPage, false).success.value

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

      "must go to has UK Tax Identifier" - {

        "when the answer is yes and Has UK Tax Identifier has not been answered" in {

          val answers = emptyAnswers.set(TaxResidentInUkPage, true).success.value
          TaxResidentInUkPage.nextPage(CheckMode, answers) mustEqual routes.HasUkTaxIdentifierController.onPageLoad(CheckMode, "operatorId")
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
  
  ".cleanup" - {

    "must remove international tax identifier information when the answer is yes" in {

      val answers =
        emptyAnswers
          .set(HasUkTaxIdentifierPage, true).success.value
          .set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value
          .set(UtrPage, "utr").success.value
          .set(CrnPage, "crn").success.value
          .set(VrnPage, "vrn").success.value
          .set(EmprefPage, "empref").success.value
          .set(ChrnPage, "chrn").success.value
          .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value
          .set(HasInternationalTaxIdentifierPage, true).success.value
          .set(InternationalTaxIdentifierPage, "foo").success.value

      val result = answers.set(TaxResidentInUkPage, true).success.value

      result.get(HasUkTaxIdentifierPage)            mustBe defined
      result.get(UkTaxIdentifiersPage)              mustBe defined
      result.get(UtrPage)                           mustBe defined
      result.get(CrnPage)                           mustBe defined
      result.get(VrnPage)                           mustBe defined
      result.get(EmprefPage)                        mustBe defined
      result.get(ChrnPage)                          mustBe defined
      result.get(TaxResidencyCountryPage)           must not be defined
      result.get(HasInternationalTaxIdentifierPage) must not be defined
      result.get(InternationalTaxIdentifierPage)    must not be defined
    }

    "must remove UK tax identifier information when the answer is no" in {

      val answers =
        emptyAnswers
          .set(HasUkTaxIdentifierPage, true).success.value
          .set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value
          .set(UtrPage, "utr").success.value
          .set(CrnPage, "crn").success.value
          .set(VrnPage, "vrn").success.value
          .set(EmprefPage, "empref").success.value
          .set(ChrnPage, "chrn").success.value
          .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value
          .set(HasInternationalTaxIdentifierPage, true).success.value
          .set(InternationalTaxIdentifierPage, "foo").success.value

      val result = answers.set(TaxResidentInUkPage, false).success.value

      result.get(HasUkTaxIdentifierPage)            must not be defined
      result.get(UkTaxIdentifiersPage)              must not be defined
      result.get(UtrPage)                           must not be defined
      result.get(CrnPage)                           must not be defined
      result.get(VrnPage)                           must not be defined
      result.get(EmprefPage)                        must not be defined
      result.get(ChrnPage)                          must not be defined
      result.get(TaxResidencyCountryPage)           mustBe defined
      result.get(HasInternationalTaxIdentifierPage) mustBe defined
      result.get(InternationalTaxIdentifierPage)    mustBe defined
    }
  }
}
