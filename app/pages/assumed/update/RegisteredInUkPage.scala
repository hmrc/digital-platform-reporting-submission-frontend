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
import controllers.routes as baseRoutes
import models.UserAnswers
import play.api.libs.json.JsPath
import play.api.mvc.Call

import scala.util.Try

case object RegisteredInUkPage extends AssumedReportingUpdateQuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "registeredInUk"

  override def nextPage(caseId: String, answers: UserAnswers): Call =
    answers.get(this).map {
      case true =>
        answers.get(UkAddressPage)
          .map(_ => routes.CheckYourAnswersController.onPageLoad(answers.operatorId, caseId))
          .getOrElse(routes.UkAddressController.onPageLoad(answers.operatorId, caseId))

      case false =>
        answers.get(InternationalAddressPage)
          .map(_ => routes.CheckYourAnswersController.onPageLoad(answers.operatorId, caseId))
          .getOrElse(routes.InternationalAddressController.onPageLoad(answers.operatorId, caseId))
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] =
    value.map {
      case true  => userAnswers.remove(InternationalAddressPage)
      case false => userAnswers.remove(UkAddressPage)
    }.getOrElse(super.cleanup(value, userAnswers))
}
