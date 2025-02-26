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

import base.SpecBase
import connectors.SubscriptionConnector
import controllers.routes as baseRoutes
import forms.CheckContactDetailsFormProvider
import models.UserAnswers
import models.subscription.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.assumed.AssumedSubmissionSentPage
import pages.assumed.update.CheckContactDetailsPage
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import viewmodels.checkAnswers.subscription.*
import viewmodels.govuk.SummaryListFluency
import views.html.assumed.update.CheckContactDetailsView

import java.time.Year
import scala.concurrent.Future

class CheckContactDetailsControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach {

  private val reportingPeriod = Year.of(2024)
  private val form = CheckContactDetailsFormProvider()()
  private val mockConnector = mock[SubscriptionConnector]
  private val mockRepository = mock[SessionRepository]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector, mockRepository)
    super.beforeEach()
  }

  "Check Contact Details Controller" - {

    "must return OK and the correct view for a GET" - {

      "for an individual" in {

        val contact = IndividualContact(Individual("first", "last"), "email", None)
        val subscription = SubscriptionInfo(
          id = "dprsId",
          gbUser = true,
          tradingName = None,
          primaryContact = contact,
          secondaryContact = None
        )

        when(mockConnector.getSubscription(any())).thenReturn(Future.successful(subscription))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[SubscriptionConnector].toInstance(mockConnector))
            .build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckContactDetailsController.onPageLoad(operatorId, reportingPeriod).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckContactDetailsView]

          implicit val msgs: Messages = messages(application)

          val summaryList = SummaryListViewModel(Seq(
            IndividualEmailSummary.row(contact),
            CanPhoneIndividualSummary.row(contact)
          ).flatten)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, summaryList, operatorId, reportingPeriod)(request, implicitly).toString
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

        when(mockConnector.getSubscription(any())).thenReturn(Future.successful(subscription))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[SubscriptionConnector].toInstance(mockConnector))
            .build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckContactDetailsController.onPageLoad(operatorId, reportingPeriod).url)

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
          contentAsString(result) mustEqual view(form, summaryList, operatorId, reportingPeriod)(request, implicitly).toString
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

        when(mockConnector.getSubscription(any())).thenReturn(Future.successful(subscription))

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[SubscriptionConnector].toInstance(mockConnector))
            .build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckContactDetailsController.onPageLoad(operatorId, reportingPeriod).url)

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
          contentAsString(result) mustEqual view(form, summaryList, operatorId, reportingPeriod)(request, implicitly).toString
        }
      }
    }

    "must redirect to AssumedReportingDisabled for a GET when submissions are disabled" - {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.submissions-enabled" -> false)
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckContactDetailsController.onPageLoad(operatorId, reportingPeriod).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
      }
    }

    "must redirect to AssumedSubmissionAlreadySent for a GET when AssumedSubmissionSentPage is true" in {

      val userAnswers = emptyUserAnswers.set(AssumedSubmissionSentPage, true).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckContactDetailsController.onPageLoad(operatorId, reportingPeriod).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.assumed.routes.AssumedSubmissionAlreadySentController.onPageLoad().url
      }
    }

    "must return BadRequest and errors when an invalid answer is submitted" in {

      val contact = IndividualContact(Individual("first", "last"), "email", None)
      val subscription = SubscriptionInfo(
        id = "dprsId",
        gbUser = true,
        tradingName = None,
        primaryContact = contact,
        secondaryContact = None
      )

      when(mockConnector.getSubscription(any())).thenReturn(Future.successful(subscription))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[SubscriptionConnector].toInstance(mockConnector))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.CheckContactDetailsController.onPageLoad(operatorId, reportingPeriod).url)
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
        contentAsString(result) mustEqual view(formWithErrors, summaryList, operatorId, reportingPeriod)(request, implicitly).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      when(mockRepository.set(any())).thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[SessionRepository].toInstance(mockRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.CheckContactDetailsController.onPageLoad(operatorId, reportingPeriod).url)
            .withFormUrlEncodedBody("value" -> "true")

        val page = application.injector.instanceOf[CheckContactDetailsPage]
        val expectedAnswers = emptyUserAnswers.set(page, true).success.value
        val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual page.nextPage(reportingPeriod, expectedAnswers).url
        verify(mockRepository, times(1)).set(answersCaptor.capture())

        val answers = answersCaptor.getValue
        answers.get(page).value mustEqual true
      }
    }

    "must redirect to AssumedReportingDisabled for a POST when submissions are disabled" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.submissions-enabled" -> false)
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.CheckContactDetailsController.onPageLoad(operatorId, reportingPeriod).url)
            .withFormUrlEncodedBody("value" -> "invalid value")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
      }
    }
  }
}
