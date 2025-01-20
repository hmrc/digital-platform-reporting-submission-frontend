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

package controllers.assumed.update

import com.google.inject.Inject
import connectors.{PlatformOperatorConnector, SubscriptionConnector}
import controllers.AnswerExtractor
import controllers.actions.*
import models.email.EmailsSentResult
import models.pageviews.assumed.update.SubmissionConfirmationViewModel
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{AssumedReportSummaryQuery, SentUpdateAssumedReportingEmailsQuery}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.assumed.update.AssumedReportUpdatedSummaryList
import views.html.assumed.update.SubmissionConfirmationView

import java.time.{Clock, Year}
import scala.concurrent.ExecutionContext

class SubmissionConfirmationController @Inject()(override val messagesApi: MessagesApi,
                                                 identify: IdentifierAction,
                                                 getData: DataRetrievalActionProvider,
                                                 requireData: DataRequiredAction,
                                                 subscriptionConnector: SubscriptionConnector,
                                                 platformOperatorConnector: PlatformOperatorConnector,
                                                 val controllerComponents: MessagesControllerComponents,
                                                 view: SubmissionConfirmationView,
                                                 clock: Clock)
                                                (using ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String, reportingPeriod: Year): Action[AnyContent] =
    (identify andThen getData(operatorId, Some(reportingPeriod)) andThen requireData) async { implicit request =>
      subscriptionConnector.getSubscription.flatMap { subscriptionInfo =>
        platformOperatorConnector.viewPlatformOperator(operatorId).map { poDetails =>
          getAnswer(AssumedReportSummaryQuery) { assumedReport =>
            val summaryList = AssumedReportUpdatedSummaryList.list(assumedReport, clock.instant())
            val emailsSentResult = request.userAnswers.get(SentUpdateAssumedReportingEmailsQuery).getOrElse(EmailsSentResult(false, None))
            Ok(view(SubmissionConfirmationViewModel(summaryList, subscriptionInfo, poDetails, emailsSentResult)))
          }
        }
      }
    }
}