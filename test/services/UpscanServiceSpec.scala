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

package services

import connectors.UpscanInitiateConnector
import models.upscan.UpscanInitiateResponse.UploadRequest
import models.upscan.{UpscanInitiateRequest, UpscanInitiateResponse}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import repositories.UpscanJourneyRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class UpscanServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach {

  private val mockUpscanInitiateConnector: UpscanInitiateConnector = mock[UpscanInitiateConnector]
  private val mockUpscanJourneyRepository: UpscanJourneyRepository = mock[UpscanJourneyRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockUpscanJourneyRepository, mockUpscanInitiateConnector)
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.upscan-initiate.redirect-base" -> "http://example.com",
        "microservice.services.upscan-initiate.minimum-file-size" -> "1b",
        "microservice.services.upscan-initiate.maximum-file-size" -> "1000kB",
        "microservice.services.digital-platform-reporting-submission-frontend.protocol" -> "http",
        "microservice.services.digital-platform-reporting-submission-frontend.host" -> "localhost",
        "microservice.services.digital-platform-reporting-submission-frontend.port" -> 20007,
      )
      .overrides(
        bind[UpscanInitiateConnector].toInstance(mockUpscanInitiateConnector),
        bind[UpscanJourneyRepository].toInstance(mockUpscanJourneyRepository)
      ).build()

  private val service = app.injector.instanceOf[UpscanService]
  private val hc: HeaderCarrier = HeaderCarrier()
  private val operatorId: String = "operatorId"

  "initiate" - {

    "must initiate a journey with upscan and save that to mongo" in {

      val uploadId = "uploadId"
      val dprsId = "dprsId"
      val submissionId = "submissionId"

      val expectedRequest = UpscanInitiateRequest(
        callbackUrl = "http://localhost:20007/digital-platform-reporting-stubs/internal/upscan/callback",
        successRedirect = "http://example.com/digital-platform-reporting/submission/operatorId/submissionId/uploading-redirect",
        errorRedirect = "http://example.com/digital-platform-reporting/submission/operatorId/submissionId/upload-failed-redirect",
        minimumFileSize = 1,
        maximumFileSize = 1000000L
      )

      val expectedUploadRequest = UploadRequest(
        href = "href",
        fields = Map("a" -> "b")
      )

      val response = UpscanInitiateResponse(
        reference = uploadId,
        uploadRequest = expectedUploadRequest
      )

      when(mockUpscanInitiateConnector.initiate(any())(using any())).thenReturn(Future.successful(response))
      when(mockUpscanJourneyRepository.initiate(any(), any(), any())).thenReturn(Future.successful(Done))

      val result = service.initiate(operatorId, dprsId, submissionId)(using hc).futureValue
      result mustEqual expectedUploadRequest

      verify(mockUpscanInitiateConnector).initiate(eqTo(expectedRequest))(using any())
      verify(mockUpscanJourneyRepository).initiate(uploadId, dprsId, submissionId)
    }

    "must fail if the connector fails" in {

      val dprsId = "dprsId"
      val submissionId = "submissionId"

      when(mockUpscanInitiateConnector.initiate(any())(using any())).thenReturn(Future.failed(new RuntimeException()))

      service.initiate(operatorId, dprsId, submissionId)(using hc).failed.futureValue

      verify(mockUpscanJourneyRepository, never()).initiate(any(), any(), any())
    }

    "must fail if mongo fails" in {

      val uploadId = "uploadId"
      val dprsId = "dprsId"
      val submissionId = "submissionId"

      val expectedUploadRequest = UploadRequest(
        href = "href",
        fields = Map("a" -> "b")
      )

      val response = UpscanInitiateResponse(
        reference = uploadId,
        uploadRequest = expectedUploadRequest
      )

      when(mockUpscanInitiateConnector.initiate(any())(using any())).thenReturn(Future.successful(response))
      when(mockUpscanJourneyRepository.initiate(any(), any(), any())).thenReturn(Future.failed(new RuntimeException()))

      service.initiate(operatorId, dprsId, submissionId)(using hc).failed.futureValue
    }
  }
}
