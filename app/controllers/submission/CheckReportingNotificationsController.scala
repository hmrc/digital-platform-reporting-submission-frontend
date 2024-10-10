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

package controllers.submission

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.PlatformOperatorConnector
import controllers.actions.{DataRequiredAction, DataRetrievalActionProvider, IdentifierAction}
import forms.CheckReportingNotificationsFormProvider
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.submission.CheckReportingNotificationsView

import scala.concurrent.{ExecutionContext, Future}

class CheckReportingNotificationsController @Inject()(
                                                       override val messagesApi: MessagesApi,
                                                       identify: IdentifierAction,
                                                       getData: DataRetrievalActionProvider,
                                                       requireData: DataRequiredAction,
                                                       val controllerComponents: MessagesControllerComponents,
                                                       connector: PlatformOperatorConnector,
                                                       formProvider: CheckReportingNotificationsFormProvider,
                                                       view: CheckReportingNotificationsView,
                                                       appConfig: FrontendAppConfig
                                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(operatorId) andThen requireData).async {
    implicit request =>
      connector.viewPlatformOperator(operatorId).map { operator =>

        if (operator.notifications.isEmpty) {
          Redirect(routes.ReportingNotificationRequiredController.onPageLoad(operatorId))
        } else {
          val form = formProvider()

          Ok(view(form, operator.notifications, operatorId, operator.operatorName))
        }
      }
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen getData(operatorId) andThen requireData).async {
    implicit request =>

      val form = formProvider()

      form.bindFromRequest().fold(
        formWithErrors => {
          connector.viewPlatformOperator(operatorId).map { operator =>
            BadRequest(view(formWithErrors, operator.notifications, operatorId, operator.operatorName))
          }
        },
        answer => if (answer) {
          Future.successful(Redirect(routes.CheckContactDetailsController.onPageLoad(operatorId)))
        } else {
          Future.successful(Redirect(appConfig.addReportingNotificationUrl(operatorId)))
        }
      )
  }
}