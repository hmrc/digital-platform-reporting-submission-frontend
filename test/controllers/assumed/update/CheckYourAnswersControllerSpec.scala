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
import builders.AssumedReportSummaryBuilder.anAssumedReportSummary
import builders.AssumedReportingSubmissionBuilder.anAssumedReportingSubmission
import builders.AssumingPlatformOperatorBuilder.anAssumingPlatformOperator
import builders.PlatformOperatorSummaryBuilder.aPlatformOperatorSummary
import connectors.AssumedReportingConnector
import controllers.routes as baseRoutes
import models.Country.UnitedKingdom
import models.audit.UpdateAssumedReportEvent
import models.email.EmailsSentResult
import models.submission.*
import models.{CountriesList, Country, DefaultCountriesList, UserAnswers, yearFormat}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.assumed.create
import pages.assumed.update.*
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.*
import repositories.SessionRepository
import services.{AuditService, EmailService, UserAnswersService}
import support.builders.SubmissionBuilder.aSubmission
import viewmodels.PlatformOperatorSummary
import viewmodels.govuk.SummaryListFluency
import views.html.assumed.update.CheckYourAnswersView

import java.time.{Instant, Year}
import scala.concurrent.Future
import scala.util.Success

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with BeforeAndAfterEach {

  private implicit val countriesList: CountriesList = new DefaultCountriesList
  private val reportingPeriod = Year.of(2024)
  private val baseAnswers = emptyUserAnswers.copy(reportingPeriod = Some(reportingPeriod))
  private val mockAssumedReportingConnector: AssumedReportingConnector = mock[AssumedReportingConnector]
  private val mockUserAnswersService: UserAnswersService = mock[UserAnswersService]
  private val mockSessionRepository: SessionRepository = mock[SessionRepository]
  private val mockAuditService: AuditService = mock[AuditService]
  private val mockEmailService = mock[EmailService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockAssumedReportingConnector, mockUserAnswersService, mockSessionRepository, mockAuditService, mockEmailService)
  }

  private val dprsId = "dprsId"

  "Check Your Answers Controller" - {
    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(operatorId, reportingPeriod).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[CheckYourAnswersView]
        val list = SummaryListViewModel(Seq.empty)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(list, operatorId, reportingPeriod)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(operatorId, reportingPeriod).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "for a POST" - {
      val assumedReportingSubmissionRequest = AssumedReportingSubmissionRequest(
        operatorId = operatorId,
        assumingOperator = anAssumingPlatformOperator,
        reportingPeriod = Year.now
      )

      "must submit an assumed reporting submission request, audit the event, replace user answers with a summary, send email, and redirect to the next page" in {
        val answers = baseAnswers
          .set(AssumedReportingSubmissionQuery, anAssumedReportingSubmission).success.value
          .set(create.ReportingPeriodPage, Year.now).success.value
          .set(AssumingOperatorNamePage, anAssumingPlatformOperator.name).success.value
          .set(TaxResidentInUkPage, true).success.value
          .set(HasUkTaxIdentifierPage, true).success.value
          .set(UkTaxIdentifierPage, anAssumingPlatformOperator.tinDetails.head.tin).success.value
          .set(RegisteredCountryPage, UnitedKingdom).success.value
          .set(AddressPage, anAssumingPlatformOperator.address).success.value
          .set(PlatformOperatorNameQuery, operatorName).success.value
          .set(ReportingPeriodQuery, Year.now).success.value
          .set(PlatformOperatorSummaryQuery, aPlatformOperatorSummary).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).overrides(
          bind[AssumedReportingConnector].toInstance(mockAssumedReportingConnector),
          bind[UserAnswersService].toInstance(mockUserAnswersService),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[AuditService].toInstance(mockAuditService),
          bind[EmailService].toInstance(mockEmailService),
          bind[CountriesList].toInstance(countriesList)
        ).build()

        when(mockAssumedReportingConnector.submit(any())(using any())).thenReturn(Future.successful(aSubmission))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockEmailService.sendUpdateAssumedReportingEmails(any(), any(), any())(using any())).thenReturn(Future.successful(EmailsSentResult(true, Some(true))))

        running(application) {
          val request = FakeRequest(routes.CheckYourAnswersController.onSubmit(operatorId, reportingPeriod))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.SubmissionConfirmationController.onPageLoad(operatorId, reportingPeriod).url
        }

        val expectedAuditEvent = UpdateAssumedReportEvent(dprsId, anAssumedReportingSubmission, assumedReportingSubmissionRequest, countriesList)

        verify(mockAssumedReportingConnector).submit(eqTo(assumedReportingSubmissionRequest))(using any())
        verify(mockAuditService).audit(eqTo(expectedAuditEvent))(using any(), any())

        val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository, times(1)).set(answersCaptor.capture())
        val expectedAssumedReportSummary = anAssumedReportSummary.copy(operatorId = operatorId, operatorName = operatorName, assumingOperatorName = anAssumingPlatformOperator.name)
        verify(mockEmailService, times(1)).sendUpdateAssumedReportingEmails(eqTo(operatorId), eqTo(expectedAssumedReportSummary), eqTo(aSubmission.updated))(using any())

        val finalAnswers = answersCaptor.getValue
        finalAnswers.get(AssumedReportSummaryQuery).value mustEqual expectedAssumedReportSummary
        finalAnswers.get(SentUpdateAssumedReportingEmailsQuery).value mustEqual EmailsSentResult(true, Some(true))
        finalAnswers.get(ReportingPeriodQuery) must not be defined
        finalAnswers.get(PlatformOperatorNameQuery) must not be defined
        finalAnswers.get(AssumingOperatorNamePage) must not be defined
      }

      "must fail if a request cannot be created from the user answers" in {
        val answers = baseAnswers
          .set(AssumedReportingSubmissionQuery, anAssumedReportingSubmission).success.value
          .set(create.ReportingPeriodPage, Year.now).success.value
          .set(AssumingOperatorNamePage, anAssumingPlatformOperator.name).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).overrides(
          bind[AssumedReportingConnector].toInstance(mockAssumedReportingConnector),
          bind[UserAnswersService].toInstance(mockUserAnswersService),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[CountriesList].toInstance(countriesList),
          bind[EmailService].toInstance(mockEmailService)
        ).build()

        when(mockAssumedReportingConnector.submit(any())(using any())).thenReturn(Future.successful(Done))
        when(mockSessionRepository.clear(any(), any(), any())).thenReturn(Future.successful(true))

        running(application) {
          val request = FakeRequest(routes.CheckYourAnswersController.onSubmit(operatorId, reportingPeriod))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.MissingInformationController.onPageLoad(operatorId, reportingPeriod).url
        }

        verify(mockAssumedReportingConnector, never()).submit(any())(using any())
        verify(mockSessionRepository, never()).set(any())
        verify(mockEmailService, never()).sendUpdateAssumedReportingEmails(any(), any(), any())(using any())
      }
    }

    "initialise" - {
      "when a MAR can be found" - {
        "must create and save user answers, then redirect to CYA onPageLoad" in {
          val submission = AssumedReportingSubmission(
            operatorId = operatorId,
            operatorName = operatorName,
            assumingOperator = AssumingPlatformOperator(
              name = "assumingOperator",
              residentCountry = Country.UnitedKingdom,
              tinDetails = Seq.empty,
              registeredCountry = Country.UnitedKingdom,
              address = "address"
            ),
            reportingPeriod = Year.of(2024),
            isDeleted = false
          )

          val userAnswers = emptyUserAnswers

          when(mockAssumedReportingConnector.get(any(), any())(using any())).thenReturn(Future.successful(Some(submission)))
          when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
          when(mockUserAnswersService.fromAssumedReportingSubmission(any(), any())).thenReturn(Success(userAnswers))

          val application = applicationBuilder(userAnswers = None)
            .overrides(
              bind[AssumedReportingConnector].toInstance(mockAssumedReportingConnector),
              bind[UserAnswersService].toInstance(mockUserAnswersService),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

          running(application) {
            val request = FakeRequest(routes.CheckYourAnswersController.initialise(operatorId, reportingPeriod))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(operatorId, reportingPeriod).url

            verify(mockAssumedReportingConnector).get(eqTo(operatorId), eqTo(reportingPeriod))(using any())
            verify(mockUserAnswersService).fromAssumedReportingSubmission(eqTo(userAnswers.userId), eqTo(submission))
            verify(mockSessionRepository).set(eqTo(userAnswers))
          }
        }
      }

      "when a MAR cannot be found" - {

        "must redirect to Journey Recovery" in {

          when(mockAssumedReportingConnector.get(any(), any())(using any())).thenReturn(Future.successful(None))

          val application = applicationBuilder(userAnswers = None)
            .overrides(
              bind[AssumedReportingConnector].toInstance(mockAssumedReportingConnector),
              bind[UserAnswersService].toInstance(mockUserAnswersService),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

          running(application) {
            val request = FakeRequest(routes.CheckYourAnswersController.initialise(operatorId, reportingPeriod))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url

            verify(mockAssumedReportingConnector).get(eqTo(operatorId), eqTo(reportingPeriod))(using any())
            verify(mockUserAnswersService, never()).fromAssumedReportingSubmission(any(), any())
            verify(mockSessionRepository, never()).set(any())
          }
        }
      }
    }
  }
}
