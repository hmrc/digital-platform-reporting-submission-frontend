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
import connectors.{PlatformOperatorConnector, SubmissionConnector}
import controllers.actions.IdentifierAction
import controllers.routes as baseRoutes
import forms.ViewSubmissionsFormProvider
import models.submission.ViewSubmissionsRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.ViewSubmissionsViewModel
import views.html.submission.ViewSubmissionsView

import java.time.{Clock, Year}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ViewSubmissionsController @Inject()(override val messagesApi: MessagesApi,
                                          identify: IdentifierAction,
                                          val controllerComponents: MessagesControllerComponents,
                                          submissionConnector: SubmissionConnector,
                                          platformOperatorConnector: PlatformOperatorConnector,
                                          view: ViewSubmissionsView,
                                          formProvider: ViewSubmissionsFormProvider,
                                          clock: Clock,
                                          appConfig: FrontendAppConfig)
                                         (using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = identify.async { implicit request =>
    formProvider().bindFromRequest().fold(
      _ => Future.successful(Redirect(baseRoutes.JourneyRecoveryController.onPageLoad())),
      filter =>
        for {
          submissions <- submissionConnector.listDeliveredSubmissions(ViewSubmissionsRequest(filter))
          operators <- platformOperatorConnector.viewPlatformOperators
        } yield {
          val viewModel = ViewSubmissionsViewModel(submissions, operators.platformOperators, filter, Year.now(clock), appConfig.baseUrl)

          Ok(view(formProvider().fill(filter), viewModel))
        }
    )
  }
}
