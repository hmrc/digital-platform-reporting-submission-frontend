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

import config.FrontendAppConfig
import connectors.AssumedReportingConnector.*
import connectors.PendingEnrolmentConnector.GetRecentSubmissionFailure
import models.recentsubmissions.{RecentSubmissionDetails, RecentSubmissionRequest}
import org.apache.pekko.Done
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.{HttpClientV2, given}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RecentSubmissionsConnector @Inject()(appConfig: FrontendAppConfig, httpClient: HttpClientV2)
                                          (implicit ec: ExecutionContext) {

  def save(request: RecentSubmissionRequest)(implicit hc: HeaderCarrier): Future[Done] = {
    httpClient.post(url"${appConfig.digitalPlatformReportingUrl}/digital-platform-reporting/recent-submissions")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => Done
        }
      }
  }

  def getRecentSubmission(operatorId: String)(implicit hc: HeaderCarrier): Future[Option[RecentSubmissionDetails]] =
    httpClient.get(url"${appConfig.digitalPlatformReportingUrl}/digital-platform-reporting/recent-submissions/$operatorId")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(Some(response.json.as[RecentSubmissionDetails]))
          case NOT_FOUND => Future.successful(None)
          case status => Future.failed(GetRecentSubmissionFailure(status))
        }
      }
}

object PendingEnrolmentConnector {

  final case class GetRecentSubmissionFailure(status: Int) extends Throwable {
    override def getMessage: String = s"Get recent submission failed with status: $status"
  }
}