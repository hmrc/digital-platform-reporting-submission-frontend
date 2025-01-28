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
import connectors.SubmissionConnector
import models.submission.{SubmissionStatus, SubmissionSummary}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.submission.ViewUndeliveredSubmissionsView

import java.time.temporal.ChronoUnit
import java.time.{Instant, Year}
import scala.concurrent.Future

class ViewUndeliveredSubmissionsControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSubmissionConnector = mock[SubmissionConnector]
  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val thisYear = Year.now

  override def beforeEach(): Unit = {
    Mockito.reset(mockSubmissionConnector)
    super.beforeEach()
  }

  "ViewUndeliveredSubmissions Controller" - {

    "must return OK and the correct view for a GET" in {

      val submissionSummary = SubmissionSummary(
        submissionId = "submissionId",
        fileName = "filename",
        operatorId = Some("operatorId"),
        operatorName = Some("operatorName"),
        reportingPeriod = Some(thisYear),
        submissionDateTime = instant,
        submissionStatus = SubmissionStatus.Pending,
        assumingReporterName = None,
        submissionCaseId = None,
        localDataExists = true
      )
      
      when(mockSubmissionConnector.listUndeliveredSubmissions(using any())).thenReturn(Future.successful(Seq(submissionSummary)))
      
      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[SubmissionConnector].toInstance(mockSubmissionConnector))
        .build()

      running(application) {
        val request = FakeRequest(routes.ViewUndeliveredSubmissionsController.onPageLoad())

        val result = route(application, request).value

        implicit val msgs: Messages = messages(application)
        val view = application.injector.instanceOf[ViewUndeliveredSubmissionsView]
        
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(Seq(submissionSummary))(request, implicitly).toString
      }
    }
  }
}
