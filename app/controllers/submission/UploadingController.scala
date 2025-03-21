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
import models.submission.Submission
import models.submission.Submission.State.*
import play.api.Configuration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.submission.UploadingView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadingController @Inject()(
                                     override val messagesApi: MessagesApi,
                                     identify: IdentifierAction,
                                     getData: DataRetrievalActionProvider,
                                     requireData: DataRequiredAction,
                                     checkSubmissionsAllowed: CheckSubmissionsAllowedAction,
                                     val controllerComponents: MessagesControllerComponents,
                                     view: UploadingView,
                                     submissionConnector: SubmissionConnector,
                                     configuration: Configuration
                                   )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val refreshRate: String = configuration.get[Int]("uploading-refresh").toString

  def onPageLoad(operatorId: String, submissionId: String): Action[AnyContent] =
    (identify andThen checkSubmissionsAllowed andThen getData(operatorId) andThen requireData).async {
      implicit request =>
        submissionConnector.get(submissionId).flatMap {
          _.map { submission =>
            handleSubmission(operatorId, submission) {
              case Uploading =>
                Future.successful(Ok(view()).withHeaders("Refresh" -> refreshRate))
            }
          }.getOrElse {
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
        }
    }

  def onRedirect(operatorId: String, submissionId: String): Action[AnyContent] =
    (identify andThen checkSubmissionsAllowed andThen getData(operatorId) andThen requireData).async { implicit request =>
      submissionConnector.get(submissionId).flatMap {
        _.map { submission =>
          handleSubmission(operatorId, submission) {
            case Ready | _: UploadFailed =>
              submissionConnector.startUpload(submissionId).map { _ =>
                Redirect(routes.UploadingController.onPageLoad(operatorId, submissionId))
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
      }

      Future.successful(Redirect(redirectLocation))
    }
}
