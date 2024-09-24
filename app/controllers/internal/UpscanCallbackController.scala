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

import connectors.SubmissionConnector
import models.submission.UploadSuccessRequest
import models.upscan.UpscanCallbackRequest
import play.api.Logging
import play.api.mvc.{Action, MessagesControllerComponents}
import repositories.UpscanJourneyRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpscanCallbackController @Inject() (
                                           mcc: MessagesControllerComponents,
                                           upscanJourneyRepository: UpscanJourneyRepository,
                                           submissionConnector: SubmissionConnector
                                         )(using ExecutionContext) extends FrontendController(mcc) with Logging {

  def callback(): Action[UpscanCallbackRequest] = Action.async(parse.json[UpscanCallbackRequest]) { implicit request =>
    upscanJourneyRepository.getByUploadId(request.body.reference).flatMap {
      _.map { journey =>
        request.body match {
          case callback: UpscanCallbackRequest.Ready =>

            val uploadSuccessRequest = UploadSuccessRequest(
              dprsId = journey.dprsId,
              downloadUrl = callback.downloadUrl,
              platformOperatorId = "platformOperatorId", // TODO get platform operator id from user's answers
              fileName = callback.uploadDetails.fileName,
              checksum = callback.uploadDetails.checksum,
              size = callback.uploadDetails.size
            )

            submissionConnector.uploadSuccess(journey.submissionId, uploadSuccessRequest)
              .map(_ => Ok)
          case failed: UpscanCallbackRequest.Failed =>
            submissionConnector.uploadFailed(journey.dprsId, journey.submissionId, failed.failureDetails.message)
              .map(_ => Ok)
        }
      }.getOrElse {
        logger.info("Upscan callback for a journey which doesn't exist")
        Future.successful(Ok)
      }
    }
  }
}
