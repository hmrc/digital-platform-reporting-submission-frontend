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

import models.{CheckMode, Mode, NormalMode, UserAnswers}
import play.api.mvc.Call

trait AssumedReportingPage {

  final def nextPage(mode: Mode, answers: UserAnswers): Call =
    mode match {
      case NormalMode => nextPageNormalMode(answers)
      case CheckMode => nextPageCheckMode(answers)
    }
    
  protected def nextPageNormalMode(answers: UserAnswers): Call
  
  protected def nextPageCheckMode(answers: UserAnswers): Call =
    controllers.assumed.create.routes.CheckYourAnswersController.onPageLoad(answers.operatorId)
}
