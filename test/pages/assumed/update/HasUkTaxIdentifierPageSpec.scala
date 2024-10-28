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
import models.{Country, UkTaxIdentifiers, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}

import java.time.Year

class HasUkTaxIdentifierPageSpec
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues {
    
  private val reportingPeriod = Year.of(2024)
  private val operatorId = "operatorId"
  private val emptyAnswers = UserAnswers("id", operatorId, Some(reportingPeriod))

  ".nextPage" - {

    "must go to Check Answers" - {

      "when the answer is yes and UK Tax Identifier has been answered" in {

        val answers =
          emptyAnswers
            .set(HasUkTaxIdentifierPage, true).success.value
            .set(UkTaxIdentifierPage, "tin").success.value

        HasUkTaxIdentifierPage.nextPage(reportingPeriod, answers).mustEqual(routes.CheckYourAnswersController.onPageLoad(operatorId, reportingPeriod))
      }
      
      "when the answer is no" in {

        val answers = emptyAnswers.set(HasUkTaxIdentifierPage, false).success.value
        HasUkTaxIdentifierPage.nextPage(reportingPeriod, answers).mustEqual(routes.CheckYourAnswersController.onPageLoad(operatorId, reportingPeriod))
      }
    }
    
    "must go to UK Tax Identifier when the answer is yes and UK Tax Identifier has not been answered" in {

      val answers = emptyAnswers.set(HasUkTaxIdentifierPage, true).success.value
      HasUkTaxIdentifierPage.nextPage(reportingPeriod, answers).mustEqual(routes.UkTaxIdentifierController.onPageLoad(operatorId, reportingPeriod))
    }
  }

  ".cleanup" - {

    "must remove UK tax identifier when the answer is no" in {

      val answers =
        emptyAnswers
          .set(UkTaxIdentifierPage, "tin").success.value
          .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value
          .set(HasInternationalTaxIdentifierPage, true).success.value
          .set(InternationalTaxIdentifierPage, "foo").success.value

      val result = answers.set(HasUkTaxIdentifierPage, false).success.value

      result.get(UkTaxIdentifierPage)               must not be defined
      result.get(TaxResidencyCountryPage)           mustBe defined
      result.get(HasInternationalTaxIdentifierPage) mustBe defined
      result.get(InternationalTaxIdentifierPage)    mustBe defined
    }

    "must not remove UK tax identifier when the answer is yes" in {

      val answers =
        emptyAnswers
          .set(UkTaxIdentifierPage, "tin").success.value
          .set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value
          .set(HasInternationalTaxIdentifierPage, true).success.value
          .set(InternationalTaxIdentifierPage, "foo").success.value

      val result = answers.set(HasUkTaxIdentifierPage, true).success.value

      result.get(UkTaxIdentifierPage)               mustBe defined
      result.get(TaxResidencyCountryPage)           mustBe defined
      result.get(HasInternationalTaxIdentifierPage) mustBe defined
      result.get(InternationalTaxIdentifierPage)    mustBe defined
    }
  }
}
