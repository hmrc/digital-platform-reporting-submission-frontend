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

import controllers.AnswerExtractor
import controllers.actions.*
import forms.ReportingPeriodFormProvider
import models.Mode
import pages.assumed.create.ReportingPeriodPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
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
                                        formProvider: ReportingPeriodFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: ReportingPeriodView
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(mode: Mode, operatorId: String): Action[AnyContent] = (identify andThen getData(operatorId) andThen requireData) { implicit request =>

    val form = formProvider()

    val preparedForm = request.userAnswers.get(ReportingPeriodPage) match {
      case None => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, mode, operatorId))
  }

  def onSubmit(mode: Mode, operatorId: String): Action[AnyContent] = (identify andThen getData(operatorId) andThen requireData).async { implicit request =>
  
    val form = formProvider()

    form.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(formWithErrors, mode, operatorId))),

      value =>
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(ReportingPeriodPage, value))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(ReportingPeriodPage.nextPage(mode, updatedAnswers))
    )
  }
}
