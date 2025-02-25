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

package controllers.assumed.create

import com.google.inject.Inject
import connectors.PlatformOperatorConnector
import controllers.actions.*
import forms.CheckReportingNotificationsFormProvider
import models.NormalMode
import models.confirmed.ConfirmedDetails
import pages.assumed.create.CheckReportingNotificationsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import services.ConfirmedDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.assumed.create.CheckReportingNotificationsView

import scala.concurrent.{ExecutionContext, Future}

class CheckReportingNotificationsController @Inject()(
                                                       override val messagesApi: MessagesApi,
                                                       identify: IdentifierAction,
                                                       getData: DataRetrievalActionProvider,
                                                       requireData: DataRequiredAction,
                                                       assumedSubmissionSentCheck: AssumedSubmissionSentCheckAction,
                                                       checkAssumedReportingAllowed: CheckAssumedReportingAllowedAction,
                                                       val controllerComponents: MessagesControllerComponents,
                                                       platformOperatorConnector: PlatformOperatorConnector,
                                                       formProvider: CheckReportingNotificationsFormProvider,
                                                       sessionRepository: SessionRepository,
                                                       confirmedDetailsService: ConfirmedDetailsService,
                                                       page: CheckReportingNotificationsPage,
                                                       view: CheckReportingNotificationsView
                                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen checkAssumedReportingAllowed andThen getData(operatorId) andThen requireData andThen assumedSubmissionSentCheck).async {
    implicit request =>
      platformOperatorConnector.viewPlatformOperator(operatorId).map { operator =>

        if (operator.notifications.isEmpty) {
          Redirect(routes.ReportingNotificationRequiredController.onPageLoad(operatorId))
        } else {
          val form = formProvider()

          val preparedForm = request.userAnswers.get(page) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, operator.notifications, operatorId, operator.operatorName))
        }
      }
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen checkAssumedReportingAllowed andThen getData(operatorId) andThen requireData).async { implicit request =>
    formProvider().bindFromRequest().fold(
      formWithErrors => {
        platformOperatorConnector.viewPlatformOperator(operatorId).map { operator =>
          BadRequest(view(formWithErrors, operator.notifications, operatorId, operator.operatorName))
        }
      },
      answer => (for {
        updatedAnswers <- Future.fromTry(request.userAnswers.set(page, answer))
        _ <- sessionRepository.set(updatedAnswers)
      } yield {
        if (answer) nextPage(operatorId)
        else Future.successful(page.nextPage(NormalMode, updatedAnswers))
      }).flatten.map(Redirect)
    )
  }

  private def nextPage(operatorId: String)(using HeaderCarrier): Future[Call] = confirmedDetailsService.confirmReportingNotificationsFor(operatorId).map {
    case ConfirmedDetails(true, true, true) => routes.ReportingPeriodController.onPageLoad(NormalMode, operatorId)
    case ConfirmedDetails(true, true, false) => routes.CheckContactDetailsController.onPageLoad(operatorId)
    case ConfirmedDetails(true, false, _) => routes.CheckReportingNotificationsController.onSubmit(operatorId)
    case ConfirmedDetails(false, _, _) => routes.CheckPlatformOperatorController.onPageLoad(operatorId)
  }
}
