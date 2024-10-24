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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class AssumingOperatorNamePageSpec extends AnyFreeSpec with Matchers {

  ".nextPage" - {
    
    val reportingPeriod = "reportingPeriod"
    val operatorId = "operatorId"
    val emptyAnswers = UserAnswers("id", operatorId, Some(reportingPeriod))
    
    "must go to Check Answers" in {
      
      AssumingOperatorNamePage.nextPage(reportingPeriod, emptyAnswers).mustEqual(routes.CheckYourAnswersController.onPageLoad(operatorId, reportingPeriod))
    }
  }
}