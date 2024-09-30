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

import com.google.inject.Inject
import controllers.actions.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.*
import viewmodels.govuk.summarylist.*
import views.html.assumed.CheckYourAnswersView

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalActionProvider,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView
                                          ) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(operatorId) andThen requireData) {
    implicit request =>

      val list = SummaryListViewModel(
        rows = Seq(
          AssumingOperatorNameSummary.row(request.userAnswers),
          HasTaxIdentifierSummary.row(request.userAnswers),
          TaxResidentInUkSummary.row(request.userAnswers),
          UkTaxIdentifiersSummary.row(request.userAnswers),
          UtrSummary.row(request.userAnswers),
          CrnSummary.row(request.userAnswers),
          VrnSummary.row(request.userAnswers),
          EmprefSummary.row(request.userAnswers),
          ChrnSummary.row(request.userAnswers),
          TaxResidencyCountrySummary.row(request.userAnswers),
          InternationalTaxIdentifierSummary.row(request.userAnswers),
          RegisteredInUkSummary.row(request.userAnswers),
          UkAddressSummary.row(request.userAnswers),
          InternationalAddressSummary.row(request.userAnswers),
          ReportingPeriodSummary.row(request.userAnswers),
        ).flatten
      )

      Ok(view(list, operatorId))
  }
}
