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
import connectors.{PlatformOperatorConnector, SubmissionConnector}
import controllers.routes as baseRoutes
import forms.CheckPlatformOperatorFormProvider
import models.DefaultCountriesList
import models.operator.responses.PlatformOperator
import models.operator.{AddressDetails, ContactDetails}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.submission.create.{CheckPlatformOperatorPage, XmlSubmissionSentPage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PlatformOperatorSummaryQuery
import repositories.SessionRepository
import services.ConfirmedDetailsService
import support.builders.ConfirmedDetailsBuilder.aConfirmedDetails
import support.builders.SubmissionBuilder.aSubmission
import support.builders.UserAnswersBuilder.aUserAnswers
import viewmodels.PlatformOperatorSummary
import viewmodels.checkAnswers.operator.*
import viewmodels.govuk.SummaryListFluency
import views.html.submission.CheckPlatformOperatorView

import scala.concurrent.Future

class CheckPlatformOperatorControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach {

  private val countriesList = new DefaultCountriesList
  private val form = CheckPlatformOperatorFormProvider()()
  private val mockPlatformOperatorConnector = mock[PlatformOperatorConnector]
  private val mockSubmissionConnector = mock[SubmissionConnector]
  private val mockConfirmedDetailsService = mock[ConfirmedDetailsService]
  private val mockSessionRepository = mock[SessionRepository]
  private val operatorSummary = PlatformOperatorSummary("operatorId", "operatorName", "primaryContactName", "test@test.com", hasReportingNotifications = true)
  private val baseAnswers = emptyUserAnswers.set(PlatformOperatorSummaryQuery, operatorSummary).success.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockPlatformOperatorConnector, mockSubmissionConnector, mockConfirmedDetailsService, mockSessionRepository)
    super.beforeEach()
  }

  "Check Platform Operator Controller" - {
    "onPageLoad(...)" - {

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

      "must return OK and the correct view for a GET" in {

        when(mockPlatformOperatorConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector))
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

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = baseAnswers.set(CheckPlatformOperatorPage, true).success.value

        when(mockPlatformOperatorConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector))
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
          contentAsString(result) mustEqual view(form.fill(true), operatorList, primaryContactList, None, "operatorId", "operatorName")(request, implicitly).toString
        }

      }

      "must redirect to SubmissionsDisabled for a GET when submissions are disabled" in {

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .configure("features.submissions-enabled" -> false)
            .build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckPlatformOperatorController.onPageLoad(operatorId).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual baseRoutes.SubmissionsDisabledController.onPageLoad().url
        }
      }

      "must redirect to XmlSubmissionAlreadySent for a GET when XmlSubmissionSentPage is true" - {

        val userAnswers = baseAnswers.set(XmlSubmissionSentPage, true).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckPlatformOperatorController.onPageLoad(operatorId).url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.submission.routes.XmlSubmissionAlreadySentController.onPageLoad().url
        }
      }

    }

    "onSubmit(...)" - {
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

        when(mockPlatformOperatorConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector))
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
          contentAsString(result) mustEqual
            view(formWithErrors, operatorList, primaryContactList, None, "operatorId", "operatorName")(request, implicitly).toString
        }
      }

      "must redirect to SubmissionsDisabled for a POST when submissions are disabled" in {

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .configure("features.submissions-enabled" -> false)
            .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.CheckPlatformOperatorController.onPageLoad(operatorId).url)
              .withFormUrlEncodedBody("value" -> "operatorId")

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual baseRoutes.SubmissionsDisabledController.onPageLoad().url
        }
      }

      "must redirect to Check Platform Operator when 'true' is selected and business details have not been confirmed" in {
        val application = applicationBuilder(userAnswers = Some(aUserAnswers)).overrides(
          bind[SubmissionConnector].toInstance(mockSubmissionConnector),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService),
          bind[SessionRepository].toInstance(mockSessionRepository)
        ).build()

        val expectedAnswers = aUserAnswers.set(CheckPlatformOperatorPage, true).success.value
        when(mockConfirmedDetailsService.confirmBusinessDetailsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(businessDetails = false)))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        running(application) {
          val request = FakeRequest(POST, routes.CheckPlatformOperatorController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "true")
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckPlatformOperatorController.onPageLoad(operatorId).url
        }

        verify(mockConfirmedDetailsService, times(1)).confirmBusinessDetailsFor(eqTo(operatorId))(using any())
        verify(mockSubmissionConnector, never()).start(any(), any(), any())(using any())
        verify(mockSessionRepository, times(1)).set(expectedAnswers)
      }

      "must redirect to Check Reporting Notifications when 'true' is selected and notifications have not been confirmed" in {
        val application = applicationBuilder(userAnswers = Some(aUserAnswers)).overrides(
          bind[SubmissionConnector].toInstance(mockSubmissionConnector),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService),
          bind[SessionRepository].toInstance(mockSessionRepository)
        ).build()

        val expectedAnswers = aUserAnswers.set(CheckPlatformOperatorPage, true).success.value
        when(mockConfirmedDetailsService.confirmBusinessDetailsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(reportingNotifications = false)))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        running(application) {
          val request = FakeRequest(POST, routes.CheckPlatformOperatorController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "true")
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckReportingNotificationsController.onPageLoad(operatorId).url
        }

        verify(mockConfirmedDetailsService, times(1)).confirmBusinessDetailsFor(eqTo(operatorId))(using any())
        verify(mockSubmissionConnector, never()).start(any(), any(), any())(using any())
        verify(mockSessionRepository, times(1)).set(expectedAnswers)
      }

      "must redirect to Check Contact details when 'true' is selected and contact details have not been confirmed" in {
        val application = applicationBuilder(userAnswers = Some(aUserAnswers)).overrides(
          bind[SubmissionConnector].toInstance(mockSubmissionConnector),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService),
          bind[SessionRepository].toInstance(mockSessionRepository)
        ).build()

        val expectedAnswers = aUserAnswers.set(CheckPlatformOperatorPage, true).success.value
        when(mockConfirmedDetailsService.confirmBusinessDetailsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(yourContactDetails = false)))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        running(application) {
          val request = FakeRequest(POST, routes.CheckPlatformOperatorController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "true")
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckContactDetailsController.onPageLoad(operatorId).url
        }

        verify(mockConfirmedDetailsService, times(1)).confirmBusinessDetailsFor(eqTo(operatorId))(using any())
        verify(mockSubmissionConnector, never()).start(any(), any(), any())(using any())
        verify(mockSessionRepository, times(1)).set(expectedAnswers)
      }

      "must redirect to Upload page when 'true' is selected and all details have been confirmed" in {
        val user = aUserAnswers.set(PlatformOperatorSummaryQuery, operatorSummary).success.value
        val application = applicationBuilder(userAnswers = Some(user)).overrides(
          bind[SubmissionConnector].toInstance(mockSubmissionConnector),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService),
          bind[SessionRepository].toInstance(mockSessionRepository)
        ).build()

        val expectedAnswers = user.set(CheckPlatformOperatorPage, true).success.value
        when(mockConfirmedDetailsService.confirmBusinessDetailsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(true, true, true)))
        when(mockSubmissionConnector.start(any(), any(), any())(using any())).thenReturn(Future.successful(aSubmission))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        running(application) {
          val request = FakeRequest(POST, routes.CheckPlatformOperatorController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "true")
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UploadController.onPageLoad(operatorId, aSubmission._id).url
        }

        verify(mockSubmissionConnector, times(1)).start(eqTo(operatorId), eqTo(operatorSummary.operatorName), eqTo(None))(using any())
        verify(mockConfirmedDetailsService, times(1)).confirmBusinessDetailsFor(eqTo(operatorId))(using any())
        verify(mockSessionRepository, times(1)).set(expectedAnswers)
      }

      "must redirect to update the platform operator when `false` is submitted" in {
        val application = applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        val expectedAnswers = baseAnswers.set(CheckPlatformOperatorPage, false).success.value
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        running(application) {
          val request = FakeRequest(POST, routes.CheckPlatformOperatorController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "false")
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual appConfig.updateOperatorUrl(operatorId)
          verify(mockSessionRepository, times(1)).set(expectedAnswers)
        }
      }
    }
  }
}
