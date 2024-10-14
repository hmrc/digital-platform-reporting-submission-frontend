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

import controllers.assumed.create.routes
import controllers.routes as baseRoutes
import models.UserAnswers
import play.api.mvc.Call
import queries.PlatformOperatorSummaryQuery

case object SelectPlatformOperatorPage extends AssumedReportingPage {

  override protected def nextPageNormalMode(answers: UserAnswers): Call =
    answers.get(PlatformOperatorSummaryQuery).map {
      case operator if operator.hasReportingNotifications => routes.StartController.onPageLoad(operator.operatorId)
      case operator                                       => routes.ReportingNotificationRequiredController.onPageLoad(operator.operatorId)
    }.getOrElse(baseRoutes.JourneyRecoveryController.onPageLoad())
}
