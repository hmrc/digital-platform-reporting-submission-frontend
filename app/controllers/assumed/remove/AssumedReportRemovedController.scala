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

package controllers.assumed.remove

import controllers.actions.{DataRequiredAction, DataRetrievalActionProvider, IdentifierAction}
import controllers.AnswerExtractor
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.AssumedReportSummariesQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.assumed.remove.AssumedReportRemovedSummaryList
import views.html.assumed.remove.AssumedReportRemovedView

import java.time.Clock
import javax.inject.Inject

class AssumedReportRemovedController @Inject()(override val messagesApi: MessagesApi,
                                               val controllerComponents: MessagesControllerComponents,
                                               identify: IdentifierAction,
                                               getData: DataRetrievalActionProvider,
                                               requireData: DataRequiredAction,
                                               view: AssumedReportRemovedView,
                                               clock: Clock)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String, submissionId: String): Action[AnyContent] = (identify andThen getData(operatorId) andThen requireData) {
    implicit request =>
      getAnswer(AssumedReportSummariesQuery) { summaries =>

        summaries.find(_.submissionId == submissionId).map { summary =>
          val summaryList = AssumedReportRemovedSummaryList.list(summary, clock.instant())

          Ok(view(summaryList, operatorId, submissionId))
        }.getOrElse(NotFound)
      }
  }
}
