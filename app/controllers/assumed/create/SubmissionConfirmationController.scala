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
import controllers.AnswerExtractor
import controllers.actions.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.AssumedReportSummaryQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.assumed.create.AssumedReportCreatedSummary
import views.html.assumed.create.SubmissionConfirmationView

import java.time.{Clock, Year}

class SubmissionConfirmationController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalActionProvider,
                                            requireData: DataRequiredAction,
                                            checkAssumedReportingAllowed: CheckAssumedReportingAllowedAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: SubmissionConfirmationView,
                                            clock: Clock
                                          )
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String, reportingPeriod: Year): Action[AnyContent] =
    (identify andThen checkAssumedReportingAllowed andThen getData(operatorId, Some(reportingPeriod)) andThen requireData) { implicit request =>
      getAnswer(AssumedReportSummaryQuery) { assumedReport =>
        val summaryList = AssumedReportCreatedSummary.list(assumedReport, clock.instant())
        Ok(view(operatorId, summaryList))
      }
  }
}
