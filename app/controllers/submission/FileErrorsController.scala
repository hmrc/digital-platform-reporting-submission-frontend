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
import controllers.actions.*
import models.submission.*
import models.submission.CadxValidationError.{FileError, RowError}
import models.submission.Submission.State.*
import org.apache.pekko.stream.connectors.csv.scaladsl.CsvFormatting
import org.apache.pekko.stream.scaladsl.Source
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FileUtils.stripExtension
import views.html.submission.{FileErrorsNoDownloadView, FileErrorsView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileErrorsController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      identify: IdentifierAction,
                                      val controllerComponents: MessagesControllerComponents,
                                      view: FileErrorsView,
                                      viewNoDownload: FileErrorsNoDownloadView,
                                      submissionConnector: SubmissionConnector
                                    )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String, submissionId: String): Action[AnyContent] = identify.async {
    implicit request =>
      submissionConnector.get(submissionId).flatMap {
        _.map { submission =>
          handleSubmission(operatorId, submission) {
            case state: Rejected => Future.successful(Ok(view(submission.operatorId, submission._id, state.fileName)))
            case state: Submitted => handleSubmittedState(operatorId, submission, state.fileName)
          }
        }.getOrElse {
          Future.successful(NotFound)
        }
      }
  }

  def listErrors(operatorId: String, submissionId: String): Action[AnyContent] = identify.async {
    implicit request =>
      submissionConnector.get(submissionId).flatMap {
        _.map { submission =>
          handleSubmission(operatorId, submission) { case state: Rejected =>
            submissionConnector.getErrors(submissionId).map { errors =>
              val tsvSource = (Source.single(tsvHeaders) ++ errors.map(toRow))
                .via(CsvFormatting.format(delimiter = CsvFormatting.Tab))
              val fileName = state.fileName.replaceAll("\\.", "_")
              Ok.chunked(tsvSource, contentType = Some("text/tsv"))
                .withHeaders("Content-Disposition" -> s"""attachment; filename="$fileName-errors.tsv"""")
            }
          }
        }.getOrElse {
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
      }
  }

  private def toRow(error: CadxValidationError): Seq[String] =
    error match {
      case fileError: FileError =>
        Seq(fileError.code, fileError.detail.getOrElse(""), "")
      case rowError: RowError =>
        Seq(rowError.code, rowError.detail.getOrElse(""), rowError.docRef)
    }

  private def tsvHeaders(using Messages): Seq[String] =
    Seq(
      Messages("fileErrors.tsv.code"),
      Messages("fileErrors.tsv.detail"),
      Messages("fileErrors.tsv.docRef")
    )

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
      }

      Future.successful(Redirect(redirectLocation))
    }

  private def handleSubmittedState(operatorId: String, submission: Submission, fileName: String)
                                  (implicit request: Request[?]): Future[Result] = {
    val viewRequest = ViewSubmissionsRequest(
      assumedReporting = false,
      pageNumber = 1,
      sortBy = SortBy.SubmissionDate,
      sortOrder = SortOrder.Descending,
      reportingPeriod = None,
      operatorId = None,
      statuses = Nil,
      fileName = Some(stripExtension(fileName))
    )

    submissionConnector.listDeliveredSubmissions(viewRequest).flatMap {
      _.map {
        _.deliveredSubmissions.headOption.map { submissionSummary =>
          submissionSummary.submissionStatus match {
            case SubmissionStatus.Rejected =>
              Future.successful(Ok(viewNoDownload(operatorId, submission._id, fileName)))

            case SubmissionStatus.Success =>
              Future.successful(Redirect(routes.SubmissionConfirmationController.onPageLoad(operatorId, submission._id)))

            case SubmissionStatus.Pending =>
              Future.successful(Redirect(routes.CheckFileController.onPageLoad(operatorId, submission._id)))
          }
        }.getOrElse(Future.successful(Redirect(routes.CheckFileController.onPageLoad(operatorId, submission._id))))
      }.getOrElse(Future.successful(Redirect(routes.CheckFileController.onPageLoad(operatorId, submission._id))))
    }
  }
}
