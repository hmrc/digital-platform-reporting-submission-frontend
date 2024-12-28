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

package controllers.assumed.create

import connectors.PlatformOperatorConnector
import controllers.actions.*
import controllers.routes as baseRoutes
import forms.assumed.SelectPlatformOperatorFormProvider
import models.{NormalMode, UserAnswers}
import pages.assumed.create.SelectPlatformOperatorPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import queries.PlatformOperatorSummaryQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.PlatformOperatorSummary
import views.html.assumed.create.{SelectPlatformOperatorSingleChoiceView, SelectPlatformOperatorView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SelectPlatformOperatorController @Inject()(
                                                  override val messagesApi: MessagesApi,
                                                  identify: IdentifierAction,
                                                  checkSubmissionsAllowed: CheckSubmissionsAllowedAction,
                                                  val controllerComponents: MessagesControllerComponents,
                                                  formProvider: SelectPlatformOperatorFormProvider,
                                                  view: SelectPlatformOperatorView,
                                                  viewSingle: SelectPlatformOperatorSingleChoiceView,
                                                  connector: PlatformOperatorConnector,
                                                  sessionRepository: SessionRepository
                                                )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen checkSubmissionsAllowed).async { implicit request =>
    connector.viewPlatformOperators.map { operatorInfo =>
      val operators = operatorInfo.platformOperators.map(PlatformOperatorSummary(_))
      val form = formProvider(operators.map(_.operatorId).toSet)

      operators.size match {
        case 0 => Redirect(baseRoutes.JourneyRecoveryController.onPageLoad())
        case 1 => Ok(viewSingle(operators.head.operatorId, operators.head.operatorName))
        case _ => Ok(view(form, operators))
      }
    }
  }

  def onSubmit: Action[AnyContent] = (identify andThen checkSubmissionsAllowed).async { implicit request =>
    connector.viewPlatformOperators.flatMap { operatorInfo =>
      val operators = operatorInfo.platformOperators.map(PlatformOperatorSummary(_))
      val form = formProvider(operators.map(_.operatorId).toSet)

      form.bindFromRequest().fold(
        formWithErrors => {
          operators.size match {
            case 0 => Future.successful(Redirect(baseRoutes.JourneyRecoveryController.onPageLoad()))
            case 1 => Future.successful(BadRequest(viewSingle(operators.head.operatorId, operators.head.operatorName)))
            case _ => Future.successful(BadRequest(view(formWithErrors, operators)))
          }
        },
        operatorId =>
          operators.find(_.operatorId == operatorId).map { operator =>
            for {
              answers <- Future.fromTry(UserAnswers(request.userId, operator.operatorId).set(PlatformOperatorSummaryQuery, operator))
              _       <- sessionRepository.set(answers)
            } yield Redirect(SelectPlatformOperatorPage.nextPage(NormalMode, answers))
          }.getOrElse {
            Future.successful(Redirect(baseRoutes.JourneyRecoveryController.onPageLoad()))
          }
      )
    }
  }
}
