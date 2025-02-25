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

import com.google.inject.Inject
import connectors.PlatformOperatorConnector
import controllers.actions.*
import forms.CheckReportingNotificationsFormProvider
import pages.assumed.update.CheckReportingNotificationsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.assumed.update.CheckReportingNotificationsView

import java.time.Year
import scala.concurrent.{ExecutionContext, Future}

class CheckReportingNotificationsController @Inject()(
                                                       override val messagesApi: MessagesApi,
                                                       identify: IdentifierAction,
                                                       getData: DataRetrievalActionProvider,
                                                       requireData: DataRequiredAction,
                                                       checkAssumedReportingAllowed: CheckAssumedReportingAllowedAction,
                                                       val controllerComponents: MessagesControllerComponents,
                                                       connector: PlatformOperatorConnector,
                                                       formProvider: CheckReportingNotificationsFormProvider,
                                                       sessionRepository: SessionRepository,
                                                       page: CheckReportingNotificationsPage,
                                                       view: CheckReportingNotificationsView
                                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String, reportingPeriod: Year): Action[AnyContent] =
    (identify andThen checkAssumedReportingAllowed andThen getData(operatorId, Some(reportingPeriod)) andThen requireData).async {
      implicit request =>
        connector.viewPlatformOperator(operatorId).map { operator =>

          if (operator.notifications.isEmpty) {
            Redirect(routes.ReportingNotificationRequiredController.onPageLoad(operatorId, reportingPeriod))
          } else {
            val form = formProvider()

            Ok(view(form, operator.notifications, operatorId, reportingPeriod, operator.operatorName))
          }
        }
    }

  def onSubmit(operatorId: String, reportingPeriod: Year): Action[AnyContent] =
    (identify andThen checkAssumedReportingAllowed andThen getData(operatorId, Some(reportingPeriod)) andThen requireData).async {
      implicit request =>
        formProvider().bindFromRequest().fold(
          formWithErrors => {
            connector.viewPlatformOperator(operatorId).map { operator =>
              BadRequest(view(formWithErrors, operator.notifications, operatorId, reportingPeriod, operator.operatorName))
            }
          },
          answer =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(page, answer))
              _ <- sessionRepository.set(updatedAnswers)
            } yield Redirect(page.nextPage(reportingPeriod, updatedAnswers))
        )
    }
}
