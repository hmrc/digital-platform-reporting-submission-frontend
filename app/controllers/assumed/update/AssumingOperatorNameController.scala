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
import forms.AssumingOperatorNameFormProvider
import pages.assumed.update.AssumingOperatorNamePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.PlatformOperatorSummaryQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.assumed.update.AssumingOperatorNameView

import java.time.Year
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AssumingOperatorNameController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                sessionRepository: SessionRepository,
                                                identify: IdentifierAction,
                                                getData: DataRetrievalActionProvider,
                                                requireData: DataRequiredAction,
                                                formProvider: AssumingOperatorNameFormProvider,
                                                val controllerComponents: MessagesControllerComponents,
                                                view: AssumingOperatorNameView
                                              )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  def onPageLoad(operatorId: String, reportingPeriod: Year): Action[AnyContent] =
    (identify andThen getData(operatorId, Some(reportingPeriod)) andThen requireData) { implicit request =>
      getAnswer(PlatformOperatorSummaryQuery) { operator =>
  
        val form = formProvider(operator.operatorName)
  
        val preparedForm = request.userAnswers.get(AssumingOperatorNamePage) match {
          case None => form
          case Some(value) => form.fill(value)
        }
  
        Ok(view(preparedForm, operator, reportingPeriod))
      }
    }

  def onSubmit(operatorId: String, reportingPeriod: Year): Action[AnyContent] =
    (identify andThen getData(operatorId, Some(reportingPeriod)) andThen requireData).async { implicit request =>
      getAnswerAsync(PlatformOperatorSummaryQuery) { operator =>
  
        val form = formProvider(operator.operatorName)
  
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, operator, reportingPeriod))),
  
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(AssumingOperatorNamePage, value))
              _ <- sessionRepository.set(updatedAnswers)
            } yield Redirect(AssumingOperatorNamePage.nextPage(reportingPeriod, updatedAnswers))
        )
      }
    }
}
