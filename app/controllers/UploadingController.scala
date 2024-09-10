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
import models.submission.Submission.State.{Ready, UploadFailed}

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.UploadingView

import scala.concurrent.{ExecutionContext, Future}

class UploadingController @Inject()(
                                     override val messagesApi: MessagesApi,
                                     identify: IdentifierAction,
                                     val controllerComponents: MessagesControllerComponents,
                                     view: UploadingView,
                                     submissionConnector: SubmissionConnector
                                   )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(submissionId: String): Action[AnyContent] = identify.async {
    implicit request =>
      submissionConnector.get(submissionId).map {
        _.map { submission =>
          Ok(view())
        }.getOrElse {
          Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
      }
  }

  def onRedirect(submissionId: String): Action[AnyContent] = identify.async { implicit request =>
    submissionConnector.get(submissionId).flatMap {
      _.map { submission =>
        if (submission.state.isInstanceOf[Ready.type] || submission.state.isInstanceOf[UploadFailed]) {
          submissionConnector.startUpload(submissionId).map { _ =>
            Redirect(routes.UploadingController.onPageLoad(submissionId))
          }
        } else {
          Future.successful(Redirect(routes.UploadingController.onPageLoad(submissionId)))
        }
      }.getOrElse {
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }
  }
}
