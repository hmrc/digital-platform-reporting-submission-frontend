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
import cats.data.NonEmptyChain
import connectors.SubmissionConnector
import controllers.routes as baseRoutes
import models.submission.Submission.State.Submitted
import models.submission.Submission.SubmissionType
import models.submission.{AssumedReportingSubmission, AssumingPlatformOperator, Submission}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.assumed.create.AssumingOperatorNamePage
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.UserAnswersService
import viewmodels.govuk.SummaryListFluency
import views.html.assumed.create.CheckYourAnswersView

import java.time.{Instant, Year}
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with BeforeAndAfterEach {

  private val mockSubmissionConnector: SubmissionConnector = mock[SubmissionConnector]
  private val mockUserAnswersService: UserAnswersService = mock[UserAnswersService]
  private val mockSessionRepository: SessionRepository = mock[SessionRepository]

  private val now: Instant = Instant.now()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockSubmissionConnector, mockUserAnswersService, mockSessionRepository)
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

      "must submit an assumed reporting submission request, clear other data from user answers and redirect to the next page" in {

        val assumedReportingSubmissionRequest = AssumedReportingSubmission(
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

        val answers = emptyUserAnswers.set(AssumingOperatorNamePage, "assumingOperatorName").success.value

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[SubmissionConnector].toInstance(mockSubmissionConnector),
            bind[UserAnswersService].toInstance(mockUserAnswersService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        when(mockUserAnswersService.toAssumedReportingSubmission(any())).thenReturn(Right(assumedReportingSubmissionRequest))
        when(mockSubmissionConnector.submitAssumedReporting(any())(using any())).thenReturn(Future.successful(submission))
        when(mockSessionRepository.clear(any(), any(), any())).thenReturn(Future.successful(true))

        running(application) {
          val request = FakeRequest(routes.CheckYourAnswersController.onSubmit(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.SubmissionConfirmationController.onPageLoad(operatorId, "submissionId").url
        }

        verify(mockUserAnswersService).toAssumedReportingSubmission(eqTo(answers))
        verify(mockSubmissionConnector).submitAssumedReporting(eqTo(assumedReportingSubmissionRequest))(using any())
        verify(mockSessionRepository).clear(answers.userId, answers.operatorId, answers.reportingPeriod)
      }

      "must fail if a request cannot be created from the user answers" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SubmissionConnector].toInstance(mockSubmissionConnector),
            bind[UserAnswersService].toInstance(mockUserAnswersService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

        when(mockUserAnswersService.toAssumedReportingSubmission(any())).thenReturn(Left(NonEmptyChain.one(AssumingOperatorNamePage)))
        when(mockSubmissionConnector.submitAssumedReporting(any())(using any())).thenReturn(Future.successful(Done))
        when(mockSessionRepository.clear(any(), any(), any())).thenReturn(Future.successful(true))

        running(application) {
          val request = FakeRequest(routes.CheckYourAnswersController.onSubmit(operatorId))
          route(application, request).value.failed.futureValue
        }

        verify(mockSubmissionConnector, never()).submitAssumedReporting(any())(using any())
        verify(mockSessionRepository, never()).clear(any(), any(), any())
      }
    }
  }
}
