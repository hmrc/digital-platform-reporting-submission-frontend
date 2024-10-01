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

import config.Service
import connectors.UpscanInitiateConnector
import models.upscan.{UpscanInitiateRequest, UpscanInitiateResponse}
import models.upscan.UpscanInitiateResponse.UploadRequest
import play.api.Configuration
import repositories.UpscanJourneyRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpscanService @Inject() (
                                configuration: Configuration,
                                upscanConnector: UpscanInitiateConnector,
                                upscanJourneyRepository: UpscanJourneyRepository
                              )(using ExecutionContext) {

  private val minFileSize: Long = configuration.underlying.getBytes("microservice.services.upscan-initiate.minimum-file-size")
  private val maxFileSize: Long = configuration.underlying.getBytes("microservice.services.upscan-initiate.maximum-file-size")

  private val dprsSubmissionFrontend: Service = configuration.get[Service]("microservice.services.digital-platform-reporting-submission-frontend")
  private lazy val callbackRoute: String =
    s"$dprsSubmissionFrontend${controllers.internal.routes.UpscanCallbackController.callback().path()}"

  private val redirectBase: String = configuration.get[String]("microservice.services.upscan-initiate.redirect-base")
  private def successRedirectRoute(submissionId: String): String =
    s"$redirectBase${controllers.submission.routes.UploadingController.onRedirect(submissionId).path()}"
  private def failureRedirectRoute(submissionId: String): String =
    s"$redirectBase${controllers.submission.routes.UploadFailedController.onRedirect(submissionId).path()}"


  def initiate(dprsId: String, submissionId: String)(using HeaderCarrier): Future[UploadRequest] = {

    val upscanRequest = UpscanInitiateRequest(
      callbackUrl = callbackRoute,
      successRedirect = successRedirectRoute(submissionId),
      errorRedirect = failureRedirectRoute(submissionId),
      minimumFileSize = minFileSize,
      maximumFileSize = maxFileSize
    )

    for {
      response <- upscanConnector.initiate(upscanRequest)
      _        <- upscanJourneyRepository.initiate(response.reference, dprsId, submissionId)
    } yield response.uploadRequest
  }
}
