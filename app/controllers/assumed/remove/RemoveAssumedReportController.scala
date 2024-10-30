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

import connectors.AssumedReportingConnector
import controllers.AnswerExtractor
import controllers.actions.{DataRequiredAction, DataRetrievalActionProvider, IdentifierAction}
import controllers.assumed.routes as assumedRoutes
import forms.RemoveAssumedReportFormProvider
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.AssumedReportSummariesQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.assumed.remove.AssumedReportSummaryList
import views.html.assumed.remove.RemoveAssumedReportView

import java.time.Year
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveAssumedReportController @Inject()(override val messagesApi: MessagesApi,
                                              val controllerComponents: MessagesControllerComponents,
                                              identify: IdentifierAction,
                                              getData: DataRetrievalActionProvider,
                                              requireData: DataRequiredAction,
                                              formProvider: RemoveAssumedReportFormProvider,
                                              view: RemoveAssumedReportView,
                                              connector: AssumedReportingConnector)(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  private val form = formProvider()

  def onPageLoad(operatorId: String, reportingPeriod: Year): Action[AnyContent] = (identify andThen getData(operatorId) andThen requireData) {
    implicit request =>
      getAnswer(AssumedReportSummariesQuery) { summaries =>
        summaries.find(_.reportingPeriod == reportingPeriod).map { summary =>
          val summaryList = AssumedReportSummaryList.list(summary)

          Ok(view(form, summaryList, operatorId, reportingPeriod))
        }.getOrElse(NotFound)
      }
  }

  def onSubmit(operatorId: String, reportingPeriod: Year): Action[AnyContent] = (identify andThen getData(operatorId) andThen requireData).async {
    implicit request =>
      getAnswerAsync(AssumedReportSummariesQuery) { summaries =>
        summaries.find(_.reportingPeriod == reportingPeriod).map { summary =>
          
          form.bindFromRequest().fold(
            formWithErrors => {
              val summaryList = AssumedReportSummaryList.list(summary)
              Future.successful(BadRequest(view(formWithErrors, summaryList, operatorId, reportingPeriod)))
            },
            answer => 
              if (answer) {
                connector.delete(operatorId, reportingPeriod)
                  .map(_ => Redirect(routes.AssumedReportRemovedController.onPageLoad(operatorId, reportingPeriod)))
              } else {
                Future.successful(Redirect(assumedRoutes.ViewAssumedReportsController.onPageLoad()))
              }
          )
        }.getOrElse(Future.successful(NotFound))
      }
  }
}
