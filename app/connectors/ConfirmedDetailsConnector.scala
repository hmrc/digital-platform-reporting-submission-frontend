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
import connectors.ConfirmedDetailsConnector.{GetConfirmedBusinessDetailsFailure, GetConfirmedContactDetailsFailure, GetConfirmedReportingNotificationsFailure, SaveConfirmedBusinessDetailsFailure, SaveConfirmedContactDetailsFailure, SaveConfirmedReportingNotificationsFailure}
import models.confirmed.{ConfirmedBusinessDetails, ConfirmedContactDetails, ConfirmedReportingNotifications}
import org.apache.pekko.Done
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.{HttpClientV2, given}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmedDetailsConnector @Inject()(appConfig: FrontendAppConfig, httpClient: HttpClientV2)
                                         (implicit ec: ExecutionContext) {

  def businessDetails(operatorId: String)(using HeaderCarrier): Future[Option[ConfirmedBusinessDetails]] =
    httpClient.get(url"${appConfig.digitalPlatformReportingUrl}/digital-platform-reporting/confirmed/business-details/$operatorId")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(Some(response.json.as[ConfirmedBusinessDetails]))
          case NOT_FOUND => Future.successful(None)
          case status => Future.failed(GetConfirmedBusinessDetailsFailure(status))
        }
      }

  def save(confirmedBusinessDetails: ConfirmedBusinessDetails)(using HeaderCarrier): Future[Done] =
    httpClient.post(url"${appConfig.digitalPlatformReportingUrl}/digital-platform-reporting/confirmed/business-details")
      .withBody(Json.toJson(confirmedBusinessDetails))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(Done)
          case status => Future.failed(SaveConfirmedBusinessDetailsFailure(status))
        }
      }

  def reportingNotifications(operatorId: String)(using HeaderCarrier): Future[Option[ConfirmedReportingNotifications]] =
    httpClient.get(url"${appConfig.digitalPlatformReportingUrl}/digital-platform-reporting/confirmed/reporting-notifications/$operatorId")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(Some(response.json.as[ConfirmedReportingNotifications]))
          case NOT_FOUND => Future.successful(None)
          case status => Future.failed(GetConfirmedReportingNotificationsFailure(status))
        }
      }

  def save(confirmedReportingNotifications: ConfirmedReportingNotifications)(using HeaderCarrier): Future[Done] =
    httpClient.post(url"${appConfig.digitalPlatformReportingUrl}/digital-platform-reporting/confirmed/reporting-notifications")
      .withBody(Json.toJson(confirmedReportingNotifications))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(Done)
          case status => Future.failed(SaveConfirmedReportingNotificationsFailure(status))
        }
      }

  def contactDetails()(using HeaderCarrier): Future[Option[ConfirmedContactDetails]] =
    httpClient.get(url"${appConfig.digitalPlatformReportingUrl}/digital-platform-reporting/confirmed/contact-details")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(Some(response.json.as[ConfirmedContactDetails]))
          case NOT_FOUND => Future.successful(None)
          case status => Future.failed(GetConfirmedContactDetailsFailure(status))
        }
      }

  def saveContactDetails()(using HeaderCarrier): Future[Done] =
    httpClient.post(url"${appConfig.digitalPlatformReportingUrl}/digital-platform-reporting/confirmed/contact-details")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(Done)
          case status => Future.failed(SaveConfirmedContactDetailsFailure(status))
        }
      }
}

object ConfirmedDetailsConnector {

  final case class GetConfirmedBusinessDetailsFailure(status: Int) extends Throwable {
    override def getMessage: String = s"Get confirmed business details failed with status: $status"
  }

  final case class SaveConfirmedBusinessDetailsFailure(status: Int) extends Throwable {
    override def getMessage: String = s"Save confirmed business details failed with status: $status"
  }

  final case class GetConfirmedReportingNotificationsFailure(status: Int) extends Throwable {
    override def getMessage: String = s"Get confirmed reporting notifications failed with status: $status"
  }

  final case class SaveConfirmedReportingNotificationsFailure(status: Int) extends Throwable {
    override def getMessage: String = s"Save confirmed reporting notifications failed with status: $status"
  }

  final case class GetConfirmedContactDetailsFailure(status: Int) extends Throwable {
    override def getMessage: String = s"Get confirmed contact details failed with status: $status"
  }

  final case class SaveConfirmedContactDetailsFailure(status: Int) extends Throwable {
    override def getMessage: String = s"Save confirmed contact details failed with status: $status"
  }
}