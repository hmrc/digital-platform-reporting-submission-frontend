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

import connectors.{PlatformOperatorConnector, SubscriptionConnector}
import controllers.AnswerExtractor
import controllers.actions.{DataRequiredAction, DataRetrievalActionProvider, IdentifierAction}
import models.email.EmailsSentResult
import models.pageviews.assumed.remove.AssumedReportRemovedViewModel
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{AssumedReportSummariesQuery, SentDeleteAssumedReportingEmailsQuery}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.assumed.remove.AssumedReportRemovedSummaryList
import views.html.assumed.remove.AssumedReportRemovedView

import java.time.{Clock, Year}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AssumedReportRemovedController @Inject()(override val messagesApi: MessagesApi,
                                               val controllerComponents: MessagesControllerComponents,
                                               identify: IdentifierAction,
                                               getData: DataRetrievalActionProvider,
                                               requireData: DataRequiredAction,
                                               subscriptionConnector: SubscriptionConnector,
                                               platformOperatorConnector: PlatformOperatorConnector,
                                               view: AssumedReportRemovedView,
                                               clock: Clock)(using ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String, reportingPeriod: Year): Action[AnyContent] =
    (identify andThen getData(operatorId) andThen requireData) async { implicit request =>
      subscriptionConnector.getSubscription.flatMap { subscriptionInfo =>
        platformOperatorConnector.viewPlatformOperator(operatorId).map { platformOperator =>
          getAnswer(AssumedReportSummariesQuery) { summaries =>
            summaries.find(_.reportingPeriod == reportingPeriod).map { summary =>
              val summaryList = AssumedReportRemovedSummaryList.list(summary, clock.instant())
              val emailsSentResult = request.userAnswers.get(SentDeleteAssumedReportingEmailsQuery).getOrElse(EmailsSentResult(false, None))
              Ok(view(AssumedReportRemovedViewModel(summaryList, subscriptionInfo, platformOperator, emailsSentResult)))
            }.getOrElse(NotFound)
          }
        }
      }
    }
}
