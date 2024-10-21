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
import models.{Country, InternationalAddress, UkAddress, UserAnswers}
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class RegisteredInUkPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  private val caseId = "caseId"
  private val operatorId = "operatorId"
  private val emptyAnswers = UserAnswers("id", operatorId, Some(caseId))
  private val ukAddress = UkAddress("line 1", None, "town", None, "postcode", Country.ukCountries.head)
  private val internationalAddress = InternationalAddress("line 1", None, "town", None, "postcode", Country.internationalCountries.head)
  
  ".nextPage" - {
    
    "must go to Check Answers" - {

      "when the answer is yes and UK Address has been answered" in {
        
        val answers =
          emptyAnswers
            .set(RegisteredInUkPage, true).success.value
            .set(UkAddressPage, ukAddress).success.value

        RegisteredInUkPage.nextPage(caseId, answers).mustEqual(routes.CheckYourAnswersController.onPageLoad(operatorId, caseId))
      }

      "when the answer is no and International Address has been answered" in {
        
        val answers =
          emptyAnswers
            .set(RegisteredInUkPage, false).success.value
            .set(InternationalAddressPage, internationalAddress).success.value

        RegisteredInUkPage.nextPage(caseId, answers).mustEqual(routes.CheckYourAnswersController.onPageLoad(operatorId, caseId))
      }
    }

    "must go to UK address when the answer is yes and UK address has not been answered" in {

      val answers = emptyAnswers.set(RegisteredInUkPage, true).success.value
      RegisteredInUkPage.nextPage(caseId, answers).mustEqual(routes.UkAddressController.onPageLoad(operatorId, caseId))
    }

    "must go to International address when the answer is no and International address has not been answered" in {

      val answers = emptyAnswers.set(RegisteredInUkPage, false).success.value
      RegisteredInUkPage.nextPage(caseId, answers).mustEqual(routes.InternationalAddressController.onPageLoad(operatorId, caseId))
    }
  }
  
  ".cleanup" - {
    
    "must remove International Address when the answer is yes" in {

      val answers =
        emptyAnswers
          .set(InternationalAddressPage, internationalAddress).success.value
          .set(UkAddressPage, ukAddress).success.value
          
      val result = answers.set(RegisteredInUkPage, true).success.value
      
      result.get(UkAddressPage) mustBe defined
      result.get(InternationalAddressPage) must not be defined
    }
    
    "must remove UK Address when the answer is no" in {

      val answers =
        emptyAnswers
          .set(InternationalAddressPage, internationalAddress).success.value
          .set(UkAddressPage, ukAddress).success.value

      val result = answers.set(RegisteredInUkPage, false).success.value

      result.get(UkAddressPage) must not be defined
      result.get(InternationalAddressPage) mustBe defined
    }
  }
}
