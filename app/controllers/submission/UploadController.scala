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
import connectors.SubmissionConnector
import controllers.actions.*
import models.submission.Submission
import models.submission.Submission.State.{Approved, Ready, Rejected, Submitted, UploadFailed, Uploading, Validated}
import models.submission.Submission.UploadFailureReason
import models.submission.Submission.UploadFailureReason.SchemaValidationError
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.UpscanService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.submission.UploadView

import scala.concurrent.duration.*
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadController @Inject()(
                                  override val messagesApi: MessagesApi,
                                  config: FrontendAppConfig,
                                  identify: IdentifierAction,
                                  getData: DataRetrievalActionProvider,
                                  requireData: DataRequiredAction,
                                  checkSubmissionsAllowed: CheckSubmissionsAllowedAction,
                                  val controllerComponents: MessagesControllerComponents,
                                  view: UploadView,
                                  submissionConnector: SubmissionConnector,
                                  upscanService: UpscanService,
                                  actorSystem: ActorSystem
                                )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String, submissionId: String): Action[AnyContent] =
    (identify andThen checkSubmissionsAllowed andThen getData(operatorId) andThen requireData).async {
      implicit request =>
        submissionConnector.get(submissionId).flatMap {
          _.map { submission =>
            handleSubmission(operatorId, submission) {
              case Ready =>
                upscanService.initiate(operatorId, request.dprsId, submissionId).map { uploadRequest =>
                  Ok(view(uploadRequest))
                }
            }
          }.getOrElse {
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
        }
    }

  def onRedirect(operatorId: String, submissionId: String): Action[AnyContent] =
    (identify andThen checkSubmissionsAllowed andThen getData(operatorId) andThen requireData).async {
      implicit request =>
        submissionConnector.get(submissionId).flatMap {
          _.map { submission =>
            handleSubmission(operatorId, submission) {
              case _: Validated =>
                submissionConnector.start(operatorId, submission.operatorName, Some(submissionId)).map { _ =>
                  Redirect(routes.UploadController.onPageLoad(operatorId, submissionId))
                }
              case state: UploadFailed if state.reason.isInstanceOf[SchemaValidationError] =>
                submissionConnector.start(operatorId, submission.operatorName, Some(submissionId)).map { _ =>
                  Redirect(routes.UploadController.onPageLoad(operatorId, submissionId))
                }
            }
          }.getOrElse {
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
        }
    }

  private def handleSubmission(operatorId: String, submission: Submission)(f: PartialFunction[Submission.State, Future[Result]]): Future[Result] = {
    pekko.pattern.after(config.upscanCallbackDelayInSeconds.seconds, actorSystem.scheduler) {
      f.lift(submission.state).getOrElse {

      val redirectLocation = submission.state match {
        case Ready | Uploading =>
          routes.UploadController.onPageLoad(operatorId, submission._id)
        case _: UploadFailed =>
          routes.UploadFailedController.onPageLoad(operatorId, submission._id)
        case _: Validated =>
          routes.SendFileController.onPageLoad(operatorId, submission._id)
        case _: Submitted =>
          routes.CheckFileController.onPageLoad(operatorId, submission._id)
        case _: Approved =>
          routes.SubmissionConfirmationController.onPageLoad(operatorId, submission._id)
        case _: Rejected =>
          routes.FileErrorsController.onPageLoad(operatorId, submission._id)
        case _ =>
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

        Future.successful(Redirect(redirectLocation))
      }
    }
  }
}
