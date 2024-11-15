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

package controllers.submission

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.PlatformOperatorConnector
import controllers.actions.{DataRequiredAction, DataRetrievalActionProvider, IdentifierAction}
import forms.CheckPlatformOperatorFormProvider
import models.operator.responses.PlatformOperator
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.operator.*
import viewmodels.govuk.summarylist.*
import views.html.submission.CheckPlatformOperatorView

import scala.concurrent.{ExecutionContext, Future}

class CheckPlatformOperatorController @Inject()(
                                                 override val messagesApi: MessagesApi,
                                                 identify: IdentifierAction,
                                                 getData: DataRetrievalActionProvider,
                                                 requireData: DataRequiredAction,
                                                 val controllerComponents: MessagesControllerComponents,
                                                 connector: PlatformOperatorConnector,
                                                 formProvider: CheckPlatformOperatorFormProvider,
                                                 view: CheckPlatformOperatorView,
                                                 appConfig: FrontendAppConfig
                                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(operatorId: String): Action[AnyContent] = (identify andThen getData(operatorId) andThen requireData).async {
    implicit request =>
      connector.viewPlatformOperator(operatorId).map { operator =>

        val form = formProvider()

        Ok(view(
          form,
          platformOperatorList(operator),
          primaryContactList(operator),
          secondaryContactList(operator),
          operator.operatorId,
          operator.operatorName
        ))
      }
  }
  
  def onSubmit(operatorId: String): Action[AnyContent] = (identify andThen getData(operatorId) andThen requireData).async {
    implicit request =>

      val form = formProvider()

      form.bindFromRequest().fold(
        formWithErrors => {
          connector.viewPlatformOperator(operatorId).map { operator =>
            BadRequest(view(
              formWithErrors,
              platformOperatorList(operator),
              primaryContactList(operator),
              secondaryContactList(operator),
              operator.operatorId,
              operator.operatorName
            ))
          }
        },
        answer => if(answer) {
          Future.successful(Redirect(routes.CheckReportingNotificationsController.onSubmit(operatorId)))
        } else {
          Future.successful(Redirect(appConfig.manageHomepageUrl))
        }
      )
  }
  
  private def platformOperatorList(operator: PlatformOperator)(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        OperatorIdSummary.row(operator),
        BusinessNameSummary.row(operator),
        HasTradingNameSummary.row(operator),
        TradingNameSummary.row(operator),
        HasTaxIdentifierSummary.row(operator),
        TaxResidentInUkSummary.row(operator),
        UkTaxIdentifiersSummary.row(operator),
        UtrSummary.row(operator),
        CrnSummary.row(operator),
        VrnSummary.row(operator),
        EmprefSummary.row(operator),
        ChrnSummary.row(operator),
        TaxResidencyCountrySummary.row(operator),
        InternationalTaxIdentifierSummary.row(operator),
        RegisteredInUkSummary.row(operator),
        AddressSummary.row(operator)
      ).flatten
    )

  private def primaryContactList(operator: PlatformOperator)(implicit messages: Messages): SummaryList =
    if (operator.secondaryContactDetails.isEmpty) {
      SummaryListViewModel(
        rows = Seq(
          PrimaryContactNameSummary.row(operator),
          PrimaryContactEmailSummary.row(operator),
          CanPhonePrimaryContactSummary.row(operator),
          PrimaryContactPhoneSummary.row(operator),
          HasSecondaryContactSummary.row(operator)
        ).flatten
      )
    } else {
      SummaryListViewModel(
        rows = Seq(
          PrimaryContactNameSummary.row(operator),
          PrimaryContactEmailSummary.row(operator),
          CanPhonePrimaryContactSummary.row(operator),
          PrimaryContactPhoneSummary.row(operator)
        ).flatten
      )
    }

  private def secondaryContactList(operator: PlatformOperator)(implicit messages: Messages): Option[SummaryList] =
    if (operator.secondaryContactDetails.isDefined) {
      Some(SummaryListViewModel(
        rows = Seq(
          HasSecondaryContactSummary.row(operator),
          SecondaryContactNameSummary.row(operator),
          SecondaryContactEmailSummary.row(operator),
          CanPhoneSecondaryContactSummary.row(operator),
          SecondaryContactPhoneSummary.row(operator),
        ).flatten
      ))
    } else {
      None
    }
}
