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
import forms.HasUkTaxIdentifierFormProvider
import pages.assumed.update.{AssumingOperatorNamePage, HasUkTaxIdentifierPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.assumed.update.HasUkTaxIdentifierView

import java.time.Year
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HasUkTaxIdentifierController @Inject()(override val messagesApi: MessagesApi,
                                             sessionRepository: SessionRepository,
                                             identify: IdentifierAction,
                                             getData: DataRetrievalActionProvider,
                                             requireData: DataRequiredAction,
                                             assumedSubmissionSentCheck: AssumedSubmissionSentCheckAction,
                                             checkAssumedReportingAllowed: CheckAssumedReportingAllowedAction,
                                             formProvider: HasUkTaxIdentifierFormProvider,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: HasUkTaxIdentifierView)
                                            (using ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String, reportingPeriod: Year): Action[AnyContent] =
    (identify andThen
      checkAssumedReportingAllowed andThen
      getData(operatorId, Some(reportingPeriod)) andThen
      requireData andThen assumedSubmissionSentCheck) { implicit request =>
      getAnswer(AssumingOperatorNamePage) { assumingOperatorName =>
        val preparedForm = request.userAnswers.get(HasUkTaxIdentifierPage) match {
          case None => formProvider(assumingOperatorName)
          case Some(value) => formProvider(assumingOperatorName).fill(value)
        }

        Ok(view(preparedForm, operatorId, reportingPeriod, assumingOperatorName))
      }
    }

  def onSubmit(operatorId: String, reportingPeriod: Year): Action[AnyContent] =
    (identify andThen checkAssumedReportingAllowed andThen getData(operatorId, Some(reportingPeriod)) andThen requireData).async { implicit request =>
      getAnswerAsync(AssumingOperatorNamePage) { assumingOperatorName =>
        formProvider(assumingOperatorName).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, operatorId, reportingPeriod, assumingOperatorName))),
          value => for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(HasUkTaxIdentifierPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(HasUkTaxIdentifierPage.nextPage(reportingPeriod, updatedAnswers))
        )
      }
    }
}
