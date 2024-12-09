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

import connectors.SubmissionConnector
import controllers.AnswerExtractor
import controllers.actions.*
import models.submission.Submission
import models.submission.Submission.State.{Approved, Ready, Rejected, Submitted, UploadFailed, Uploading, Validated}
import models.submission.Submission.UploadFailureReason
import models.submission.Submission.UploadFailureReason.SchemaValidationError
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.UpscanService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.submission.{SchemaFailureView, UploadFailedView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadFailedController @Inject()(override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: UploadFailedView,
                                       schemaFailureView: SchemaFailureView,
                                       submissionConnector: SubmissionConnector,
                                       upscanService: UpscanService)
                                      (using ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String, submissionId: String): Action[AnyContent] = identify.async {
    implicit request =>
      submissionConnector.get(submissionId).flatMap {
        _.map { submission =>
          handleSubmission(operatorId, submission) { case state: UploadFailed =>
            if (state.reason.isInstanceOf[SchemaValidationError]) {
              state.fileName.map { fileName =>
                val uploadDifferentFileUrl = routes.UploadController.onRedirect(submission.operatorId, submissionId).url
                Future.successful(Ok(schemaFailureView(uploadDifferentFileUrl, fileName)))
              }.getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))
            } else {
              upscanService.initiate(operatorId, request.dprsId, submissionId).map { uploadRequest =>
                Ok(view(uploadRequest, state.reason, submission.operatorName))
              }
            }
          }
        }.getOrElse {
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
      }
  }

  private val knownErrors: Map[String, UploadFailureReason] = Map(
    "EntityTooLarge" -> UploadFailureReason.EntityTooLarge,
    "EntityTooSmall" -> UploadFailureReason.EntityTooSmall,
    "InvalidArgument" -> UploadFailureReason.InvalidArgument
  )

  def onRedirect(operatorId: String, submissionId: String, errorCode: Option[String]): Action[AnyContent] = identify.async { implicit request =>
    submissionConnector.get(submissionId).flatMap {
      _.map { submission =>
        handleSubmission(operatorId, submission) {
          case Ready | Uploading | _: UploadFailed =>
            submissionConnector.uploadFailed(request.dprsId, submissionId, errorCode.flatMap(knownErrors.get).getOrElse(UploadFailureReason.UnknownFailure)).map { _ =>
              Redirect(routes.UploadFailedController.onPageLoad(operatorId, submissionId))
            }
        }
      }.getOrElse {
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }
  }

  private def handleSubmission(operatorId: String, submission: Submission)(f: PartialFunction[Submission.State, Future[Result]]): Future[Result] =
    f.lift(submission.state).getOrElse {

      val redirectLocation = submission.state match {
        case Ready =>
          routes.UploadController.onPageLoad(operatorId, submission._id)
        case Uploading =>
          routes.UploadingController.onPageLoad(operatorId, submission._id)
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
