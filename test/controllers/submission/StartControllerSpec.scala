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
import models.submission.Submission
import models.submission.Submission.State.Ready
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.submission.StartPageView

import java.time.Instant
import scala.concurrent.Future

class StartControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSubmissionConnector: SubmissionConnector = mock[SubmissionConnector]

  private val now: Instant = Instant.now()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockSubmissionConnector)
  }

  "StartPage Controller" - {

    "onPageLoad" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(routes.StartController.onPageLoad())
          val result = route(application, request).value
          val view = application.injector.instanceOf[StartPageView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view()(request, messages(application)).toString
        }
      }
    }

    "onSubmit" - {

      "must create a new submission and redirect to the upload page for that submission" in {

        val submissionId = "id"
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SubmissionConnector].toInstance(mockSubmissionConnector)
          )
          .build()

        val submission = Submission(
          _id = "id",
          dprsId = "dprsId",
          state = Ready,
          created = now,
          updated = now
        )

        when(mockSubmissionConnector.start(any())(using any())).thenReturn(Future.successful(submission))

        running(application) {
          val request = FakeRequest(routes.StartController.onSubmit())
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UploadController.onPageLoad(submissionId).url
        }

        verify(mockSubmissionConnector).start(eqTo(None))(using any())
      }

      "must fail when the call to create a new submission fails" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SubmissionConnector].toInstance(mockSubmissionConnector)
          )
          .build()

        when(mockSubmissionConnector.start(any())(using any())).thenReturn(Future.failed(new RuntimeException()))

        running(application) {
          val request = FakeRequest(routes.StartController.onSubmit())
          route(application, request).value.failed.futureValue
        }
      }
    }
  }
}
