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
import connectors.SubmissionConnector.*
import models.submission.*
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{JsonFraming, Source}
import org.apache.pekko.{Done, NotUsed}
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.{CONFLICT, CREATED, NOT_FOUND, NO_CONTENT, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.{HttpClientV2, given}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionConnector @Inject() (
                                      httpClient: HttpClientV2,
                                      configuration: Configuration
                                    )(using ExecutionContext, Materializer) {

  private val digitalPlatformReportingService: Service =
    configuration.get[Service]("microservice.services.digital-platform-reporting")

  def start(operatorId: String, operatorName: String, id: Option[String])(using HeaderCarrier): Future[Submission] =
    httpClient.put(url"$digitalPlatformReportingService/digital-platform-reporting/submission/start")
      .transform(_.withQueryStringParameters(Seq(id.map(id => "id" -> id)).flatten*))
      .withBody(Json.toJson(StartSubmissionRequest(operatorId, operatorName)))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK | CREATED =>
            Future.successful(response.json.as[Submission])
          case _ =>
            Future.failed(StartFailure(id))
        }
      }

  def get(id: String)(using HeaderCarrier): Future[Option[Submission]] =
    httpClient.get(url"$digitalPlatformReportingService/digital-platform-reporting/submission/$id")
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
    httpClient.post(url"$digitalPlatformReportingService/digital-platform-reporting/submission/$id/start-upload")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK | CONFLICT =>
            Future.successful(Done)
          case _ =>
            Future.failed(StartUploadFailure(id))
        }
      }

  def uploadSuccess(id: String, request: UploadSuccessRequest): Future[Done] =
    httpClient.post(url"$digitalPlatformReportingService/digital-platform-reporting/submission/$id/upload-success")(HeaderCarrier())
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            Future.successful(Done)
          case _ =>
            Future.failed(UploadSuccessFailure(id))
        }
      }

  def uploadFailed(dprsId: String, id: String, reason: String): Future[Done] =
    httpClient.post(url"$digitalPlatformReportingService/digital-platform-reporting/submission/$id/upload-failed")(HeaderCarrier())
      .withBody(Json.toJson(UploadFailedRequest(dprsId, reason)))
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
    httpClient.post(url"$digitalPlatformReportingService/digital-platform-reporting/submission/$id/submit")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            Future.successful(Done)
          case _ =>
            Future.failed(SubmitFailure(id))
        }
      }

  def getErrors(id: String)(using HeaderCarrier): Future[Source[CadxValidationError, NotUsed]] =
    httpClient.get(url"$digitalPlatformReportingService/digital-platform-reporting/submission/$id/errors")
      .stream[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            Future.successful {
              response.bodyAsSource
                .via(JsonFraming.objectScanner(10000))
                .map(bytes => Json.parse(bytes.utf8String).as[CadxValidationError])
                .mapMaterializedValue(_ => NotUsed)
            }
          case _ =>
            Future.failed(GetErrorsFailure(id, response.status))
        }
      }
      
  def list(request: ViewSubmissionsRequest)(using HeaderCarrier): Future[Option[SubmissionsSummary]] =
    httpClient.post(url"$digitalPlatformReportingService/digital-platform-reporting/submission/list")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK        => Future.successful(Some(response.json.as[SubmissionsSummary]))
          case NOT_FOUND => Future.successful(None)
          case _         => Future.failed(ViewFailure)
        }
      }

  def submitAssumedReporting(request: AssumedReportingSubmissionRequest)(using HeaderCarrier): Future[Submission] =
    httpClient.post(url"$digitalPlatformReportingService/digital-platform-reporting/submission/assumed/submit")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(response.json.as[Submission])
          case _  => Future.failed(SubmitAssumedReportingFailure)
        }
      }
}

object SubmissionConnector {

  final case class StartFailure(id: Option[String]) extends Throwable
  final case class GetFailure(id: String) extends Throwable
  final case class StartUploadFailure(id: String) extends Throwable
  final case class UploadSuccessFailure(id: String) extends Throwable
  final case class UploadFailedFailure(id: String) extends Throwable
  final case class SubmitFailure(id: String) extends Throwable
  final case class GetErrorsFailure(id: String, status: Int) extends Throwable
  case object ViewFailure extends Throwable
  case object SubmitAssumedReportingFailure extends Throwable
}