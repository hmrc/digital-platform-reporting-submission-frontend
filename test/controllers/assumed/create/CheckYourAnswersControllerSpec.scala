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
import builders.AssumingPlatformOperatorBuilder.anAssumingPlatformOperator
import builders.EmailsSentResultBuilder.anEmailsSentResult
import builders.PlatformOperatorSummaryBuilder.aPlatformOperatorSummary
import connectors.AssumedReportingConnector
import connectors.AssumedReportingConnector.SubmitAssumedReportingFailure
import controllers.routes as baseRoutes
import models.Country.UnitedKingdom
import models.audit.AddAssumedReportEvent
import models.submission.{AssumedReportSummary, AssumedReportingSubmissionRequest}
import models.{CountriesList, Country, DefaultCountriesList, UserAnswers, yearFormat}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.assumed.create.*
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AssumedReportSummaryQuery, PlatformOperatorSummaryQuery, ReportingPeriodQuery, SentAddAssumedReportingEmailsQuery}
import repositories.SessionRepository
import services.{AuditService, EmailService}
import support.builders.SubmissionBuilder.aSubmission
import viewmodels.PlatformOperatorSummary
import viewmodels.govuk.SummaryListFluency
import views.html.assumed.create.CheckYourAnswersView

import java.time.{Clock, Instant, Year, ZoneId}
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with BeforeAndAfterEach {

  private val mockAssumedReportingConnector: AssumedReportingConnector = mock[AssumedReportingConnector]
  private val mockSessionRepository: SessionRepository = mock[SessionRepository]
  private val mockAuditService: AuditService = mock[AuditService]
  private val mockEmailService = mock[EmailService]

  private val now: Instant = Instant.now()
  private val stubClock: Clock = Clock.fixed(now, ZoneId.systemDefault)

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockAssumedReportingConnector, mockSessionRepository, mockAuditService, mockEmailService)
  }

  "Check Your Answers Controller" - {
    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(operatorId).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[CheckYourAnswersView]
        val list = SummaryListViewModel(Seq.empty)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(list, operatorId)(request, messages(application)).toString
      }
    }

    "must redirect to AssumedReportingDisabled for a GET when submissions are disabled" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .configure("features.submissions-enabled" -> false)
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(operatorId).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(operatorId).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "for a POST" - {
      val countriesList = new DefaultCountriesList
      val assumedReportingSubmissionRequest = AssumedReportingSubmissionRequest(
        operatorId = operatorId,
        assumingOperator = anAssumingPlatformOperator,
        reportingPeriod = Year.of(2024)
      )

      "must submit an assumed reporting submission request, audit the event, replace user answers with a summary, and redirect to the next page" in {
        val answers = emptyUserAnswers
          .set(PlatformOperatorSummaryQuery, aPlatformOperatorSummary).success.value
          .set(ReportingPeriodPage, Year.parse("2024")).success.value
          .set(AssumingOperatorNamePage, anAssumingPlatformOperator.name).success.value
          .set(TaxResidentInUkPage, true).success.value
          .set(HasUkTaxIdentifierPage, true).success.value
          .set(UkTaxIdentifierPage, anAssumingPlatformOperator.tinDetails.head.tin).success.value
          .set(RegisteredCountryPage, UnitedKingdom).success.value
          .set(AddressPage, anAssumingPlatformOperator.address).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).overrides(
          bind[AssumedReportingConnector].toInstance(mockAssumedReportingConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[AuditService].toInstance(mockAuditService),
          bind[EmailService].toInstance(mockEmailService),
          bind[Clock].toInstance(stubClock),
          bind[CountriesList].toInstance(countriesList)
        ).build()

        when(mockAssumedReportingConnector.submit(any())(using any())).thenReturn(Future.successful(aSubmission))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockSessionRepository.clear(any(), any(), any())).thenReturn(Future.successful(true))
        when(mockEmailService.sendAddAssumedReportingEmails(any(), any(), any())(using any())).thenReturn(Future.successful(anEmailsSentResult))

        running(application) {
          val request = FakeRequest(routes.CheckYourAnswersController.onSubmit(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.SubmissionConfirmationController.onPageLoad(operatorId, Year.of(2024)).url
        }

        val expectedAuditEvent = AddAssumedReportEvent("dprsId", aPlatformOperatorSummary.operatorName,
          assumedReportingSubmissionRequest, 200, now, Some(aSubmission._id), countriesList)

        verify(mockAssumedReportingConnector).submit(eqTo(assumedReportingSubmissionRequest))(using any())
        verify(mockAuditService).audit(eqTo(expectedAuditEvent))(using any(), any())

        val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository, times(1)).set(answersCaptor.capture())
        verify(mockSessionRepository, times(1)).clear(userId, operatorId, None)
        verify(mockEmailService, times(1)).sendAddAssumedReportingEmails(any(), any(), any())(using any())

        val finalAnswers = answersCaptor.getValue
        finalAnswers.reportingPeriod.value mustEqual Year.of(2024)
        finalAnswers.get(AssumedReportSummaryQuery).value mustEqual AssumedReportSummary(operatorId, aPlatformOperatorSummary.operatorName, anAssumingPlatformOperator.name, Year.of(2024))
        finalAnswers.get(SentAddAssumedReportingEmailsQuery).value mustEqual anEmailsSentResult
        finalAnswers.get(ReportingPeriodQuery) must not be defined
        finalAnswers.get(PlatformOperatorSummaryQuery) must not be defined
        finalAnswers.get(AssumingOperatorNamePage) must not be defined
      }

      "must fail if a request cannot be created from the user answers" in {
        val answers = emptyUserAnswers
          .set(PlatformOperatorSummaryQuery, aPlatformOperatorSummary).success.value
          .remove(ReportingPeriodPage).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).overrides(
          bind[AssumedReportingConnector].toInstance(mockAssumedReportingConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[AuditService].toInstance(mockAuditService),
          bind[EmailService].toInstance(mockEmailService),
        ).build()

        running(application) {
          val request = FakeRequest(routes.CheckYourAnswersController.onSubmit(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.MissingInformationController.onPageLoad(operatorId).url
        }

        verify(mockAssumedReportingConnector, never()).submit(any())(using any())
        verify(mockSessionRepository, never()).set(any())
        verify(mockAuditService, never()).audit(any())(using any(), any())
        verify(mockEmailService, never()).sendAddAssumedReportingEmails(any(), any(), any())(using any())
      }

      "must audit the event and fail if sending the submission fails" in {
        val answers = emptyUserAnswers
          .set(PlatformOperatorSummaryQuery, aPlatformOperatorSummary).success.value
          .set(ReportingPeriodPage, Year.parse("2024")).success.value
          .set(AssumingOperatorNamePage, anAssumingPlatformOperator.name).success.value
          .set(TaxResidentInUkPage, true).success.value
          .set(HasUkTaxIdentifierPage, true).success.value
          .set(UkTaxIdentifierPage, anAssumingPlatformOperator.tinDetails.head.tin).success.value
          .set(RegisteredCountryPage, UnitedKingdom).success.value
          .set(AddressPage, anAssumingPlatformOperator.address).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).overrides(
          bind[AssumedReportingConnector].toInstance(mockAssumedReportingConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[AuditService].toInstance(mockAuditService),
          bind[EmailService].toInstance(mockEmailService),
          bind[Clock].toInstance(stubClock),
          bind[CountriesList].toInstance(countriesList)
        ).build()

        when(mockAssumedReportingConnector.submit(any())(using any())).thenReturn(Future.failed(SubmitAssumedReportingFailure(500)))

        running(application) {
          val request = FakeRequest(routes.CheckYourAnswersController.onSubmit(operatorId))
          route(application, request).value.failed.futureValue
        }

        val expectedAuditEvent = AddAssumedReportEvent("dprsId", aPlatformOperatorSummary.operatorName,
          assumedReportingSubmissionRequest, 500, now, None, countriesList)

        verify(mockAssumedReportingConnector).submit(eqTo(assumedReportingSubmissionRequest))(using any())
        verify(mockAuditService).audit(eqTo(expectedAuditEvent))(using any(), any())
        verify(mockSessionRepository, never).set(any())
        verify(mockEmailService, never()).sendAddAssumedReportingEmails(any(), any(), any())(using any())
      }

      "must redirect to AssumedReportingDisabled when submissions are disabled" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.submissions-enabled" -> false)
          .build()

        running(application) {
          val request = FakeRequest(routes.CheckYourAnswersController.onSubmit(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
        }
      }
    }
  }
}
