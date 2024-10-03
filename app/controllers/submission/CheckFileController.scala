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
import models.submission.Submission.State.{Approved, Ready, Rejected, Submitted, UploadFailed, Uploading, Validated}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.govukfrontend.views.Aliases.{Key, SummaryList, Text, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.submission.CheckFileView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckFileController @Inject()(
                                    override val messagesApi: MessagesApi,
                                    identify: IdentifierAction,
                                    getData: DataRetrievalActionProvider,
                                    requireData: DataRequiredAction,
                                    val controllerComponents: MessagesControllerComponents,
                                    view: CheckFileView,
                                    submissionConnector: SubmissionConnector
                                  )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String, submissionId: String): Action[AnyContent] = (identify andThen getData(operatorId) andThen requireData).async {
    implicit request =>
      submissionConnector.get(submissionId).flatMap {
        _.map { submission =>
          handleSubmission(operatorId, submission) { case state: Submitted =>
            Future.successful(Ok(view(operatorId, submissionId, getSummaryList(state.fileName), submission.operatorName)))
          }
        }.getOrElse {
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
      }
  }

  private def getSummaryList(fileName: String)(using Messages): SummaryList =
    SummaryList(
      rows = Seq(
        SummaryListRow(
          key = Key(content = Text(Messages("checkFile.fileName"))),
          value = Value(content = Text(fileName)),
        )
      )
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
        case _ =>
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      Future.successful(Redirect(redirectLocation))
    }
}
