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

import connectors.ConfirmedDetailsConnector
import models.confirmed.{ConfirmedBusinessDetails, ConfirmedDetails, ConfirmedReportingNotifications}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ConfirmedDetailsService @Inject()(confirmedDetailsConnector: ConfirmedDetailsConnector)
                                       (using ExecutionContext) {

  def confirmedDetailsFor(operatorId: String)
                         (using HeaderCarrier): Future[ConfirmedDetails] =
    Future.sequence(Seq(
      confirmedDetailsConnector.businessDetails(operatorId),
      confirmedDetailsConnector.reportingNotifications(operatorId),
      confirmedDetailsConnector.contactDetails()
    )).map { results =>
      val (businessDetails, reportingNotifications, contactDetails) = (results.head, results(1), results(2))

      ConfirmedDetails(
        businessDetails = businessDetails.isDefined,
        reportingNotifications = reportingNotifications.isDefined,
        yourContactDetails = contactDetails.isDefined
      )
    }.recover {
      case NonFatal(_) => ConfirmedDetails(false, false, false)
    }

  def confirmBusinessDetailsFor(operatorId: String)
                               (using HeaderCarrier): Future[ConfirmedDetails] =
    (for {
      _ <- confirmedDetailsConnector.save(ConfirmedBusinessDetails(operatorId))
      confirmedDetails <- confirmedDetailsFor(operatorId)
    } yield confirmedDetails).recover {
      case NonFatal(_) => ConfirmedDetails(false, false, false)
    }

  def confirmReportingNotificationsFor(operatorId: String)
                                      (using HeaderCarrier): Future[ConfirmedDetails] =
    (for {
      _ <- confirmedDetailsConnector.save(ConfirmedReportingNotifications(operatorId))
      confirmedDetails <- confirmedDetailsFor(operatorId)
    } yield confirmedDetails).recover {
      case NonFatal(_) => ConfirmedDetails(false, false, false)
    }

  def confirmContactDetailsFor(operatorId: String)
                              (implicit hc: HeaderCarrier): Future[ConfirmedDetails] =
    (for {
      _ <- confirmedDetailsConnector.saveContactDetails()
      confirmedDetails <- confirmedDetailsFor(operatorId)
    } yield confirmedDetails).recover {
      case NonFatal(_) => ConfirmedDetails(false, false, false)
    }
}
