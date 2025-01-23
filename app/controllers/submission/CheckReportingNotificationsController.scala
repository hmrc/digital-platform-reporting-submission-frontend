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

import config.FrontendAppConfig
import connectors.{PlatformOperatorConnector, SubmissionConnector}
import controllers.AnswerExtractor
import controllers.actions.*
import forms.CheckReportingNotificationsFormProvider
import models.confirmed.ConfirmedDetails
import pages.submission.create.CheckReportingNotificationsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PlatformOperatorSummaryQuery
import repositories.SessionRepository
import services.ConfirmedDetailsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.submission.CheckReportingNotificationsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckReportingNotificationsController @Inject()(override val messagesApi: MessagesApi,
                                                      identify: IdentifierAction,
                                                      getData: DataRetrievalActionProvider,
                                                      requireData: DataRequiredAction,
                                                      sessionRepository: SessionRepository,
                                                      checkSubmissionsAllowed: CheckSubmissionsAllowedAction,
                                                      val controllerComponents: MessagesControllerComponents,
                                                      platformOperatorConnector: PlatformOperatorConnector,
                                                      submissionConnector: SubmissionConnector,
                                                      confirmedDetailsService: ConfirmedDetailsService,
                                                      formProvider: CheckReportingNotificationsFormProvider,
                                                      view: CheckReportingNotificationsView,
                                                      appConfig: FrontendAppConfig)
                                                     (implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String): Action[AnyContent] =
    (identify andThen checkSubmissionsAllowed andThen getData(operatorId) andThen requireData).async { implicit request =>
      platformOperatorConnector.viewPlatformOperator(operatorId).map { operator =>
        if (operator.notifications.isEmpty) {
          Redirect(routes.ReportingNotificationRequiredController.onPageLoad(operatorId))
        } else {
          val form = formProvider()

          val preparedForm = request.userAnswers.get(CheckReportingNotificationsPage) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, operator.notifications, operatorId, operator.operatorName))
        }
      }
    }

  def onSubmit(operatorId: String): Action[AnyContent] =
    (identify andThen checkSubmissionsAllowed andThen getData(operatorId) andThen requireData).async { implicit request =>
      formProvider().bindFromRequest().fold(
        formWithErrors => {
          platformOperatorConnector.viewPlatformOperator(operatorId).map { operator =>
            BadRequest(view(formWithErrors, operator.notifications, operatorId, operator.operatorName))
          }
        },
        answer => (for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(CheckReportingNotificationsPage, answer))
          _ <- sessionRepository.set(updatedAnswers)
        } yield {
          if (answer) {
            confirmedDetailsService.confirmReportingNotificationsFor(operatorId).flatMap {
              case ConfirmedDetails(true, true, true) => getAnswerAsync(PlatformOperatorSummaryQuery) { summary =>
                submissionConnector.start(operatorId, summary.operatorName, None).map { submission =>
                  Redirect(routes.UploadController.onPageLoad(operatorId, submission._id))
                }
              }
              case ConfirmedDetails(true, true, false) => Future.successful(Redirect(routes.CheckContactDetailsController.onPageLoad(operatorId)))
              case ConfirmedDetails(true, false, _) => Future.successful(Redirect(routes.CheckReportingNotificationsController.onSubmit(operatorId)))
              case ConfirmedDetails(false, _, _) => Future.successful(Redirect(routes.CheckPlatformOperatorController.onPageLoad(operatorId)))
            }
          } else {
            Future.successful(Redirect(appConfig.viewNotificationsUrl(operatorId)))
          }
        }).flatten
      )
    }
}
