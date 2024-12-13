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

import base.SpecBase
import config.FrontendAppConfig
import connectors.PlatformOperatorConnector
import forms.CheckPlatformOperatorFormProvider
import models.operator.responses.PlatformOperator
import models.operator.{AddressDetails, ContactDetails}
import models.{DefaultCountriesList, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PlatformOperatorSummaryQuery
import viewmodels.PlatformOperatorSummary
import viewmodels.checkAnswers.operator.*
import viewmodels.govuk.SummaryListFluency
import views.html.submission.CheckPlatformOperatorView

import scala.concurrent.Future

class CheckPlatformOperatorControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach {

  private val countriesList = new DefaultCountriesList
  private val form = CheckPlatformOperatorFormProvider()()
  private val mockConnector = mock[PlatformOperatorConnector]
  private val operatorSummary = PlatformOperatorSummary("operatorId", "operatorName", true)
  private val baseAnswers = emptyUserAnswers.set(PlatformOperatorSummaryQuery, operatorSummary).success.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector)
    super.beforeEach()
  }

  "Check Platform Operator Controller" - {

    "must return OK and the correct view for a GET" in {

      val operator = PlatformOperator(
        operatorId = "operatorId",
        operatorName = "operatorName",
        tinDetails = Nil,
        businessName = None,
        tradingName = None,
        primaryContactDetails = ContactDetails(None, "name", "email"),
        secondaryContactDetails = None,
        addressDetails = AddressDetails("line 1", None, None, None, None, Some("GB")),
        notifications = Nil
      )

      when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[PlatformOperatorConnector].toInstance(mockConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckPlatformOperatorController.onPageLoad(operatorId).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckPlatformOperatorView]

        implicit val msgs: Messages = messages(application)

        val operatorList = SummaryListViewModel(Seq(
          OperatorIdSummary.row(operator),
          BusinessNameSummary.row(operator),
          HasTradingNameSummary.row(operator),
          HasTaxIdentifierSummary.row(operator),
          RegisteredInUkSummary.row(operator, countriesList),
          AddressSummary.row(operator, countriesList),
        ).flatten)

        val primaryContactList = SummaryListViewModel(Seq(
          PrimaryContactNameSummary.row(operator),
          PrimaryContactEmailSummary.row(operator),
          CanPhonePrimaryContactSummary.row(operator),
          HasSecondaryContactSummary.row(operator)
        ).flatten)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, operatorList, primaryContactList, None, "operatorId", "operatorName")(request, implicitly).toString
      }
    }

    "must return BadRequest and errors when an invalid answer is submitted" in {

      val operator = PlatformOperator(
        operatorId = "operatorId",
        operatorName = "operatorName",
        tinDetails = Nil,
        businessName = None,
        tradingName = None,
        primaryContactDetails = ContactDetails(None, "name", "email"),
        secondaryContactDetails = None,
        addressDetails = AddressDetails("line 1", None, None, None, None, Some("GB")),
        notifications = Nil
      )

      when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[PlatformOperatorConnector].toInstance(mockConnector))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.CheckPlatformOperatorController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "invalid value")

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckPlatformOperatorView]

        implicit val msgs: Messages = messages(application)

        val operatorList = SummaryListViewModel(Seq(
          OperatorIdSummary.row(operator),
          BusinessNameSummary.row(operator),
          HasTradingNameSummary.row(operator),
          HasTaxIdentifierSummary.row(operator),
          RegisteredInUkSummary.row(operator, countriesList),
          AddressSummary.row(operator, countriesList),
        ).flatten)

        val primaryContactList = SummaryListViewModel(Seq(
          PrimaryContactNameSummary.row(operator),
          PrimaryContactEmailSummary.row(operator),
          CanPhonePrimaryContactSummary.row(operator),
          HasSecondaryContactSummary.row(operator)
        ).flatten)

        val formWithErrors = form.bind(Map("value" -> "invalid value"))

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(formWithErrors, operatorList, primaryContactList, None, "operatorId", "operatorName")(request, implicitly).toString
      }
    }

    "must redirect to Check Reporting Notifications when `true` is submitted" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.CheckPlatformOperatorController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckReportingNotificationsController.onPageLoad(operatorId).url
      }
    }

    "must redirect to update the platform operator when `false` is submitted" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.CheckPlatformOperatorController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "false")

        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual appConfig.manageHomepageUrl
      }
    }
  }
}
