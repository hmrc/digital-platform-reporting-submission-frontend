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

import cats.implicits.*
import connectors.AssumedReportingConnector
import controllers.actions.IdentifierAction
import models.UserAnswers
import org.apache.pekko.Done
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.AssumedReportSummariesQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.assumed.ViewAssumedReportsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ViewAssumedReportsController @Inject()(override val messagesApi: MessagesApi,
                                             identify: IdentifierAction,
                                             val controllerComponents: MessagesControllerComponents,
                                             connector: AssumedReportingConnector,
                                             sessionRepository: SessionRepository,
                                             view: ViewAssumedReportsView)
                                            (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = identify.async {
    implicit request =>
      connector.list.flatMap { submissions =>

        submissions.groupBy(_.operatorId).toList
          .traverse { (operatorId, submissions) =>
            for {
              answers <- Future.fromTry(UserAnswers(request.userId, operatorId).set(AssumedReportSummariesQuery, submissions))
              _       <- sessionRepository.set(answers)
            } yield Done
          }
          .map(_ => Ok(view(submissions)))
      }
  }
}
