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
import connectors.AssumedReportingConnector.*
import models.submission.*
import org.apache.pekko.stream.Materializer
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.{HttpClientV2, given}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import java.time.Year
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AssumedReportingConnector @Inject() (
                                            httpClient: HttpClientV2,
                                            configuration: Configuration
                                          )(using ExecutionContext, Materializer) {

  private val digitalPlatformReportingService: Service =
    configuration.get[Service]("microservice.services.digital-platform-reporting")

  def submit(request: AssumedReportingSubmissionRequest)(using HeaderCarrier): Future[Submission] =
    httpClient.post(url"$digitalPlatformReportingService/digital-platform-reporting/submission/assumed/submit")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(response.json.as[Submission])
          case _ => Future.failed(SubmitAssumedReportingFailure)
        }
      }

  def get(operatorId: String, reportingPeriod: Year)(using HeaderCarrier): Future[Option[AssumedReportingSubmission]] =
    httpClient.get(url"$digitalPlatformReportingService/digital-platform-reporting/submission/assumed/$operatorId/$reportingPeriod")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK        => Future.successful(Some(response.json.as[AssumedReportingSubmission]))
          case NOT_FOUND => Future.successful(None)
          case _         => Future.failed(GetAssumedReportFailure)
        }
      }

  def delete(operatorId: String, reportingPeriod: Year)(using HeaderCarrier): Future[Done] =
    httpClient.delete(url"$digitalPlatformReportingService/digital-platform-reporting/submission/assumed/$operatorId/$reportingPeriod")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK     => Future.successful(Done)
          case status => Future.failed(DeleteAssumedReportFailure(status))
        }
      }

  def list(using HeaderCarrier): Future[Seq[AssumedReportingSubmissionSummary]] =
    httpClient.post(url"$digitalPlatformReportingService/digital-platform-reporting/submission/assumed")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(response.json.as[Seq[AssumedReportingSubmissionSummary]])
          case _  => Future.failed(ListAssumedReportsFailure)
        }
      }
}

object AssumedReportingConnector {
  
  case object SubmitAssumedReportingFailure extends Throwable
  case object GetAssumedReportFailure extends Throwable
  final case class DeleteAssumedReportFailure(status: Int) extends Throwable {
    override def getMessage: String = s"Unexpected status code $status received when trying to delete an assumed report"
  }
  case object ListAssumedReportsFailure extends Throwable
}