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
import connectors.PlatformOperatorConnector.PlatformOperatorNotFoundFailure
import controllers.AnswerExtractor
import controllers.actions.*
import models.confirmed.ConfirmedDetails
import models.{NormalMode, UserAnswers}
import pages.assumed.create.StartPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PlatformOperatorSummaryQuery
import repositories.SessionRepository
import services.ConfirmedDetailsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.PlatformOperatorSummary
import views.html.assumed.create.StartView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StartController @Inject()(override val messagesApi: MessagesApi,
                                identify: IdentifierAction,
                                getData: DataRetrievalActionProvider,
                                requireData: DataRequiredAction,
                                checkAssumedReportingAllowed: CheckAssumedReportingAllowedAction,
                                sessionRepository: SessionRepository,
                                platformOperatorConnector: PlatformOperatorConnector,
                                confirmedDetailsService: ConfirmedDetailsService,
                                val controllerComponents: MessagesControllerComponents,
                                view: StartView)
                               (implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen
    checkAssumedReportingAllowed andThen getData(operatorId)).async { implicit request =>
    platformOperatorConnector.viewPlatformOperator(operatorId).flatMap { operator =>
      val summary = PlatformOperatorSummary(operator)
      for {
        answers <- Future.fromTry(UserAnswers(request.userId, operatorId).set(PlatformOperatorSummaryQuery, summary))
        _ <- sessionRepository.set(answers)
      } yield Ok(view(operatorId, operator.operatorName))
    }.recover {
      case PlatformOperatorNotFoundFailure => Redirect(routes.SelectPlatformOperatorController.onPageLoad)
    }
  }

  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen
    checkAssumedReportingAllowed andThen getData(operatorId) andThen requireData).async { implicit request =>
    confirmedDetailsService.confirmedDetailsFor(operatorId).map {
      case ConfirmedDetails(true, true, true) => Redirect(routes.ReportingPeriodController.onPageLoad(NormalMode, operatorId))
      case ConfirmedDetails(true, true, false) => Redirect(routes.CheckContactDetailsController.onPageLoad(operatorId))
      case ConfirmedDetails(true, false, _) => Redirect(routes.CheckReportingNotificationsController.onSubmit(operatorId))
      case ConfirmedDetails(false, _, _) => Redirect(StartPage.nextPage(NormalMode, request.userAnswers))
    }
  }
}
