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
import cats.data.NonEmptyChain
import connectors.AssumedReportingConnector
import controllers.routes as baseRoutes
import models.{UserAnswers, yearFormat}
import models.submission.Submission.State.Submitted
import models.submission.Submission.SubmissionType
import models.submission.{AssumedReportSummary, AssumedReportingSubmission, AssumedReportingSubmissionRequest, AssumingPlatformOperator, Submission}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.{ArgumentCaptor, Mockito}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.assumed.update.AssumingOperatorNamePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AssumedReportSummaryQuery, PlatformOperatorNameQuery, ReportingPeriodQuery}
import repositories.SessionRepository
import services.UserAnswersService
import viewmodels.govuk.SummaryListFluency
import views.html.assumed.update.CheckYourAnswersView

import java.time.{Instant, Year}
import scala.concurrent.Future
import scala.util.Success

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with BeforeAndAfterEach {

  private val reportingPeriod = Year.of(2024)
  private val baseAnswers = emptyUserAnswers.copy(reportingPeriod = Some(reportingPeriod))
  private val mockAssumedReportingConnector: AssumedReportingConnector = mock[AssumedReportingConnector]
  private val mockUserAnswersService: UserAnswersService = mock[UserAnswersService]
  private val mockSessionRepository: SessionRepository = mock[SessionRepository]

  private val now: Instant = Instant.now()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockAssumedReportingConnector, mockUserAnswersService, mockSessionRepository)
  }

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

      "must submit an assumed reporting submission request, replace user answers with a summary, and redirect to the next page" in {

        val assumedReportingSubmissionRequest = AssumedReportingSubmissionRequest(
          operatorId = "operatorId",
          assumingOperator = AssumingPlatformOperator(
            name = "assumingOperator",
            residentCountry = "GB",
            tinDetails = Seq.empty,
            registeredCountry = "GB",
            address = "address"
          ),
          reportingPeriod = Year.of(2024)
        )

        val submission = Submission(
          _id = "submissionId",
          submissionType = SubmissionType.ManualAssumedReport,
          dprsId = "dprsId",
          operatorId = "operatorId",
          operatorName = operatorName,
          assumingOperatorName = Some("assumingOperatorName"),
          state = Submitted(fileName = "test.xml", Year.of(2024)),
          created = now,
          updated = now
        )

        val answers =
          baseAnswers
            .set(AssumingOperatorNamePage, "assumingOperatorName").success.value
            .set(PlatformOperatorNameQuery, "operatorName").success.value
            .set(ReportingPeriodQuery, Year.of(2024)).success.value

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[AssumedReportingConnector].toInstance(mockAssumedReportingConnector),
            bind[UserAnswersService].toInstance(mockUserAnswersService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        when(mockUserAnswersService.toAssumedReportingSubmission(any())).thenReturn(Right(assumedReportingSubmissionRequest))
        when(mockAssumedReportingConnector.submit(any())(using any())).thenReturn(Future.successful(submission))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        running(application) {
          val request = FakeRequest(routes.CheckYourAnswersController.onSubmit(operatorId, reportingPeriod))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.SubmissionConfirmationController.onPageLoad(operatorId, reportingPeriod).url
        }

        verify(mockUserAnswersService).toAssumedReportingSubmission(eqTo(answers))
        verify(mockAssumedReportingConnector).submit(eqTo(assumedReportingSubmissionRequest))(using any())

        val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository, times(1)).set(answersCaptor.capture())
        
        val finalAnswers = answersCaptor.getValue
        finalAnswers.get(AssumedReportSummaryQuery).value mustEqual AssumedReportSummary(operatorId, operatorName, "assumingOperatorName", Year.of(2024))
        finalAnswers.get(ReportingPeriodQuery)            must not be defined
        finalAnswers.get(PlatformOperatorNameQuery)       must not be defined
        finalAnswers.get(AssumingOperatorNamePage)        must not be defined
      }

      "must fail if a request cannot be created from the user answers" in {

        val application = applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[AssumedReportingConnector].toInstance(mockAssumedReportingConnector),
            bind[UserAnswersService].toInstance(mockUserAnswersService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        when(mockUserAnswersService.toAssumedReportingSubmission(any())).thenReturn(Left(NonEmptyChain.one(AssumingOperatorNamePage)))
        when(mockAssumedReportingConnector.submit(any())(using any())).thenReturn(Future.successful(Done))
        when(mockSessionRepository.clear(any(), any(), any())).thenReturn(Future.successful(true))

        running(application) {
          val request = FakeRequest(routes.CheckYourAnswersController.onSubmit(operatorId, reportingPeriod))
          route(application, request).value.failed.futureValue
        }

        verify(mockAssumedReportingConnector, never()).submit(any())(using any())
        verify(mockSessionRepository, never()).clear(any(), any(), any())
      }
    }

    "initialise" - {

      "when a MAR can be found" - {

        "must create and save user answers, then redirect to CYA onPageLoad" in {

          val submission = AssumedReportingSubmission(
            operatorId = "operatorId",
            operatorName = "operatorName",
            assumingOperator = AssumingPlatformOperator(
              name = "assumingOperator",
              residentCountry = "not a country",
              tinDetails = Seq.empty,
              registeredCountry = "GB",
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
