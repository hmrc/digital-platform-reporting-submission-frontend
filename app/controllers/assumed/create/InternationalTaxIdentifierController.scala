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
import forms.InternationalTaxIdentifierFormProvider

import javax.inject.Inject
import models.{Mode, yearFormat}
import pages.assumed.create.{InternationalTaxIdentifierPage, TaxResidencyCountryPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.assumed.create.InternationalTaxIdentifierView

import scala.concurrent.{ExecutionContext, Future}

class InternationalTaxIdentifierController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalActionProvider,
                                        requireData: DataRequiredAction,
                                        formProvider: InternationalTaxIdentifierFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: InternationalTaxIdentifierView
                                    )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(mode: Mode, operatorId: String): Action[AnyContent] = (identify andThen getData(operatorId) andThen requireData) { implicit request =>
    getAnswer(TaxResidencyCountryPage) { country =>

      val form = formProvider(country)

      val preparedForm = request.userAnswers.get(InternationalTaxIdentifierPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, operatorId, country))
    }
  }

  def onSubmit(mode: Mode, operatorId: String): Action[AnyContent] = (identify andThen getData(operatorId) andThen requireData).async { implicit request =>
    getAnswerAsync(TaxResidencyCountryPage) { country =>

      val form = formProvider(country)
      
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, operatorId, country))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(InternationalTaxIdentifierPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(InternationalTaxIdentifierPage.nextPage(mode, updatedAnswers))
      )
    }
  }
}
