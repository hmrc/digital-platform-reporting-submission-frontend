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

class TaxResidencyCountryPageSpec extends AnyFreeSpec with Matchers with TryValues with OptionValues {

  ".nextPage" - {

    val reportingPeriod = Year.of(2024)
    val operatorId = "operatorId"
    val emptyAnswers = UserAnswers("id", operatorId, Some(reportingPeriod))

    "must go to Check Answers when HasInternationalTaxIdentifier has been answered" in {

      val answers = emptyAnswers.set(HasInternationalTaxIdentifierPage, true).success.value
      TaxResidencyCountryPage.nextPage(reportingPeriod, answers).mustEqual(routes.CheckYourAnswersController.onPageLoad(operatorId, reportingPeriod))
    }

    "must go to HasInternationalTaxIdentifier when it has not been answered" in {

      TaxResidencyCountryPage.nextPage(reportingPeriod, emptyAnswers).mustEqual(routes.HasInternationalTaxIdentifierController.onPageLoad(operatorId, reportingPeriod))
    }
  }
}
