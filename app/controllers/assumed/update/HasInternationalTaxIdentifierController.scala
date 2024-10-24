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
import forms.HasInternationalTaxIdentifierFormProvider
import models.Mode
import pages.assumed.update.{AssumingOperatorNamePage, HasInternationalTaxIdentifierPage, TaxResidencyCountryPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.assumed.update.HasInternationalTaxIdentifierView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HasInternationalTaxIdentifierController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              sessionRepository: SessionRepository,
                                              identify: IdentifierAction,
                                              getData: DataRetrievalActionProvider,
                                              requireData: DataRequiredAction,
                                              formProvider: HasInternationalTaxIdentifierFormProvider,
                                              val controllerComponents: MessagesControllerComponents,
                                              view: HasInternationalTaxIdentifierView
                                            )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {


  def onPageLoad(operatorId: String, reportingPeriod: String): Action[AnyContent] =
    (identify andThen getData(operatorId, Some(reportingPeriod)) andThen requireData) { implicit request =>
      getAnswers(AssumingOperatorNamePage, TaxResidencyCountryPage) { case (assumingOperatorName, country) =>
  
        val form = formProvider(assumingOperatorName, country)
  
        val preparedForm = request.userAnswers.get(HasInternationalTaxIdentifierPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }
  
        Ok(view(preparedForm, operatorId, reportingPeriod, assumingOperatorName, country))
      }
    }

  def onSubmit(operatorId: String, reportingPeriod: String): Action[AnyContent] =
    (identify andThen getData(operatorId, Some(reportingPeriod)) andThen requireData).async { implicit request =>
      getAnswersAsync(AssumingOperatorNamePage, TaxResidencyCountryPage) { case (assumingOperatorName, country) =>
  
        val form = formProvider(assumingOperatorName, country)
  
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, operatorId, reportingPeriod, assumingOperatorName, country))),
  
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HasInternationalTaxIdentifierPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(HasInternationalTaxIdentifierPage.nextPage(reportingPeriod, updatedAnswers))
        )
      }
    }
}
