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

package controllers.internal

import base.SpecBase
import connectors.SubmissionConnector
import models.submission.UploadSuccessRequest
import models.upscan.UpscanCallbackRequest.FailureReason.Rejected
import models.upscan.UpscanCallbackRequest.{ErrorDetails, UploadDetails}
import models.upscan.{UpscanCallbackRequest, UpscanJourney}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.UpscanJourneyRepository
import uk.gov.hmrc.http.StringContextOps

import java.time.Instant
import scala.concurrent.Future

class UpscanCallbackControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockUpscanJourneyRepository: UpscanJourneyRepository = mock[UpscanJourneyRepository]
  private val mockSubmissionConector: SubmissionConnector = mock[SubmissionConnector]

  private val now: Instant = Instant.now()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockSubmissionConector, mockUpscanJourneyRepository)
  }

  "callback" - {

    "when there is an UpscanJourney matching the reference in the request" - {

      "when given a successful response" - {

        "must indicate the success downstream" in {

          val application = applicationBuilder(userAnswers = None)
            .overrides(
              bind[UpscanJourneyRepository].toInstance(mockUpscanJourneyRepository),
              bind[SubmissionConnector].toInstance(mockSubmissionConector)
            )
            .build()

          val upscanJourney = UpscanJourney(
            dprsId = "dprsId",
            submissionId = "submissionId",
            uploadId = "uploadId",
            createdAt = now
          )

          val requestBody = UpscanCallbackRequest.Ready(
            reference = "uploadId",
            downloadUrl = url"http://example.com/test.xml",
            uploadDetails = UploadDetails(
              uploadTimestamp = now,
              checksum = "checksum",
              fileMimeType = "mimetype",
              fileName = "filename",
              size = 1337
            )
          )

          running(application) {
            val request = FakeRequest(routes.UpscanCallbackController.callback())
              .withBody(Json.toJson[UpscanCallbackRequest](requestBody))
            val result = route(application, request).value

            when(mockUpscanJourneyRepository.getByUploadId(any())).thenReturn(Future.successful(Some(upscanJourney)))
            when(mockSubmissionConector.uploadSuccess(any(), any())).thenReturn(Future.successful(Done))

            status(result) mustEqual OK

            verify(mockUpscanJourneyRepository).getByUploadId("uploadId")
            verify(mockSubmissionConector).uploadSuccess("submissionId", UploadSuccessRequest("dprsId", url"http://example.com/test.xml", "platformOperatorId"))
          }
        }
      }

      "when given a failed response" - {

        "must indicate the failure downstream" in {

          val application = applicationBuilder(userAnswers = None)
            .overrides(
              bind[UpscanJourneyRepository].toInstance(mockUpscanJourneyRepository),
              bind[SubmissionConnector].toInstance(mockSubmissionConector)
            )
            .build()

          val upscanJourney = UpscanJourney(
            dprsId = "dprsId",
            submissionId = "submissionId",
            uploadId = "uploadId",
            createdAt = now
          )

          val requestBody = UpscanCallbackRequest.Failed(
            reference = "uploadId",
            failureDetails = ErrorDetails(
              failureReason = Rejected,
              message = "some reason"
            )
          )

          running(application) {
            val request = FakeRequest(routes.UpscanCallbackController.callback())
              .withBody(Json.toJson[UpscanCallbackRequest](requestBody))
            val result = route(application, request).value

            when(mockUpscanJourneyRepository.getByUploadId(any())).thenReturn(Future.successful(Some(upscanJourney)))
            when(mockSubmissionConector.uploadFailed(any(), any(), any())).thenReturn(Future.successful(Done))

            status(result) mustEqual OK

            verify(mockUpscanJourneyRepository).getByUploadId("uploadId")
            verify(mockSubmissionConector).uploadFailed("dprsId", "submissionId", "some reason")
          }
        }
      }
    }

    "when there is no UpscanJourney matching the reference in the request" - {

      "must return OK" in {

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[UpscanJourneyRepository].toInstance(mockUpscanJourneyRepository),
            bind[SubmissionConnector].toInstance(mockSubmissionConector)
          )
          .build()

        val requestBody = UpscanCallbackRequest.Ready(
          reference = "uploadId",
          downloadUrl = url"http://example.com/test.xml",
          uploadDetails = UploadDetails(
            uploadTimestamp = now,
            checksum = "checksum",
            fileMimeType = "mimetype",
            fileName = "filename",
            size = 1337
          )
        )

        running(application) {
          val request = FakeRequest(routes.UpscanCallbackController.callback())
            .withBody(Json.toJson[UpscanCallbackRequest](requestBody))
          val result = route(application, request).value

          when(mockUpscanJourneyRepository.getByUploadId(any())).thenReturn(Future.successful(None))

          status(result) mustEqual OK

          verify(mockUpscanJourneyRepository).getByUploadId("uploadId")
          verify(mockSubmissionConector, never()).uploadSuccess(any(), any())
          verify(mockSubmissionConector, never()).uploadFailed(any(), any(), any())
        }
      }
    }
  }
}
