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

package controllers.assumed

import connectors.PlatformOperatorConnector
import connectors.PlatformOperatorConnector.PlatformOperatorNotFoundFailure
import controllers.actions.{DataRequiredAction, DataRetrievalActionProvider, IdentifierAction}
import models.{NormalMode, UserAnswers}
import pages.assumed.StartPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PlatformOperatorSummaryQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.PlatformOperatorSummary
import views.html.assumed.StartView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StartController @Inject()(
                                 override val messagesApi: MessagesApi,
                                 identify: IdentifierAction,
                                 getData: DataRetrievalActionProvider,
                                 requireData: DataRequiredAction,
                                 sessionRepository: SessionRepository,
                                 connector: PlatformOperatorConnector,
                                 val controllerComponents: MessagesControllerComponents,
                                 view: StartView
                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(operatorId)).async { implicit request =>
    request.userAnswers
      .map(_ => Future.successful(Ok(view(operatorId))))
      .getOrElse {
        connector.viewPlatformOperator(operatorId).flatMap { operator =>
          val summary = PlatformOperatorSummary(operator)

          for {
            answers  <- Future.fromTry(UserAnswers(request.userId, operatorId).set(PlatformOperatorSummaryQuery, summary))
            _        <- sessionRepository.set(answers)
          } yield Ok(view(operatorId))
        }.recover {
          case PlatformOperatorNotFoundFailure => Redirect(routes.SelectPlatformOperatorController.onPageLoad)
        }
      }
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen getData(operatorId) andThen requireData) { implicit request =>
    Redirect(StartPage.nextPage(NormalMode, request.userAnswers))
  }
}
