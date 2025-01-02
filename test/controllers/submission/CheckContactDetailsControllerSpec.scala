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
import connectors.{SubmissionConnector, SubscriptionConnector}
import controllers.routes as baseRoutes
import forms.CheckContactDetailsFormProvider
import models.subscription.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
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
import viewmodels.checkAnswers.subscription.*
import viewmodels.govuk.SummaryListFluency
import views.html.submission.CheckContactDetailsView

import scala.concurrent.Future

class CheckContactDetailsControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach {

  private val form = CheckContactDetailsFormProvider()()
  private val mockSubmissionConnector: SubmissionConnector = mock[SubmissionConnector]
  private val mockSubscriptionConnector = mock[SubscriptionConnector]
  private val mockRepository = mock[SessionRepository]
  private val mockConfirmedDetailsService = mock[ConfirmedDetailsService]
  private val platformOperatorSummary = PlatformOperatorSummary("operatorId", "operatorName", "primaryContactName", "test@test.com", hasReportingNotifications = true)
  private val baseAnswers = aUserAnswers.set(PlatformOperatorSummaryQuery, platformOperatorSummary).success.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockSubscriptionConnector, mockRepository, mockSubmissionConnector, mockConfirmedDetailsService)
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

        when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))

        val application = applicationBuilder(userAnswers = Some(aUserAnswers))
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

        val application = applicationBuilder(userAnswers = Some(aUserAnswers))
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

        val application = applicationBuilder(userAnswers = Some(aUserAnswers))
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

    "must redirect to SubmissionsDisabled for a GET when submissions are disabled" - {

      val application = applicationBuilder(userAnswers = Some(aUserAnswers))
        .configure("features.submissions-enabled" -> false)
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckContactDetailsController.onPageLoad(operatorId).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.SubmissionsDisabledController.onPageLoad().url
      }
    }

    "onSubmit(...)" - {
      "must return BadRequest and errors when an invalid answer is submitted" in {
        val contact = IndividualContact(Individual("first", "last"), "email", None)
        val subscription = SubscriptionInfo(
          id = "dprsId",
          gbUser = true,
          tradingName = None,
          primaryContact = contact,
          secondaryContact = None
        )

        when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))

        val application = applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[SubscriptionConnector].toInstance(mockSubscriptionConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckContactDetailsController.onPageLoad(operatorId).url)
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

      "must redirect to SubmissionsDisabled for a POST when submissions are disabled" in {

        val application = applicationBuilder(userAnswers = Some(baseAnswers))
          .configure("features.submissions-enabled" -> false)
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckContactDetailsController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "true")
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual baseRoutes.SubmissionsDisabledController.onPageLoad().url
        }
      }

      "must redirect to manage home page when `false` is submitted" in {
        val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
          bind[SubmissionConnector].toInstance(mockSubmissionConnector),
          bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
        ).build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckContactDetailsController.onPageLoad("any-operator-id").url)
            .withFormUrlEncodedBody("value" -> "false")
          val result = route(application, request).value
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual appConfig.updateContactDetailsUrl
        }

        verify(mockSubmissionConnector, never()).start(any(), any(), any())(using any())
        verify(mockConfirmedDetailsService, never()).confirmContactDetailsFor(any())(any())
      }

      "when `true` is submitted" - {
        "must redirect to Check Platform Operator when business details have not been confirmed" in {
          val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
            bind[SubmissionConnector].toInstance(mockSubmissionConnector),
            bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
          ).build()

          when(mockConfirmedDetailsService.confirmContactDetailsFor(any())(any()))
            .thenReturn(Future.successful(aConfirmedDetails.copy(businessDetails = false)))

          running(application) {
            val request = FakeRequest(POST, routes.CheckContactDetailsController.onSubmit("some-operator-id").url)
              .withFormUrlEncodedBody("value" -> "true")
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.CheckPlatformOperatorController.onPageLoad("some-operator-id").url
          }

          verify(mockConfirmedDetailsService, times(1)).confirmContactDetailsFor(eqTo("some-operator-id"))(any())
          verify(mockSubmissionConnector, never()).start(any(), any(), any())(using any())
        }

        "must redirect to Check Reporting Notifications when notifications have not been confirmed" in {
          val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
            bind[SubmissionConnector].toInstance(mockSubmissionConnector),
            bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
          ).build()

          when(mockConfirmedDetailsService.confirmContactDetailsFor(any())(any()))
            .thenReturn(Future.successful(aConfirmedDetails.copy(reportingNotifications = false)))

          running(application) {
            val request = FakeRequest(POST, routes.CheckContactDetailsController.onSubmit("some-operator-id").url)
              .withFormUrlEncodedBody("value" -> "true")
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.CheckReportingNotificationsController.onPageLoad("some-operator-id").url
          }

          verify(mockConfirmedDetailsService, times(1)).confirmContactDetailsFor(eqTo("some-operator-id"))(any())
          verify(mockSubmissionConnector, never()).start(any(), any(), any())(using any())
        }

        "must redirect to Check Contact details when contact details have not been confirmed" in {
          val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
            bind[SubmissionConnector].toInstance(mockSubmissionConnector),
            bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
          ).build()

          when(mockConfirmedDetailsService.confirmContactDetailsFor(any())(any()))
            .thenReturn(Future.successful(aConfirmedDetails.copy(yourContactDetails = false)))

          running(application) {
            val request = FakeRequest(POST, routes.CheckContactDetailsController.onSubmit("some-operator-id").url)
              .withFormUrlEncodedBody("value" -> "true")
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.CheckContactDetailsController.onPageLoad("some-operator-id").url
          }

          verify(mockConfirmedDetailsService, times(1)).confirmContactDetailsFor(eqTo("some-operator-id"))(any())
          verify(mockSubmissionConnector, never()).start(any(), any(), any())(using any())
        }

        "must redirect to Upload page when all details have been confirmed" in {
          val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
            bind[SubmissionConnector].toInstance(mockSubmissionConnector),
            bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
          ).build()

          when(mockConfirmedDetailsService.confirmContactDetailsFor(any())(any()))
            .thenReturn(Future.successful(aConfirmedDetails.copy(true, true, true)))
          when(mockSubmissionConnector.start(any(), any(), any())(using any())).thenReturn(Future.successful(aSubmission))

          running(application) {
            val request = FakeRequest(POST, routes.CheckContactDetailsController.onSubmit("some-operator-id").url)
              .withFormUrlEncodedBody("value" -> "true")
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.UploadController.onPageLoad("some-operator-id", aSubmission._id).url
          }

          verify(mockConfirmedDetailsService, times(1)).confirmContactDetailsFor(eqTo("some-operator-id"))(any())
          verify(mockSubmissionConnector, times(1)).start(eqTo("some-operator-id"), eqTo(platformOperatorSummary.operatorName), eqTo(None))(using any())
        }

        "must fail when the call to create a new submission fails" in {
          val application = applicationBuilder(userAnswers = Some(baseAnswers)).overrides(
            bind[SubmissionConnector].toInstance(mockSubmissionConnector),
            bind[ConfirmedDetailsService].toInstance(mockConfirmedDetailsService)
          ).build()

          when(mockSubmissionConnector.start(any(), any(), any())(using any())).thenReturn(Future.failed(new RuntimeException()))

          running(application) {
            val request = FakeRequest(routes.CheckContactDetailsController.onSubmit("some-operator-id"))
              .withFormUrlEncodedBody("value" -> "true")

            route(application, request).value.failed.futureValue
          }

          verify(mockConfirmedDetailsService, never()).confirmedDetailsFor(any())(using any())
        }
      }
    }
  }
}
