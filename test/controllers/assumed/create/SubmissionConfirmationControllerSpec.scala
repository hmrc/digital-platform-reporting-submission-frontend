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
import connectors.SubmissionConnector
import models.submission.Submission
import models.submission.Submission.State.{Approved, Ready, Rejected, Submitted, UploadFailed, Uploading, Validated}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.StringContextOps
import views.html.assumed.create.SubmissionConfirmationView

import java.time.{Instant, LocalDateTime, Year, ZoneOffset}
import scala.concurrent.Future

class SubmissionConfirmationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  private val mockSubmissionConnector: SubmissionConnector = mock[SubmissionConnector]

  private val now: Instant = Instant.now()
  private val submissionId: String = "id"

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockSubmissionConnector)
  }

  "SubmissionConfirmation Controller" - {

    "onPageLoad" - {

      "when there is a manual submission in a submitted state for the given id" - {

        "must return OK and the correct view" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          val updatedInstant = LocalDateTime.of(2024, 2, 1, 12, 30, 0, 0).toInstant(ZoneOffset.UTC)

          val submission = Submission(
            _id = submissionId,
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = operatorName,
            assumingOperatorName = Some("assumingOperatorName"),
            state = Submitted("test.xml", Year.of(2024)),
            created = now,
            updated = updatedInstant
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {
            val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, submissionId))
            val result = route(application, request).value
            val view = application.injector.instanceOf[SubmissionConfirmationView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(operatorId, "assumingOperatorName", operatorName, Year.of(2024))(request, messages(application)).toString
          }

          verify(mockSubmissionConnector).get(eqTo(submissionId))(using any())
        }
      }

      "when there is a manual submission in an approved state for the given id" - {

        "must return OK and the correct view" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          val updatedInstant = LocalDateTime.of(2024, 2, 1, 12, 30, 0, 0).toInstant(ZoneOffset.UTC)

          val submission = Submission(
            _id = submissionId,
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = operatorName,
            assumingOperatorName = Some("assumingOperatorName"),
            state = Approved("test.xml", Year.of(2024)),
            created = now,
            updated = updatedInstant
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {
            val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, submissionId))
            val result = route(application, request).value
            val view = application.injector.instanceOf[SubmissionConfirmationView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(operatorId, "assumingOperatorName", operatorName, Year.of(2024))(request, messages(application)).toString
          }

          verify(mockSubmissionConnector).get(eqTo(submissionId))(using any())
        }
      }

      "when there is a manual submission in a rejected state for the given id" - {

        "must return OK and the correct view" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          val updatedInstant = LocalDateTime.of(2024, 2, 1, 12, 30, 0, 0).toInstant(ZoneOffset.UTC)

          val submission = Submission(
            _id = submissionId,
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = operatorName,
            assumingOperatorName = Some("assumingOperatorName"),
            state = Rejected("test.xml", Year.of(2024)),
            created = now,
            updated = updatedInstant
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {
            val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, submissionId))
            val result = route(application, request).value
            val view = application.injector.instanceOf[SubmissionConfirmationView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(operatorId, "assumingOperatorName", operatorName, Year.of(2024))(request, messages(application)).toString
          }

          verify(mockSubmissionConnector).get(eqTo(submissionId))(using any())
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
            val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, submissionId))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }

          verify(mockSubmissionConnector).get(eqTo(submissionId))(using any())
        }
      }

      "when the submission is in a ready state" - {

        "must redirect to the journey recovery page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          val submission = Submission(
            _id = submissionId,
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = operatorName,
            assumingOperatorName = None,
            state = Ready,
            created = now,
            updated = now
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {
            val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, submissionId))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }

          verify(mockSubmissionConnector).get(eqTo(submissionId))(using any())
        }
      }

      "when the submission is in an uploading state" - {

        "must redirect to the journey recovery page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          val submission = Submission(
            _id = submissionId,
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = operatorName,
            assumingOperatorName = None,
            state = Uploading,
            created = now,
            updated = now
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {
            val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, submissionId))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }

          verify(mockSubmissionConnector).get(eqTo(submissionId))(using any())
        }
      }

      "when the submission is in an upload failed state" - {

        "must redirect to the journey recovery page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          val submission = Submission(
            _id = submissionId,
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = operatorName,
            assumingOperatorName = None,
            state = UploadFailed("some reason"),
            created = now,
            updated = now
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {
            val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, submissionId))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }

          verify(mockSubmissionConnector).get(eqTo(submissionId))(using any())
        }
      }

      "when the submission is in a validated state" - {

        "must redirect to the journey recovery page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          val submission = Submission(
            _id = submissionId,
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = operatorName,
            assumingOperatorName = None,
            state = Validated(
              downloadUrl = url"http://example.com/test.xml",
              reportingPeriod = Year.of(2024),
              fileName = "test.xml",
              checksum = "checksum",
              size = 1337L
            ),
            created = now,
            updated = now
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {
            val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, submissionId))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }

          verify(mockSubmissionConnector).get(eqTo(submissionId))(using any())
        }
      }

      "when there are no user answers" - {

        "must redirect to the journey recovery page" in {

          val application = applicationBuilder(userAnswers = None)
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, submissionId))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }

          verify(mockSubmissionConnector, never()).get(any())(using any())
        }
      }
    }
  }
}