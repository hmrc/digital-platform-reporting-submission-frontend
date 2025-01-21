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
import controllers.AnswerExtractor
import controllers.actions.*
import models.submission.{CadxValidationError, Submission}
import models.submission.Submission.State.{Approved, Ready, Rejected, Submitted, UploadFailed, Uploading, Validated}
import models.submission.Submission.UploadFailureReason
import models.submission.Submission.UploadFailureReason.SchemaValidationError
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko
import org.apache.pekko.stream.connectors.csv.scaladsl.CsvFormatting
import org.apache.pekko.stream.scaladsl.Source
import play.api.Configuration
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.UpscanService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.submission.{SchemaFailureView, UploadFailedView}

import scala.concurrent.duration.*
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadFailedController @Inject()(override val messagesApi: MessagesApi,
                                       config: FrontendAppConfig,
                                       identify: IdentifierAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: UploadFailedView,
                                       schemaFailureView: SchemaFailureView,
                                       submissionConnector: SubmissionConnector,
                                       upscanService: UpscanService,
                                       configuration: Configuration,
                                       actorSystem: ActorSystem
                                      )(using ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  private val maxErrors: Int = configuration.get[Int]("max-errors")

  def onPageLoad(operatorId: String, submissionId: String): Action[AnyContent] = identify.async {
    implicit request =>
      submissionConnector.get(submissionId).flatMap {
        _.map { submission =>
          handleSubmission(operatorId, submission) { case state: UploadFailed =>
            state.reason match {
              case SchemaValidationError(_, moreErrors) =>
                state.fileName.map { fileName =>
                  val uploadDifferentFileUrl = routes.UploadController.onRedirect(submission.operatorId, submissionId).url
                  Future.successful(Ok(schemaFailureView(uploadDifferentFileUrl, fileName, operatorId, submissionId, moreErrors, maxErrors)))
                }.getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))
              case _ =>
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

  def downloadSchemaErrors(operatorId: String, submissionId: String): Action[AnyContent] = identify.async { implicit request =>
    submissionConnector.get(submissionId).map {
      _.map { submission =>
        submission.state match {
          case state @ UploadFailed(SchemaValidationError(errors, _), _) =>
            val tsvSource = (Source.single(tsvHeaders) ++ Source(errors.map(toRow)))
              .via(CsvFormatting.format(delimiter = CsvFormatting.Tab))
            val fileName = state.fileName.get.replaceAll("\\.", "_")
            Ok.chunked(tsvSource, contentType = Some("text/tsv"))
              .withHeaders("Content-Disposition" -> s"""attachment; filename="$fileName-schema-errors.tsv"""")
          case _ =>
            NotFound
        }
      }.getOrElse(NotFound)
    }
  }

  private def toRow(error: SchemaValidationError.Error): Seq[String] =
    Seq(
      error.line.toString,
      error.col.toString,
      error.message
    )

  private def tsvHeaders(using Messages): Seq[String] =
    Seq(
      Messages("uploadFailed.schemafailure.tsv.line"),
      Messages("uploadFailed.schemafailure.tsv.column"),
      Messages("uploadFailed.schemafailure.tsv.message")
    )

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
