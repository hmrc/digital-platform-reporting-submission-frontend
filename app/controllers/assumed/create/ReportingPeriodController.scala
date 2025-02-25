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

import connectors.SubmissionConnector
import controllers.AnswerExtractor
import controllers.actions.*
import forms.ReportingPeriodFormProvider
import models.submission.{SortBy, SortOrder, SubmissionStatus, ViewSubmissionsRequest}
import models.{Mode, yearFormat}
import pages.assumed.create.ReportingPeriodPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import queries.SubmissionsExistQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.assumed.create.ReportingPeriodView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportingPeriodController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           sessionRepository: SessionRepository,
                                           identify: IdentifierAction,
                                           getData: DataRetrievalActionProvider,
                                           requireData: DataRequiredAction,
                                           assumedSubmissionSentCheck: AssumedSubmissionSentCheckAction,
                                           checkAssumedReportingAllowed: CheckAssumedReportingAllowedAction,
                                           formProvider: ReportingPeriodFormProvider,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: ReportingPeriodView,
                                           connector: SubmissionConnector
                                         )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(mode: Mode, operatorId: String): Action[AnyContent] =
    (identify andThen checkAssumedReportingAllowed andThen getData(operatorId) andThen requireData andThen assumedSubmissionSentCheck) { implicit request =>

      val form = formProvider()
  
      val preparedForm = request.userAnswers.get(ReportingPeriodPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }
  
      Ok(view(preparedForm, mode, operatorId))
    }

  def onSubmit(mode: Mode, operatorId: String): Action[AnyContent] =
    (identify andThen checkAssumedReportingAllowed andThen getData(operatorId) andThen requireData).async { implicit request =>
    
      val form = formProvider()
  
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, operatorId))),
        reportingPeriod => {
          
          val viewSubmissionsRequest = ViewSubmissionsRequest(
            assumedReporting = false,
            pageNumber = 1,
            sortBy = SortBy.SubmissionDate,
            sortOrder = SortOrder.Descending,
            reportingPeriod = Some(reportingPeriod.getValue),
            operatorId = Some(operatorId),
            statuses = SubmissionStatus.values
          )
          
          for {
            deliveredSubmissions                <- connector.listDeliveredSubmissions(viewSubmissionsRequest)
            undeliveredSubmissions              <- connector.listUndeliveredSubmissions
            matchingUndeliveredSubmissionExists = undeliveredSubmissions.exists(s => s.operatorId.contains(operatorId) && s.reportingPeriod.contains(reportingPeriod))
            submissionsExist                    = deliveredSubmissions.exists(_.deliveredSubmissionRecordCount > 0) || matchingUndeliveredSubmissionExists
            updatedAnswers                      <- Future.fromTry(request.userAnswers
                                                      .set(ReportingPeriodPage, reportingPeriod)
                                                      .flatMap(_.set(SubmissionsExistQuery, submissionsExist))
                                                    )
            _                                   <- sessionRepository.set(updatedAnswers)
          } yield Redirect(ReportingPeriodPage.nextPage(mode, updatedAnswers))
        }
      )
    }
}
