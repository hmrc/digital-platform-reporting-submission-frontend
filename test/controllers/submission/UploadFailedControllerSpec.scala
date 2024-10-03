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
import models.submission.Submission.State.{Approved, Ready, Rejected, Submitted, UploadFailed, Uploading, Validated}
import models.upscan.UpscanInitiateResponse.UploadRequest
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, verify, when}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.UpscanService
import uk.gov.hmrc.http.StringContextOps
import views.html.submission.UploadFailedView

import java.time.{Instant, Year}
import scala.concurrent.Future

class UploadFailedControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSubmissionConnector: SubmissionConnector = mock[SubmissionConnector]
  private val mockUpscanService: UpscanService = mock[UpscanService]

  private val now: Instant = Instant.now()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockSubmissionConnector, mockUpscanService)
  }

  private val readyGen: Gen[Ready.type] = Gen.const(Ready)
  private val uploadingGen: Gen[Uploading.type] = Gen.const(Uploading)
  private val uploadFailedGen: Gen[UploadFailed] = Gen.asciiPrintableStr.map(UploadFailed.apply)

  "UploadFailed Controller" - {

    "onPageLoad" - {

      "when there is a submission in an upload failed state for the given id" - {

        "must return OK and the correct view for a GET" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector),
              bind[UpscanService].toInstance(mockUpscanService)
            )
            .build()

          val submission = Submission(
            _id = "id",
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = "operatorName",
            state = UploadFailed("reason"),
            created = now,
            updated = now
          )

          val uploadRequest = UploadRequest(
            href = "href",
            fields = Map.empty
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))
          when(mockUpscanService.initiate(any(), any(), any())(using any())).thenReturn(Future.successful(uploadRequest))

          running(application) {
            val request = FakeRequest(GET, routes.UploadFailedController.onPageLoad(operatorId, "id").url)
            val result = route(application, request).value
            val view = application.injector.instanceOf[UploadFailedView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(uploadRequest, "reason")(request, messages(application)).toString
          }

          verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          verify(mockUpscanService).initiate(eqTo(operatorId), eqTo("dprsId"), eqTo("id"))(using any())
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
            val request = FakeRequest(GET, routes.UploadFailedController.onPageLoad(operatorId, "id").url)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }

      "when the submission is in a ready state" - {

        "must redirect to the upload page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector),
              bind[UpscanService].toInstance(mockUpscanService)
            )
            .build()

          val submission = Submission(
            _id = "id",
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = "operatorName",
            state = Ready,
            created = now,
            updated = now
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {
            val request = FakeRequest(GET, routes.UploadFailedController.onPageLoad(operatorId, "id").url)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.UploadController.onPageLoad(operatorId, "id").url
          }

          verify(mockUpscanService, never()).initiate(any(), any(), any())(using any())
        }
      }

      "when the submission is in an uploading state" - {

        "must redirect to the uploading page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector),
              bind[UpscanService].toInstance(mockUpscanService)
            )
            .build()

          val submission = Submission(
            _id = "id",
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = "operatorName",
            state = Uploading,
            created = now,
            updated = now
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {
            val request = FakeRequest(GET, routes.UploadFailedController.onPageLoad(operatorId, "id").url)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.UploadingController.onPageLoad(operatorId, "id").url
          }

          verify(mockUpscanService, never()).initiate(any(), any(), any())(using any())
        }
      }

      "when the submission is in a validated state" - {

        "must redirect to the send file page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector),
              bind[UpscanService].toInstance(mockUpscanService)
            )
            .build()

          val submission = Submission(
            _id = "id",
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = "operatorName",
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
            val request = FakeRequest(GET, routes.UploadFailedController.onPageLoad(operatorId, "id").url)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.SendFileController.onPageLoad(operatorId, "id").url
          }

          verify(mockUpscanService, never()).initiate(any(), any(), any())(using any())
        }
      }

      "when the submission is in a submitted state" - {

        "must redirect to the checking file page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector),
              bind[UpscanService].toInstance(mockUpscanService)
            )
            .build()

          val submission = Submission(
            _id = "id",
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = "operatorName",
            state = Submitted("test.xml", Year.of(2024)),
            created = now,
            updated = now
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {
            val request = FakeRequest(GET, routes.UploadFailedController.onPageLoad(operatorId, "id").url)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.CheckFileController.onPageLoad(operatorId, "id").url
          }

          verify(mockUpscanService, never()).initiate(any(), any(), any())(using any())
        }
      }

      "when the submission is in an approved state" - {

        "must redirect to the file passed page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector),
              bind[UpscanService].toInstance(mockUpscanService)
            )
            .build()

          val submission = Submission(
            _id = "id",
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = "operatorName",
            state = Approved("test.xml", Year.of(2024)),
            created = now,
            updated = now
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {
            val request = FakeRequest(GET, routes.UploadFailedController.onPageLoad(operatorId, "id").url)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.SubmissionConfirmationController.onPageLoad(operatorId, "id").url
          }

          verify(mockUpscanService, never()).initiate(any(), any(), any())(using any())
        }
      }

      "when the submission is in a rejected state" - {

        "must redirect to the file failed page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector),
              bind[UpscanService].toInstance(mockUpscanService)
            )
            .build()

          val submission = Submission(
            _id = "id",
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = "operatorName",
            state = Rejected("test.xml", Year.of(2024)),
            created = now,
            updated = now
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))

          running(application) {
            val request = FakeRequest(GET, routes.UploadFailedController.onPageLoad(operatorId, "id").url)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.FileErrorsController.onPageLoad(operatorId, "id").url
          }

          verify(mockUpscanService, never()).initiate(any(), any(), any())(using any())
        }
      }

      "when there are no user answers" - {

        "must redirect to Journey Recovery" in {

          val application = applicationBuilder(userAnswers = None)
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(routes.UploadFailedController.onPageLoad(operatorId, "id"))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }

          verify(mockSubmissionConnector, never()).get(any())(using any())
        }
      }
    }

    "onRedirect" - {

      "when there is a submission in a ready, uploading, or upload failed state" - {

        "when errorCode is given" - {

          "must update update the state of the submission and redirect to the uploading failed page" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector)
              )
              .build()

            val state = Gen.oneOf(readyGen, uploadingGen, uploadFailedGen).sample.value
            val submission = Submission(
              _id = "id",
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = "operatorName",
              state = state,
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))
            when(mockSubmissionConnector.uploadFailed(any(), any(), any())).thenReturn(Future.successful(Done))

            running(application) {
              val request = FakeRequest(routes.UploadFailedController.onRedirect(operatorId, "id", Some("some reason")))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.UploadFailedController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
            verify(mockSubmissionConnector).uploadFailed("dprsId", "id", "some reason")
          }
        }

        "when no errorCode is given" - {

          "must update update the state of the submission and redirect to the uploading failed page" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                bind[SubmissionConnector].toInstance(mockSubmissionConnector)
              )
              .build()

            val state = Gen.oneOf(readyGen, uploadingGen, uploadFailedGen).sample.value
            val submission = Submission(
              _id = "id",
              dprsId = "dprsId",
              operatorId = "operatorId",
              operatorName = "operatorName",
              state = state,
              created = now,
              updated = now
            )

            when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))
            when(mockSubmissionConnector.uploadFailed(any(), any(), any())).thenReturn(Future.successful(Done))

            running(application) {
              val request = FakeRequest(routes.UploadFailedController.onRedirect(operatorId, "id", None))
              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.UploadFailedController.onPageLoad(operatorId, "id").url
            }

            verify(mockSubmissionConnector).get(eqTo("id"))(using any())
            verify(mockSubmissionConnector).uploadFailed("dprsId", "id", "Unknown")
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
            val request = FakeRequest(routes.UploadFailedController.onRedirect(operatorId, "id", None))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }

      "when the submission is in a validated state" - {

        "must redirect to the send file page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          val submission = Submission(
            _id = "id",
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = "operatorName",
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
          when(mockSubmissionConnector.uploadFailed(any(), any(), any())).thenReturn(Future.successful(Done))

          running(application) {
            val request = FakeRequest(routes.UploadFailedController.onRedirect(operatorId, "id", None))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.SendFileController.onPageLoad(operatorId, "id").url
          }

          verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          verify(mockSubmissionConnector, never()).uploadFailed(any(), any(), any())
        }
      }

      "when the submission is in a submitted state" - {

        "must redirect to the checking file page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          val submission = Submission(
            _id = "id",
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = "operatorName",
            state = Submitted("test.xml", Year.of(2024)),
            created = now,
            updated = now
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))
          when(mockSubmissionConnector.uploadFailed(any(), any(), any())).thenReturn(Future.successful(Done))

          running(application) {
            val request = FakeRequest(routes.UploadFailedController.onRedirect(operatorId, "id", None))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.CheckFileController.onPageLoad(operatorId, "id").url
          }

          verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          verify(mockSubmissionConnector, never()).uploadFailed(any(), any(), any())
        }
      }

      "when the submission is in an approved state" - {

        "must redirect to the file passed page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          val submission = Submission(
            _id = "id",
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = "operatorName",
            state = Approved("test.xml", Year.of(2024)),
            created = now,
            updated = now
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))
          when(mockSubmissionConnector.uploadFailed(any(), any(), any())).thenReturn(Future.successful(Done))

          running(application) {
            val request = FakeRequest(routes.UploadFailedController.onRedirect(operatorId, "id", None))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.SubmissionConfirmationController.onPageLoad(operatorId, "id").url
          }

          verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          verify(mockSubmissionConnector, never()).uploadFailed(any(), any(), any())
        }
      }

      "when the submission is in a rejected state" - {

        "must redirect to the file failed page" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          val submission = Submission(
            _id = "id",
            dprsId = "dprsId",
            operatorId = "operatorId",
            operatorName = "operatorName",
            state = Rejected("test.xml", Year.of(2024)),
            created = now,
            updated = now
          )

          when(mockSubmissionConnector.get(any())(using any())).thenReturn(Future.successful(Some(submission)))
          when(mockSubmissionConnector.uploadFailed(any(), any(), any())).thenReturn(Future.successful(Done))

          running(application) {
            val request = FakeRequest(routes.UploadFailedController.onRedirect(operatorId, "id", None))
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.FileErrorsController.onPageLoad(operatorId, "id").url
          }

          verify(mockSubmissionConnector).get(eqTo("id"))(using any())
          verify(mockSubmissionConnector, never()).uploadFailed(any(), any(), any())
        }
      }

      "when there are no user answers" - {

        "must redirect to Journey Recovery" in {

          val application = applicationBuilder(userAnswers = None)
            .overrides(
              bind[SubmissionConnector].toInstance(mockSubmissionConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(routes.UploadFailedController.onRedirect(operatorId, "id"))
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
