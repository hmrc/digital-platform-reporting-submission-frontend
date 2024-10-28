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
import models.UserAnswers
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import java.time.Year

class HasInternationalTaxIdentifierPageSpec
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues {

  private val reportingPeriod = Year.of(2024)
  private val operatorId = "operatorId"
  private val emptyAnswers = UserAnswers("id", operatorId, Some(reportingPeriod))
  
  ".nextPage" - {

    "must go to Check Answers" - {

      "when the answer is yes and International Tax Identifier has been answered" in {

        val answers =
          emptyAnswers
            .set(HasInternationalTaxIdentifierPage, true).success.value
            .set(InternationalTaxIdentifierPage, "foo").success.value

        HasInternationalTaxIdentifierPage.nextPage(reportingPeriod, answers).mustEqual(routes.CheckYourAnswersController.onPageLoad(operatorId, reportingPeriod))
      }
      
      "when the answer is no" in {

        val answers = emptyAnswers.set(HasInternationalTaxIdentifierPage, false).success.value
        HasInternationalTaxIdentifierPage.nextPage(reportingPeriod, answers).mustEqual(routes.CheckYourAnswersController.onPageLoad(operatorId, reportingPeriod))
      }
    }
    
    "must go to International Tax Identifier when the answer is yes and International Tax Identifier has not been answered" in {

      val answers = emptyAnswers.set(HasInternationalTaxIdentifierPage, true).success.value
      HasInternationalTaxIdentifierPage.nextPage(reportingPeriod, answers).mustEqual(routes.InternationalTaxIdentifierController.onPageLoad(operatorId, reportingPeriod))
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
