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

package controllers

import base.SpecBase
import connectors.SubmissionConnector
import models.submission.Submission
import models.submission.Submission.State.{UploadFailed, Uploading}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.UploadFailedView

import java.time.Instant
import scala.concurrent.Future

class UploadFailedControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSubmissionConnector: SubmissionConnector = mock[SubmissionConnector]

  private val now: Instant = Instant.now()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockSubmissionConnector)
  }

  "UploadFailed Controller" - {

    "onPageLoad" - {

      "when there is a submission in an upload failed state for the given id" - {

        "must return OK and the correct view for a GET" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          val submission = Submission(
            _id = "id",
            dprsId = "dprsId",
            platformOperatorId = "poid",
            state = UploadFailed("reason"),
            created = now,
            updated = now
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {
            val request = FakeRequest(GET, routes.UploadFailedController.onPageLoad("id").url)
            val result = route(application, request).value
            val view = application.injector.instanceOf[UploadFailedView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view()(request, messages(application)).toString
          }
        }
      }

      "when there is no submission for the given id" - {

        "must redirect to the journey recovery page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(None))

          running(application) {
            val request = FakeRequest(GET, routes.UploadFailedController.onPageLoad("id").url)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }

      "when the submission is in a ready state" - {

        "must redirect to the upload page" ignore {

        }
      }

      "when the submission is in an uploading state" - {

        "must redirect to the uploading page" ignore {

        }
      }

      "when the submission is in a validated state" - {

        "must redirect to the send file page" ignore {

        }
      }

      "when the submission is in a submitted state" - {

        "must redirect to the checking file page" ignore {

        }
      }

      "when the submission is in an approved state" - {

        "must redirect to the file passed page" ignore {

        }
      }

      "when the submission is in a rejected state" - {

        "must redirect to the file failed page" ignore {

        }
      }
    }
  }
}
