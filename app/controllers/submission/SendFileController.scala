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
import pages.submission.create.XmlSubmissionSentPage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.Aliases.{ActionItem, Actions, SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryList}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.submission.SendFileView

import java.time.Year
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SendFileController @Inject()(
                                    override val messagesApi: MessagesApi,
                                    identify: IdentifierAction,
                                    getData: DataRetrievalActionProvider,
                                    requireData: DataRequiredAction,
                                    sessionRepository: SessionRepository,
                                    checkSubmissionsAllowed: CheckSubmissionsAllowedAction,
                                    val controllerComponents: MessagesControllerComponents,
                                    view: SendFileView,
                                    submissionConnector: SubmissionConnector
                                  )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String, submissionId: String): Action[AnyContent] =
    (identify andThen checkSubmissionsAllowed andThen getData(operatorId) andThen requireData).async {
      implicit request =>
        submissionConnector.get(submissionId).flatMap {
          _.map { submission =>
            handleSubmission(operatorId, submission) { case state: Validated =>
              Future.successful(Ok(view(operatorId, submissionId, getSummaryList(submissionId, state.fileName, state.reportingPeriod, submission))))
            }
          }.getOrElse {
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
        }
    }

  private def getSummaryList(submissionId: String, fileName: String, reportingPeriod: Year, submission: Submission)(using Messages): SummaryList =
    SummaryList(
      rows = Seq(
        SummaryListRow(
          key = Key(content = Text(Messages("sendFile.uploadedFile"))),
          value = Value(content = Text(fileName)),
          actions = Some(Actions(items = Seq(
            ActionItem(href = routes.UploadController.onRedirect(submission.operatorId, submissionId).url, content = Text(Messages("site.change")), visuallyHiddenText = Some(Messages("sendFile.uploadedFile.change")))
          )))
        ),
        SummaryListRow(
          key = Key(content = Text(Messages("sendFile.operatorName"))),
          value = Value(content = Text(submission.operatorName))
        ),
        SummaryListRow(
          key = Key(content = Text(Messages("sendFile.operatorId"))),
          value = Value(content = Text(submission.operatorId))
        ),
        SummaryListRow(
          key = Key(content = Text(Messages("sendFile.reportingPeriod"))),
          value = Value(content = Text(reportingPeriod.toString))
        )
      ),
      classes = "govuk-!-margin-bottom-8"
    )

  def onSubmit(operatorId: String, submissionId: String): Action[AnyContent] =
    (identify andThen checkSubmissionsAllowed andThen getData(operatorId) andThen requireData).async {
      implicit request =>
        submissionConnector.get(submissionId).flatMap {
          _.map { submission =>
            handleSubmission(operatorId, submission) { case _: Validated =>
              submissionConnector.submit(submissionId).flatMap { _ =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(XmlSubmissionSentPage, true))
                  _ <- sessionRepository.set(updatedAnswers)
                } yield Redirect(routes.CheckFileController.onPageLoad(operatorId, submissionId))
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
