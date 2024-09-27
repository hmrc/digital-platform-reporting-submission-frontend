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

package controllers

import connectors.SubmissionConnector
import controllers.actions.*
import models.submission.Submission
import models.submission.Submission.State.{Approved, Ready, Rejected, Submitted, UploadFailed, Uploading, Validated}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SubmissionConfirmationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionConfirmationController @Inject()(
                                                  override val messagesApi: MessagesApi,
                                                  identify: IdentifierAction,
                                                  val controllerComponents: MessagesControllerComponents,
                                                  view: SubmissionConfirmationView,
                                                  submissionConnector: SubmissionConnector
                                  )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(submissionId: String): Action[AnyContent] = identify.async {
    implicit request =>
      submissionConnector.get(submissionId).flatMap {
        _.map { submission =>
          handleSubmission(submission) { case Approved =>
            Future.successful(Ok(view()))
          }
        }.getOrElse {
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
      }
  }

  private def handleSubmission(submission: Submission)(f: PartialFunction[Submission.State, Future[Result]]): Future[Result] =
    f.lift(submission.state).getOrElse {

      val redirectLocation = submission.state match {
        case Ready =>
          routes.UploadController.onPageLoad(submission._id)
        case Uploading =>
          routes.UploadingController.onPageLoad(submission._id)
        case _: UploadFailed =>
          routes.UploadFailedController.onPageLoad(submission._id)
        case _: Validated =>
          routes.SendFileController.onPageLoad(submission._id)
        case Submitted =>
          routes.CheckFileController.onPageLoad(submission._id)
        case Approved =>
          routes.SubmissionConfirmationController.onPageLoad(submission._id)
        case Rejected =>
          routes.FileErrorsController.onPageLoad(submission._id)
        case _ =>
          routes.JourneyRecoveryController.onPageLoad()
      }

      Future.successful(Redirect(redirectLocation))
    }
}