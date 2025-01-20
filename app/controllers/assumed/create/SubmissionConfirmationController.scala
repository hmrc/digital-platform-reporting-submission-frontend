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

import com.google.inject.Inject
import connectors.{PlatformOperatorConnector, SubscriptionConnector}
import controllers.AnswerExtractor
import controllers.actions.*
import models.email.EmailsSentResult
import models.pageviews.assumed.create.SubmissionConfirmationViewModel
import models.submission.AssumedReportSummary
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{AssumedReportSummaryQuery, SentAddAssumedReportingEmailsQuery}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.assumed.create.AssumedReportCreatedSummary
import views.html.assumed.create.SubmissionConfirmationView

import java.time.{Clock, Year}
import scala.concurrent.ExecutionContext

class SubmissionConfirmationController @Inject()(override val messagesApi: MessagesApi,
                                                 identify: IdentifierAction,
                                                 getData: DataRetrievalActionProvider,
                                                 requireData: DataRequiredAction,
                                                 checkAssumedReportingAllowed: CheckAssumedReportingAllowedAction,
                                                 subscriptionConnector: SubscriptionConnector,
                                                 platformOperatorConnector: PlatformOperatorConnector,
                                                 val controllerComponents: MessagesControllerComponents,
                                                 view: SubmissionConfirmationView,
                                                 clock: Clock)
                                                (using ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String, reportingPeriod: Year): Action[AnyContent] =
    (identify andThen checkAssumedReportingAllowed andThen getData(operatorId, Some(reportingPeriod)) andThen requireData) async { implicit request =>
      subscriptionConnector.getSubscription.flatMap { subscriptionInfo =>
        platformOperatorConnector.viewPlatformOperator(operatorId).map { poDetails =>
          getAnswer(AssumedReportSummaryQuery) { assumedReport =>
            val summaryList = AssumedReportCreatedSummary.list(assumedReport, clock.instant())
            val emailsSentResult = request.userAnswers.get(SentAddAssumedReportingEmailsQuery).getOrElse(EmailsSentResult(false, None))
            Ok(view(SubmissionConfirmationViewModel(summaryList, subscriptionInfo, poDetails, emailsSentResult)))
          }
        }
      }
    }
}
