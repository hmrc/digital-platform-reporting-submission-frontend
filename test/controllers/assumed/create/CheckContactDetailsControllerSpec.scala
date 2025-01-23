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

import base.SpecBase
import config.FrontendAppConfig
import connectors.SubscriptionConnector
import controllers.routes as baseRoutes
import forms.CheckContactDetailsFormProvider
import models.subscription.*
import models.NormalMode
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.assumed.create.CheckContactDetailsPage
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.ConfirmedDetailsService
import support.builders.ConfirmedDetailsBuilder.aConfirmedDetails
import support.builders.UserAnswersBuilder.aUserAnswers
import viewmodels.checkAnswers.subscription.*
import viewmodels.govuk.SummaryListFluency
import views.html.assumed.create.CheckContactDetailsView

import scala.concurrent.Future

class CheckContactDetailsControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach {

  private val form = CheckContactDetailsFormProvider()()
  private val mockSubscriptionConnector = mock[SubscriptionConnector]
  private val mockSessionRepository = mock[SessionRepository]
  private val mockConfirmedDetailsService = mock[ConfirmedDetailsService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockSubscriptionConnector, mockSessionRepository, mockConfirmedDetailsService)
    super.beforeEach()
  }

  "Check Contact Details Controller" - {

    val contact = IndividualContact(Individual("first", "last"), "email", None)
    val subscription = SubscriptionInfo(
      id = "dprsId",
      gbUser = true,
      tradingName = None,
      primaryContact = contact,
      secondaryContact = None
    )

    "must return OK and the correct view for a GET" - {

      "for an individual" in {

        when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[SubscriptionConnector].toInstance(mockSubscriptionConnector))
            .build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckContactDetailsController.onPageLoad(operatorId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckContactDetailsView]

          implicit val msgs: Messages = messages(application)

          val summaryList = SummaryListViewModel(Seq(
            IndividualEmailSummary.row(contact),
            CanPhoneIndividualSummary.row(contact)
          ).flatten)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, summaryList, "operatorId")(request, implicitly).toString
        }
      }

      "for an organisation with one contact" in {

        val contact = OrganisationContact(Organisation("name"), "email", None)
        val subscription = SubscriptionInfo(
          id = "dprsId",
          gbUser = true,
          tradingName = None,
          primaryContact = contact,
          secondaryContact = None
        )

        when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[SubscriptionConnector].toInstance(mockSubscriptionConnector))
            .build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckContactDetailsController.onPageLoad(operatorId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckContactDetailsView]

          implicit val msgs: Messages = messages(application)

          val summaryList = SummaryListViewModel(Seq(
            PrimaryContactNameSummary.row(contact),
            PrimaryContactEmailSummary.row(contact),
            CanPhonePrimaryContactSummary.row(contact),
            HasSecondaryContactSummary.row(None)
          ).flatten)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, summaryList, "operatorId")(request, implicitly).toString
        }
      }

      "for an organisation with two contacts" in {

        val contact1 = OrganisationContact(Organisation("name"), "email", Some("phone"))
        val contact2 = OrganisationContact(Organisation("name2"), "email2", Some("phone2"))
        val subscription = SubscriptionInfo(
          id = "dprsId",
          gbUser = true,
          tradingName = None,
          primaryContact = contact1,
          secondaryContact = Some(contact2)
        )

        when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[SubscriptionConnector].toInstance(mockSubscriptionConnector))
            .build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckContactDetailsController.onPageLoad(operatorId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckContactDetailsView]

          implicit val msgs: Messages = messages(application)

          val summaryList = SummaryListViewModel(Seq(
            PrimaryContactNameSummary.row(contact1),
            PrimaryContactEmailSummary.row(contact1),
            CanPhonePrimaryContactSummary.row(contact1),
            PrimaryContactPhoneSummary.row(contact1),
            HasSecondaryContactSummary.row(Some(contact2)),
            SecondaryContactNameSummary.row(Some(contact2)),
            SecondaryContactEmailSummary.row(Some(contact2)),
            CanPhoneSecondaryContactSummary.row(Some(contact2)),
            SecondaryContactPhoneSummary.row(Some(contact2))
          ).flatten)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, summaryList, "operatorId")(request, implicitly).toString
        }
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val mockAppConfig = mock[FrontendAppConfig]
      val baseAnswers = emptyUserAnswers.set(CheckContactDetailsPage(mockAppConfig), true).success.value

      when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[SubscriptionConnector].toInstance(mockSubscriptionConnector))
          .build()
      
      running(application) {
        val request = FakeRequest(GET, routes.CheckContactDetailsController.onPageLoad(operatorId).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[CheckContactDetailsView]
        
        implicit val msgs: Messages = messages(application)

        val summaryList = SummaryListViewModel(Seq(
          IndividualEmailSummary.row(contact),
          CanPhoneIndividualSummary.row(contact)
        ).flatten)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), summaryList, "operatorId")(request, implicitly).toString
      }

    }

    "must redirect to AssumedReportingDisabled when submissions are disabled" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.submissions-enabled" -> false)
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckContactDetailsController.onPageLoad(operatorId).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
      }
    }

    "onSubmit(...)" - {
      "must return BadRequest and errors when an invalid answer is submitted" in {

        when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[SubscriptionConnector].toInstance(mockSubscriptionConnector))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.CheckContactDetailsController.onPageLoad(operatorId).url)
              .withFormUrlEncodedBody("value" -> "invalid value")

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckContactDetailsView]

          implicit val msgs: Messages = messages(application)

          val summaryList = SummaryListViewModel(Seq(
            IndividualEmailSummary.row(contact),
            CanPhoneIndividualSummary.row(contact)
          ).flatten)

          val formWithErrors = form.bind(Map("value" -> "invalid value"))

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(formWithErrors, summaryList, "operatorId")(request, implicitly).toString
        }
      }

      "must redirect to add a reporting notification when `false` is submitted" in {
        val application = applicationBuilder(userAnswers = Some(aUserAnswers)).overrides(
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()
        val page = application.injector.instanceOf[CheckContactDetailsPage]
        val expectedAnswers = aUserAnswers.set(page, false).success.value

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        running(application) {
          val request = FakeRequest(POST, routes.CheckContactDetailsController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "false")
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual appConfig.updateContactDetailsUrl
        }

        verify(mockSessionRepository, times(1)).set(expectedAnswers)
        verify(mockConfirmedDetailsService, never()).confirmContactDetailsFor(any())(using any())
      }

      "must redirect to Check Platform Operator when 'true' is selected and business details have not been confirmed" in {
        val application = applicationBuilder(userAnswers = Some(aUserAnswers)).overrides(
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()
        val page = application.injector.instanceOf[CheckContactDetailsPage]
        val expectedAnswers = aUserAnswers.set(page, true).success.value

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockConfirmedDetailsService.confirmContactDetailsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(businessDetails = false)))

        running(application) {
          val request = FakeRequest(POST, routes.CheckContactDetailsController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "true")
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckPlatformOperatorController.onPageLoad(operatorId).url
        }

        verify(mockSessionRepository, times(1)).set(expectedAnswers)
        verify(mockConfirmedDetailsService, times(1)).confirmContactDetailsFor(eqTo(operatorId))(using any())
      }

      "must redirect to Check Reporting Notifications when 'true' is selected and notifications have not been confirmed" in {
        val application = applicationBuilder(userAnswers = Some(aUserAnswers)).overrides(
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()
        val page = application.injector.instanceOf[CheckContactDetailsPage]
        val expectedAnswers = aUserAnswers.set(page, true).success.value

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockConfirmedDetailsService.confirmContactDetailsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(reportingNotifications = false)))

        running(application) {
          val request = FakeRequest(POST, routes.CheckContactDetailsController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "true")
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckReportingNotificationsController.onPageLoad(operatorId).url
        }

        verify(mockSessionRepository, times(1)).set(expectedAnswers)
        verify(mockConfirmedDetailsService, times(1)).confirmContactDetailsFor(eqTo(operatorId))(using any())
      }

      "must redirect to Check Contact details when 'true' is selected and contact details have not been confirmed" in {
        val application = applicationBuilder(userAnswers = Some(aUserAnswers)).overrides(
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()
        val page = application.injector.instanceOf[CheckContactDetailsPage]
        val expectedAnswers = aUserAnswers.set(page, true).success.value

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockConfirmedDetailsService.confirmContactDetailsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(yourContactDetails = false)))

        running(application) {
          val request = FakeRequest(POST, routes.CheckContactDetailsController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "true")
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckContactDetailsController.onPageLoad(operatorId).url
        }

        verify(mockSessionRepository, times(1)).set(expectedAnswers)
        verify(mockConfirmedDetailsService, times(1)).confirmContactDetailsFor(eqTo(operatorId))(using any())
      }

      "must redirect to Reporting Period when all details are confirmed" in {
        val application = applicationBuilder(userAnswers = Some(aUserAnswers)).overrides(
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()
        val page = application.injector.instanceOf[CheckContactDetailsPage]
        val expectedAnswers = aUserAnswers.set(page, true).success.value

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockConfirmedDetailsService.confirmContactDetailsFor(any())(using any()))
          .thenReturn(Future.successful(aConfirmedDetails.copy(true, true, true)))

        running(application) {
          val request = FakeRequest(POST, routes.CheckContactDetailsController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "true")
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.ReportingPeriodController.onPageLoad(NormalMode, operatorId).url
        }

        verify(mockSessionRepository, times(1)).set(expectedAnswers)
        verify(mockConfirmedDetailsService, times(1)).confirmContactDetailsFor(eqTo(operatorId))(using any())
      }

      "must redirect to AssumedReportingDisabled when submissions are disabled" in {

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .configure("features.submissions-enabled" -> false)
            .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.CheckContactDetailsController.onPageLoad(operatorId).url)
              .withFormUrlEncodedBody("value" -> "invalid value")

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
        }
      }
    }
  }
}
