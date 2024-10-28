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

package pages.assumed.update

import controllers.assumed.update.routes
import models.{Country, UserAnswers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import java.time.Year

class TaxResidentInUkPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private val reportingPeriod = Year.of(2024)
  private val operatorId = "operatorId"
  private val emptyAnswers = UserAnswers("id", operatorId, Some(reportingPeriod))
  
  ".nextPage" - {

    "must go to Check Answers" - {

      "when the answer is yes and Has UK Tax Identifier has been answered" in {

        val answers =
          emptyAnswers
            .set(TaxResidentInUkPage, true).success.value
            .set(HasUkTaxIdentifierPage, true).success.value

        TaxResidentInUkPage.nextPage(reportingPeriod, answers).mustEqual(routes.CheckYourAnswersController.onPageLoad(operatorId, reportingPeriod))
      }

      "when the answer is no and Tax Residency Country has been answered" in {

        val answers =
          emptyAnswers
            .set(TaxResidentInUkPage, false).success.value
            .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value

        TaxResidentInUkPage.nextPage(reportingPeriod, answers).mustEqual(routes.CheckYourAnswersController.onPageLoad(operatorId, reportingPeriod))
      }
    }

    "must go to Has UK Tax Identifier when the answer is yes and Has UK Tax Identifier has not been answered" in {

      val answers = emptyAnswers.set(TaxResidentInUkPage, true).success.value
      TaxResidentInUkPage.nextPage(reportingPeriod, answers).mustEqual(routes.HasUkTaxIdentifierController.onPageLoad(operatorId, reportingPeriod))
    }

    "must go to Tax Residency Country when the answer is yes and Tax Residency Country has not been answered" in {

      val answers = emptyAnswers.set(TaxResidentInUkPage, false).success.value
      TaxResidentInUkPage.nextPage(reportingPeriod, answers).mustEqual(routes.TaxResidencyCountryController.onPageLoad(operatorId, reportingPeriod))
    }
  }

  ".cleanup" - {

    "must remove international tax identifier information when the answer is yes" in {

      val answers =
        emptyAnswers
          .set(HasUkTaxIdentifierPage, true).success.value
          .set(UkTaxIdentifierPage, "tin").success.value
          .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value
          .set(HasInternationalTaxIdentifierPage, true).success.value
          .set(InternationalTaxIdentifierPage, "foo").success.value

      val result = answers.set(TaxResidentInUkPage, true).success.value

      result.get(HasUkTaxIdentifierPage)            mustBe defined
      result.get(UkTaxIdentifierPage)               mustBe defined
      result.get(TaxResidencyCountryPage)           must not be defined
      result.get(HasInternationalTaxIdentifierPage) must not be defined
      result.get(InternationalTaxIdentifierPage)    must not be defined
    }

    "must remove UK tax identifier information when the answer is no" in {

      val answers =
        emptyAnswers
          .set(HasUkTaxIdentifierPage, true).success.value
          .set(UkTaxIdentifierPage, "tin").success.value
          .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value
          .set(HasInternationalTaxIdentifierPage, true).success.value
          .set(InternationalTaxIdentifierPage, "foo").success.value

      val result = answers.set(TaxResidentInUkPage, false).success.value

      result.get(HasUkTaxIdentifierPage)            must not be defined
      result.get(UkTaxIdentifierPage)               must not be defined
      result.get(TaxResidencyCountryPage)           mustBe defined
      result.get(HasInternationalTaxIdentifierPage) mustBe defined
      result.get(InternationalTaxIdentifierPage)    mustBe defined
    }
  }
}
