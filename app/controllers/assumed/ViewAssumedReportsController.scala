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
import connectors.{AssumedReportingConnector, PlatformOperatorConnector}
import controllers.actions.IdentifierAction
import controllers.routes as baseRoutes
import forms.assumed.{ViewAssumedReportsFormData, ViewAssumedReportsFormProvider}
import models.UserAnswers
import models.pageviews.assumed.ViewAssumedReportsViewModel
import models.submission.AssumedReportingSubmissionSummary
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.AssumedReportSummariesQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.assumed.ViewAssumedReportsView

import java.time.{Clock, Year}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ViewAssumedReportsController @Inject()(override val messagesApi: MessagesApi,
                                             identify: IdentifierAction,
                                             val controllerComponents: MessagesControllerComponents,
                                             assumedReportingConnector: AssumedReportingConnector,
                                             platformOperatorConnector: PlatformOperatorConnector,
                                             sessionRepository: SessionRepository,
                                             view: ViewAssumedReportsView,
                                             formProvider: ViewAssumedReportsFormProvider,
                                             clock: Clock)
                                            (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = identify.async { implicit request =>
    formProvider().bindFromRequest().fold(
      _ => Future.successful(Redirect(baseRoutes.JourneyRecoveryController.onPageLoad())),
      formData => {
        (for {
          assumedReports <- assumedReportingConnector.list
          filteredAssumedReports = filterBy(assumedReports, formData)
          operatorsResponse <- platformOperatorConnector.viewPlatformOperators
        } yield {
          assumedReports.groupBy(_.operatorId).toList.traverse { (operatorId, submissions) =>
              for {
                answers <- Future.fromTry(UserAnswers(request.userId, operatorId).set(AssumedReportSummariesQuery, submissions))
                _ <- sessionRepository.set(answers)
              } yield answers
            }
            .map(_ => {
              val viewModel = ViewAssumedReportsViewModel(
                platformOperators = operatorsResponse.platformOperators,
                assumedReportingSubmissionSummaries = filteredAssumedReports,
                currentYear = Year.now(clock),
                form = formProvider().fill(formData)
              )
              Ok(view(viewModel))
            })
        }).flatten
      }
    )
  }

  private def filterBy(assumedReports: Seq[AssumedReportingSubmissionSummary],
                       formData: ViewAssumedReportsFormData): Seq[AssumedReportingSubmissionSummary] = assumedReports.filter { assumedReport =>
    val matchesOperatorId = formData.operatorId match {
      case Some(id) => assumedReport.operatorId == id
      case None => true
    }
    val matchReportingPeriod = formData.reportingPeriod match {
      case Some(year) => assumedReport.reportingPeriod == year
      case None => true
    }

    matchesOperatorId && matchReportingPeriod
  }
}
