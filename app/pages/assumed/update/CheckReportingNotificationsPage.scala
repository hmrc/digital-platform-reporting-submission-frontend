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

import config.FrontendAppConfig
import controllers.assumed.update.routes
import controllers.routes as baseRoutes
import models.UserAnswers
import play.api.libs.json.JsPath
import play.api.mvc.Call

import javax.inject.Inject

class CheckReportingNotificationsPage @Inject()(appConfig: FrontendAppConfig) extends AssumedReportingUpdateQuestionPage[Boolean] {

  override def path: JsPath = JsPath \ "checkPlatformOperator"

  override def nextPage(reportingPeriod: String, answers: UserAnswers): Call =
    answers.get(this).map {
      case true  => routes.CheckContactDetailsController.onPageLoad(answers.operatorId, reportingPeriod)
      case false => Call("GET", appConfig.addReportingNotificationUrl(answers.operatorId))
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())
}
