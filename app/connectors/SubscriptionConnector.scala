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
import connectors.SubscriptionConnector.GetSubscriptionFailure
import models.subscription.SubscriptionInfo
import play.api.Configuration
import play.api.http.Status.OK
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionConnector @Inject() (
                                        configuration: Configuration,
                                        httpClient: HttpClientV2
                                      )(implicit ec: ExecutionContext) {

  private val digitalPlatformReporting: Service = configuration.get[Service]("microservice.services.digital-platform-reporting")

  def getSubscription(implicit hc: HeaderCarrier): Future[SubscriptionInfo] =
    httpClient.get(url"$digitalPlatformReporting/digital-platform-reporting/subscribe")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK     => Future.successful(response.json.as[SubscriptionInfo])
          case status => Future.failed(GetSubscriptionFailure(status))
        }
      }
}

object SubscriptionConnector {

  final case class GetSubscriptionFailure(status: Int) extends Throwable {
    override def getMessage: String = s"Get subscription failed with status: $status"
  }
}
