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
import connectors.SubmissionConnector
import models.submission.{SubmissionStatus, SubmissionSummary, SubmissionsSummary}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.assumed.ViewAssumedReportsView

import java.time.Instant
import scala.concurrent.Future

class ViewAssumedReportsControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[SubmissionConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector)
    super.beforeEach()
  }
  
  private val now = Instant.now
  
  "ViewAssumedReports Controller" - {

    "must return OK and the correct view for a GET when assumed reports exist" in {

      val submissionSummary = SubmissionSummary(
        submissionId = "submissionId",
        fileName = "filename",
        operatorId = "operatorId",
        operatorName = "operatorName",
        reportingPeriod = "2024",
        submissionDateTime = now,
        submissionStatus = SubmissionStatus.Success,
        assumingReporterName = Some("name")
      )
      val summary = SubmissionsSummary(Seq(submissionSummary), Nil)

      when(mockConnector.list(any())(using any())).thenReturn(Future.successful(Some(summary)))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[SubmissionConnector].toInstance(mockConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(routes.ViewAssumedReportsController.onPageLoad())

        val result = route(application, request).value

        val view = application.injector.instanceOf[ViewAssumedReportsView]
        implicit val msgs: Messages = messages(application)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(Some(summary))(request, implicitly).toString
      }
    }

    "must return OK and the correct view for a GET when no assumed reports exist" in {

      when(mockConnector.list(any())(using any())).thenReturn(Future.successful(None))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[SubmissionConnector].toInstance(mockConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(routes.ViewAssumedReportsController.onPageLoad())

        val result = route(application, request).value

        val view = application.injector.instanceOf[ViewAssumedReportsView]
        implicit val msgs: Messages = messages(application)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(None)(request, implicitly).toString
      }
    }
  }
}
