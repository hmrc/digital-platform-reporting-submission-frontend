/*
 * Copyright 2025 HM Revenue & Customs
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

import controllers.actions.*
import models.submission.UpdateAssumedReportingSubmissionRequest
import pages.assumed.update.AssumedReportingUpdateQuestionPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import queries.Query
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.assumed.update.MissingInformationView

import java.time.Year
import javax.inject.Inject
import scala.concurrent.Future

class MissingInformationController @Inject()(override val messagesApi: MessagesApi,
                                             identify: IdentifierAction,
                                             getData: DataRetrievalActionProvider,
                                             requireData: DataRequiredAction,
                                             checkAssumedReportingAllowed: CheckAssumedReportingAllowedAction,
                                             view: MissingInformationView)
                                            (implicit mmc: MessagesControllerComponents)
  extends FrontendController(mmc) with I18nSupport {

  def onPageLoad(operatorId: String, reportingPeriod: Year): Action[AnyContent] =
    (identify andThen checkAssumedReportingAllowed andThen getData(operatorId, Some(reportingPeriod)) andThen requireData) { implicit request =>
      Ok(view(operatorId, reportingPeriod))
    }

  def onSubmit(operatorId: String, reportingPeriod: Year): Action[AnyContent] =
    (identify andThen checkAssumedReportingAllowed andThen getData(operatorId, Some(reportingPeriod)) andThen requireData).async { implicit request =>
      UpdateAssumedReportingSubmissionRequest.build(request.userAnswers).fold(
        errors => Future.successful(Redirect(findRoute(operatorId, reportingPeriod, errors.head).url)),
        _ => Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad(operatorId, reportingPeriod)))
      )
    }

  private def findRoute(operatorId: String, reportingPeriod: Year, error: Query): Call = error match {
    case page: AssumedReportingUpdateQuestionPage[_] => page.route(operatorId, reportingPeriod)
    case _ => controllers.routes.JourneyRecoveryController.onPageLoad()
  }
}
