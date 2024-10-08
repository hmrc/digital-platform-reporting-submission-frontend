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
import forms.CheckContactDetailsFormProvider
import models.submission.Submission
import models.submission.Submission.State.Ready
import models.subscription.*
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{mockitoSession, never, times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PlatformOperatorSummaryQuery
import repositories.SessionRepository
import viewmodels.PlatformOperatorSummary
import viewmodels.checkAnswers.subscription.*
import viewmodels.govuk.SummaryListFluency
import views.html.submission.CheckContactDetailsView

import java.time.Instant
import scala.concurrent.Future

class CheckContactDetailsControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach {

  private val form = CheckContactDetailsFormProvider()()
  private val mockSubmissionConnector: SubmissionConnector = mock[SubmissionConnector]
  private val mockSubscriptionConnector = mock[SubscriptionConnector]
  private val mockRepository = mock[SessionRepository]
  private val platformOperatorSummary = PlatformOperatorSummary("operatorId", "operatorName", true)
  private val baseAnswers = emptyUserAnswers.set(PlatformOperatorSummaryQuery, platformOperatorSummary).success.value

  private val now: Instant = Instant.now()

  override def beforeEach(): Unit = {
    Mockito.reset(mockSubscriptionConnector, mockRepository, mockSubmissionConnector)
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

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
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

    "when `true` is submitted" - {
      
      "must start a new submission and redirect to upload when `true` is submitted" in {

        val submissionId = "id"
        val submission = Submission(
          _id = "id",
          dprsId = "dprsId",
          operatorId = "operatorId",
          operatorName = "operatorName",
          assumingOperatorName = None,
          state = Ready,
          created = now,
          updated = now
        )

        when(mockSubmissionConnector.start(any(), any(), any())(using any())).thenReturn(Future.successful(submission))

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[SubmissionConnector].toInstance(mockSubmissionConnector))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.CheckContactDetailsController.onPageLoad(operatorId).url)
              .withFormUrlEncodedBody("value" -> "true")

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UploadController.onPageLoad(operatorId, submissionId).url
          verify(mockSubmissionConnector).start(eqTo("operatorId"), eqTo("operatorName"), eqTo(None))(using any())
        }
      }
      
      "must fail when the call to create a new submission fails" in {

        val application = applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SubmissionConnector].toInstance(mockSubmissionConnector)
          )
          .build()

        when(mockSubmissionConnector.start(any(), any(), any())(using any())).thenReturn(Future.failed(new RuntimeException()))

        running(application) {
          val request = FakeRequest(routes.CheckContactDetailsController.onPageLoad(operatorId))
          route(application, request).value.failed.futureValue
        }
      }
    }

    "must redirect to update contact details when `false` is submitted" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[SubmissionConnector].toInstance(mockSubmissionConnector))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.CheckContactDetailsController.onPageLoad(operatorId).url)
            .withFormUrlEncodedBody("value" -> "false")

        val result = route(application, request).value
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual appConfig.updateContactDetailsUrl
        verify(mockSubmissionConnector, never()).start(any(), any(), any())(using any())
      }
    }
    
  }
}
