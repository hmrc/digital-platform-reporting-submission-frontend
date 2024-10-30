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

package controllers.assumed.remove

import base.SpecBase
import connectors.AssumedReportingConnector
import controllers.assumed.routes as assumedRoutes
import forms.RemoveAssumedReportFormProvider
import models.submission.{SubmissionStatus, SubmissionSummary}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.AssumedReportSummariesQuery
import viewmodels.checkAnswers.assumed.remove.AssumedReportSummaryList
import views.html.assumed.remove.RemoveAssumedReportView

import java.time.{Instant, Year}
import scala.concurrent.Future

class RemoveAssumedReportControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val now = Instant.now()
  private val submission1 = SubmissionSummary("submissionId1", "file1", "operatorId", "operatorName", Year.of(2024), now, SubmissionStatus.Success, Some("assuming"), Some("caseId1"))
  private val submission2 = SubmissionSummary("submissionId2", "file2", "operatorId", "operatorName", Year.of(2025), now, SubmissionStatus.Success, Some("assuming"), Some("caseId2"))

  private val baseAnswers = emptyUserAnswers.set(AssumedReportSummariesQuery, Seq(submission1, submission2)).success.value

  private val form = RemoveAssumedReportFormProvider()()

  private val mockConnector = mock[AssumedReportingConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector)
    super.beforeEach()
  }

  "Remove Assumed Report Controller" - {

    "for a GET" - {

      "must return OK and the correct view for a known reporting period" in {

        val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

        running(application) {
          given Messages = messages(application)

          val request = FakeRequest(routes.RemoveAssumedReportController.onPageLoad("operatorId", Year.of(2024)))

          val view = application.injector.instanceOf[RemoveAssumedReportView]
          val summaryList = AssumedReportSummaryList.list(submission1)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, summaryList, "operatorId", Year.of(2024))(request, implicitly).toString
        }
      }

      "must return 404 for an unknown reporting period" in {

        val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

        running(application) {
          val request = FakeRequest(routes.RemoveAssumedReportController.onPageLoad("operatorId", Year.of(2000)))

          val result = route(application, request).value

          status(result) mustEqual NOT_FOUND
        }
      }
    }

    "for a POST" - {

      "must delete the submission and redirect to Assumed Report Removed when the answer is yes" in {

        when(mockConnector.delete(any(), any())(using any())).thenReturn(Future.successful(Done))

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AssumedReportingConnector].toInstance(mockConnector))
            .build()

        running(application) {
          val request =
            FakeRequest(routes.RemoveAssumedReportController.onSubmit("operatorId", Year.of(2024)))
              .withFormUrlEncodedBody("value" -> "true")

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.AssumedReportRemovedController.onPageLoad("operatorId", Year.of(2024)).url
          verify(mockConnector).delete(eqTo("operatorId"), eqTo(Year.of(2024)))(using any())
        }
      }

      "must not delete the submission and redirect to View Submissions when the answer is no" in {

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[AssumedReportingConnector].toInstance(mockConnector))
            .build()

        running(application) {
          val request =
            FakeRequest(routes.RemoveAssumedReportController.onSubmit("operatorId", Year.of(2024)))
              .withFormUrlEncodedBody("value" -> "false")

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual assumedRoutes.ViewAssumedReportsController.onPageLoad().url
          verify(mockConnector, never()).delete(any(), any())(using any())
        }
      }

      "must return Bad Request and display the view when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

        running(application) {
          given Messages = messages(application)

          val request =
            FakeRequest(routes.RemoveAssumedReportController.onSubmit("operatorId", Year.of(2024)))
              .withFormUrlEncodedBody("value" -> "")

          val view = application.injector.instanceOf[RemoveAssumedReportView]
          val summaryList = AssumedReportSummaryList.list(submission1)
          val boundForm = form.bind(Map("value" -> ""))

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, summaryList, "operatorId", Year.of(2024))(request, implicitly).toString
        }
      }
    }
  }
}
