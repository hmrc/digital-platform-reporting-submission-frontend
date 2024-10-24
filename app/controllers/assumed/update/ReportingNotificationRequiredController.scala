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

package controllers.assumed.update

import connectors.PlatformOperatorConnector
import controllers.actions.IdentifierAction
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.assumed.update.ReportingNotificationRequiredView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ReportingNotificationRequiredController @Inject()(
                                                         override val messagesApi: MessagesApi,
                                                         identify: IdentifierAction,
                                                         val controllerComponents: MessagesControllerComponents,
                                                         connector: PlatformOperatorConnector,
                                                         view: ReportingNotificationRequiredView
                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String, reportingPeriod: String): Action[AnyContent] = identify.async { implicit request =>
    connector.viewPlatformOperator(operatorId).map { operator =>
      Ok(view(operatorId, operator.operatorName))
    }
  }
}
