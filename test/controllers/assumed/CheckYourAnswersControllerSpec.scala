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

package controllers.assumed

import base.SpecBase
import cats.data.NonEmptyChain
import connectors.SubmissionConnector
import controllers.routes as baseRoutes
import models.submission.{AssumedReportingSubmissionRequest, AssumingOperatorAddress, AssumingPlatformOperator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.assumed.AssumingOperatorNamePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.UserAnswersService
import viewmodels.govuk.SummaryListFluency
import views.html.assumed.CheckYourAnswersView

import java.time.Year
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with BeforeAndAfterEach {

  private val mockSubmissionConnector: SubmissionConnector = mock[SubmissionConnector]
  private val mockUserAnswersService: UserAnswersService = mock[UserAnswersService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockSubmissionConnector, mockUserAnswersService)
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

        val assumedReportingSubmissionRequest = AssumedReportingSubmissionRequest(
          operatorId = "operatorId",
          assumingOperator = AssumingPlatformOperator(
            name = "assumingOperator",
            residentCountry = "GB",
            tinDetails = Seq.empty,
            address = AssumingOperatorAddress(
              line1 = "line1",
              line2 = None,
              city = "city",
              region = None,
              postCode = "postcode",
              country = "GB"
            )
          ),
          reportingPeriod = Year.of(2024)
        )

        val answers = emptyUserAnswers

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[SubmissionConnector].toInstance(mockSubmissionConnector),
            bind[UserAnswersService].toInstance(mockUserAnswersService)
          )
          .build()

        when(mockUserAnswersService.toAssumedReportingSubmissionRequest(any())).thenReturn(Right(assumedReportingSubmissionRequest))
        when(mockSubmissionConnector.submitAssumedReporting(any())(using any())).thenReturn(Future.successful(Done))

        running(application) {
          val request = FakeRequest(routes.CheckYourAnswersController.onSubmit(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url // TODO update when the next page exists
        }

        verify(mockUserAnswersService).toAssumedReportingSubmissionRequest(eqTo(answers))
        verify(mockSubmissionConnector).submitAssumedReporting(eqTo(assumedReportingSubmissionRequest))(using any())
      }

      "must fail if a request cannot be created from the user answers" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SubmissionConnector].toInstance(mockSubmissionConnector),
            bind[UserAnswersService].toInstance(mockUserAnswersService)
          )
          .build()

        when(mockUserAnswersService.toAssumedReportingSubmissionRequest(any())).thenReturn(Left(NonEmptyChain.one(AssumingOperatorNamePage)))
        when(mockSubmissionConnector.submitAssumedReporting(any())(using any())).thenReturn(Future.successful(Done))

        running(application) {
          val request = FakeRequest(routes.CheckYourAnswersController.onSubmit(operatorId))
          route(application, request).value.failed.futureValue
        }

        verify(mockSubmissionConnector, never()).submitAssumedReporting(any())(using any())
      }
    }
  }
}
