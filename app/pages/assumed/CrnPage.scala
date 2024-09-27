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
import controllers.routes as baseRoutes
import models.UkTaxIdentifiers.*
import models.{CheckMode, NormalMode, UserAnswers}
import play.api.libs.json.JsPath
import play.api.mvc.Call

case object CrnPage extends AssumedReportingQuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "crn"

  override protected def nextPageNormalMode(answers: UserAnswers): Call =
    answers.get(UkTaxIdentifiersPage).map {
      case x if x.contains(Vrn) => routes.VrnController.onPageLoad(NormalMode, answers.operatorId)
      case x if x.contains(Empref) => routes.EmprefController.onPageLoad(NormalMode, answers.operatorId)
      case x if x.contains(Chrn) => routes.ChrnController.onPageLoad(NormalMode, answers.operatorId)
      case _ => routes.RegisteredInUkController.onPageLoad(NormalMode, answers.operatorId)
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())

  override protected def nextPageCheckMode(answers: UserAnswers): Call =
    answers.get(UkTaxIdentifiersPage).map { identifiers =>
      if (identifiers.contains(Vrn) && answers.get(VrnPage).isEmpty) {
        routes.VrnController.onPageLoad(CheckMode, answers.operatorId)
      } else if (identifiers.contains(Empref) && answers.get(EmprefPage).isEmpty) {
        routes.EmprefController.onPageLoad(CheckMode, answers.operatorId)
      } else if (identifiers.contains(Chrn) && answers.get(ChrnPage).isEmpty) {
        routes.ChrnController.onPageLoad(CheckMode, answers.operatorId)
      } else {
        routes.CheckYourAnswersController.onPageLoad(answers.operatorId)
      }
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())
}
