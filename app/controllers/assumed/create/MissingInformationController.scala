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

package controllers.assumed.create

import controllers.AnswerExtractor
import controllers.actions.*
import models.NormalMode
import models.submission.CreateAssumedReportingSubmissionRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.assumed.create.MissingInformationView

import javax.inject.Inject
import scala.concurrent.Future

class MissingInformationController @Inject()(identify: IdentifierAction,
                                             getData: DataRetrievalActionProvider,
                                             requireData: DataRequiredAction,
                                             checkAssumedReportingAllowed: CheckAssumedReportingAllowedAction,
                                             view: MissingInformationView)
                                            (implicit mmc: MessagesControllerComponents)
  extends FrontendController(mmc) with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String): Action[AnyContent] =
    (identify andThen checkAssumedReportingAllowed andThen getData(operatorId) andThen requireData) { implicit request =>
      Ok(view(operatorId))
    }

  def onSubmit(operatorId: String): Action[AnyContent] =
    (identify andThen checkAssumedReportingAllowed andThen getData(operatorId) andThen requireData).async { implicit request =>
      CreateAssumedReportingSubmissionRequest.build(request.userAnswers).fold(
        _ => Future.successful(Redirect(routes.ReportingPeriodController.onPageLoad(NormalMode, operatorId))),
        _ => Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad(operatorId)))
      )
    }
}
