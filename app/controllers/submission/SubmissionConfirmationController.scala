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
import connectors.{PlatformOperatorConnector, SubmissionConnector, SubscriptionConnector}
import controllers.actions.*
import forms.SubmissionConfirmationFormProvider
import models.submission.Submission
import models.submission.Submission.State.{Approved, Ready, Rejected, Submitted, UploadFailed, Uploading, Validated}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.govukfrontend.views.Aliases.{Key, SummaryList, Text, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeFormats
import viewmodels.PlatformOperatorSummary
import views.html.submission.SubmissionConfirmationView
import models.subscription._
import models.operator.responses.PlatformOperator
import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionConfirmationController @Inject()(
                                                  appConfig: FrontendAppConfig,
                                                  override val messagesApi: MessagesApi,
                                                  identify: IdentifierAction,
                                                  val controllerComponents: MessagesControllerComponents,
                                                  view: SubmissionConfirmationView,
                                                  submissionConnector: SubmissionConnector,
                                                  subscriptionConnector: SubscriptionConnector,
                                                  platformOperatorConnector: PlatformOperatorConnector,
                                                  formProvider: SubmissionConfirmationFormProvider
                                                )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

/*  def onPageLoad(operatorId: String, submissionId: String): Action[AnyContent] = identify.async {
    implicit request =>
        submissionConnector.get(submissionId).flatMap {
          _.map { submission =>
            handleSubmission(operatorId, submission) { case state: Approved =>
              Future.successful(Ok(view(formProvider(submission.operatorName), operatorId, submission.operatorName, submissionId, getSummaryList(state.fileName, submission, state, submission.updated, request.dprsId))))
            }
          }.getOrElse {
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
        }
  }  */
    def onPageLoad(operatorId: String, submissionId: String): Action[AnyContent] = identify.async {
      implicit request =>
        val contactDetails = for {
          subscriptionInfo <- subscriptionConnector.getSubscription
          operator <- platformOperatorConnector.viewPlatformOperator(operatorId)
        } yield {
          Seq(subscriptionInfo.primaryContact match {
            case individualContact: IndividualContact => individualContact.email
            case organisationContact: OrganisationContact => organisationContact.email
          },
          operator.primaryContactDetails.emailAddress).distinct
        }
        contactDetails.flatMap {
          emails =>
            submissionConnector.get(submissionId).flatMap {
              case Some(submission) =>
                handleSubmission(operatorId, submission) { case state: Approved =>
                  Future.successful(Ok(view(formProvider(submission.operatorName), operatorId, submission.operatorName, submissionId, getSummaryList(state.fileName, submission, state, submission.updated, request.dprsId),emails)))
                }
              case _ => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            }
        }
    }
  
  private def getSummaryList(fileName: String, submission: Submission, state: Approved, checksCompleted: Instant, dprsId: String)(using Messages): SummaryList =
    SummaryList(
      rows = Seq(
        SummaryListRow(
          key = Key(content = Text(Messages("submissionConfirmation.fileName"))),
          value = Value(content = Text(fileName)),
        ),
        SummaryListRow(
          key = Key(content = Text(Messages("submissionConfirmation.operatorName"))),
          value = Value(content = Text(submission.operatorName)),
        ),
        SummaryListRow(
          key = Key(content = Text(Messages("submissionConfirmation.operatorId"))),
          value = Value(content = Text(submission.operatorId)),
        ),
        SummaryListRow(
          key = Key(content = Text(Messages("submissionConfirmation.reportingPeriod"))),
          value = Value(content = Text(state.reportingPeriod.toString)),
        ),
        SummaryListRow(
          key = Key(content = Text(Messages("submissionConfirmation.checksCompleted"))),
          value = Value(content = Text(DateTimeFormats.formatInstant(checksCompleted, DateTimeFormats.fullDateTimeFormatter))),
        ),
        SummaryListRow(
          key = Key(content = Text(Messages("submissionConfirmation.dprsId"))),
          value = Value(content = Text(dprsId))
        ),
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