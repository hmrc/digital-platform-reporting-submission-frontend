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

import controllers.AnswerExtractor
import controllers.actions.*
import forms.RegisteredCountryFormProvider
import models.Mode
import pages.assumed.update.{AssumingOperatorNamePage, RegisteredCountryPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.assumed.update.RegisteredCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegisteredCountryController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             sessionRepository: SessionRepository,
                                             identify: IdentifierAction,
                                             getData: DataRetrievalActionProvider,
                                             requireData: DataRequiredAction,
                                             formProvider: RegisteredCountryFormProvider,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: RegisteredCountryView
                                           )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {


  def onPageLoad(operatorId: String, reportingPeriod: String): Action[AnyContent] =
    (identify andThen getData(operatorId, Some(reportingPeriod)) andThen requireData) { implicit request =>
      getAnswer(AssumingOperatorNamePage) { assumingOperatorName =>
  
        val form = formProvider(assumingOperatorName)
  
        val preparedForm = request.userAnswers.get(RegisteredCountryPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }
  
        Ok(view(preparedForm, operatorId, reportingPeriod, assumingOperatorName))
      }
    }

  def onSubmit(operatorId: String, reportingPeriod: String): Action[AnyContent] =
    (identify andThen getData(operatorId, Some(reportingPeriod)) andThen requireData).async { implicit request =>
      getAnswerAsync(AssumingOperatorNamePage) { assumingOperatorName =>
  
        val form = formProvider(assumingOperatorName)
        
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, operatorId, reportingPeriod, assumingOperatorName))),
  
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(RegisteredCountryPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(RegisteredCountryPage.nextPage(reportingPeriod, updatedAnswers))
        )
      }
    }
}