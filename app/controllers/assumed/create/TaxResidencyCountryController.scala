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
import forms.TaxResidencyCountryFormProvider

import javax.inject.Inject
import models.Mode
import pages.assumed.create.{AssumingOperatorNamePage, TaxResidencyCountryPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.assumed.create.TaxResidencyCountryView

import scala.concurrent.{ExecutionContext, Future}

class TaxResidencyCountryController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalActionProvider,
                                        requireData: DataRequiredAction,
                                        assumedSubmissionSentCheck: AssumedSubmissionSentCheckAction,
                                        checkAssumedReportingAllowed: CheckAssumedReportingAllowedAction,
                                        formProvider: TaxResidencyCountryFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: TaxResidencyCountryView
                                    )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(mode: Mode, operatorId: String): Action[AnyContent] =
    (identify andThen checkAssumedReportingAllowed andThen getData(operatorId) andThen requireData andThen assumedSubmissionSentCheck) { implicit request =>
      getAnswer(AssumingOperatorNamePage) { assumingOperatorName =>
  
        val form = formProvider(assumingOperatorName)
  
        val preparedForm = request.userAnswers.get(TaxResidencyCountryPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }
  
        Ok(view(preparedForm, mode, operatorId, assumingOperatorName))
      }
    }

  def onSubmit(mode: Mode, operatorId: String): Action[AnyContent] =
    (identify andThen checkAssumedReportingAllowed andThen getData(operatorId) andThen requireData).async { implicit request =>
      getAnswerAsync(AssumingOperatorNamePage) { assumingOperatorName =>
  
        val form = formProvider(assumingOperatorName)
  
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, mode, operatorId, assumingOperatorName))),
  
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(TaxResidencyCountryPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(TaxResidencyCountryPage.nextPage(mode, updatedAnswers))
        )
      }
    }
}
