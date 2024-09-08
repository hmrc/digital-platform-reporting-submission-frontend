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

package connectors

import config.Service
import connectors.SubmissionConnector.{GetFailure, StartFailure, StartUploadFailure, SubmitFailure, UploadFailedFailure, UploadSuccessFailure}
import models.submission.{StartSubmissionRequest, Submission, UploadFailedRequest}
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.{CREATED, NOT_FOUND, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits.*

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionConnector @Inject() (
                                      httpClient: HttpClientV2,
                                      configuration: Configuration
                                    )(using ExecutionContext) {

  private val digitalPlatformReportingService: Service =
    configuration.get[Service]("microservice.services.digital-platform-reporting")

  // TODO remove POID from here, add it to the relevant states
  def start(platformOperatorId: String, id: Option[String])(using HeaderCarrier): Future[Submission] =
    httpClient.put(url"$digitalPlatformReportingService/submission/start")
      .transform(_.withQueryStringParameters(Seq(id.map(id => "id" -> id)).flatten*))
      .withBody(Json.toJson(StartSubmissionRequest(platformOperatorId)))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK | CREATED =>
            Future.successful(response.json.as[Submission])
          case _ =>
            Future.failed(StartFailure(platformOperatorId, id))
        }
      }

  def get(id: String)(using HeaderCarrier): Future[Option[Submission]] =
    httpClient.get(url"$digitalPlatformReportingService/submission/$id")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            Future.successful(Some(response.json.as[Submission]))
          case NOT_FOUND =>
            Future.successful(None)
          case _ =>
            Future.failed(GetFailure(id))
        }
      }

  def startUpload(id: String)(using HeaderCarrier): Future[Done] =
    httpClient.post(url"$digitalPlatformReportingService/submission/$id/start-upload")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            Future.successful(Done)
          case _ =>
            Future.failed(StartUploadFailure(id))
        }
      }

  def uploadSuccess(id: String)(using HeaderCarrier): Future[Done] =
    httpClient.post(url"$digitalPlatformReportingService/submission/$id/upload-success")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            Future.successful(Done)
          case _ =>
            Future.failed(UploadSuccessFailure(id))
        }
      }

  def uploadFailed(id: String, reason: String)(using HeaderCarrier): Future[Done] =
    httpClient.post(url"$digitalPlatformReportingService/submission/$id/upload-failed")
      .withBody(Json.toJson(UploadFailedRequest(reason)))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            Future.successful(Done)
          case _ =>
            Future.failed(UploadFailedFailure(id))
        }
      }

  def submit(id: String)(using HeaderCarrier): Future[Done] =
    httpClient.post(url"$digitalPlatformReportingService/submission/$id/submit")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            Future.successful(Done)
          case _ =>
            Future.failed(SubmitFailure(id))
        }
      }
}

object SubmissionConnector {

  final case class StartFailure(platformOperatorId: String, id: Option[String]) extends Throwable
  final case class GetFailure(id: String) extends Throwable
  final case class StartUploadFailure(id: String) extends Throwable
  final case class UploadSuccessFailure(id: String) extends Throwable
  final case class UploadFailedFailure(id: String) extends Throwable
  final case class SubmitFailure(id: String) extends Throwable
}