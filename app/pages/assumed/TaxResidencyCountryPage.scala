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
import models.{CheckMode, Country, NormalMode, UserAnswers}
import play.api.libs.json.JsPath
import play.api.mvc.Call

case object TaxResidencyCountryPage extends AssumedReportingQuestionPage[Country] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "taxResidencyCountry"

  override protected def nextPageNormalMode(answers: UserAnswers): Call =
    routes.InternationalTaxIdentifierController.onPageLoad(NormalMode, answers.operatorId)

  override protected def nextPageCheckMode(answers: UserAnswers): Call =
    if (answers.get(InternationalTaxIdentifierPage).isDefined) {
      routes.CheckYourAnswersController.onPageLoad(answers.operatorId)
    } else {
      routes.InternationalTaxIdentifierController.onPageLoad(CheckMode, answers.operatorId)
    }
}
