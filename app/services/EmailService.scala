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

import com.google.inject.Inject
import connectors.{EmailConnector, PlatformOperatorConnector, SubscriptionConnector}
import models.email.EmailsSentResult
import models.email.requests.*
import models.submission.{AssumedReportSummary, AssumedReportingSubmission}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.PlatformOperatorSummary

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class EmailService @Inject()(emailConnector: EmailConnector,
                             subscriptionConnector: SubscriptionConnector,
                             platformOperatorConnector: PlatformOperatorConnector)
                            (using ExecutionContext) extends Logging {

  def sendAddAssumedReportingEmails(platformOperatorSummary: PlatformOperatorSummary,
                                    createdInstant: Instant,
                                    assumedReportSummary: AssumedReportSummary)
                                   (using HeaderCarrier): Future[EmailsSentResult] = {
    (for {
      subscriptionInfo <- subscriptionConnector.getSubscription
      emailSentResult <- {
        val poEmailSent = if (!matchingEmails(subscriptionInfo.primaryContact.email, platformOperatorSummary.operatorPrimaryContactEmail)) {
          Some(emailConnector.send(AddAssumedReportingPlatformOperator(platformOperatorSummary, assumedReportSummary, createdInstant)))
        } else None
        val userEmailSent = emailConnector.send(AddAssumedReportingUser(subscriptionInfo, assumedReportSummary, createdInstant))
        for {
          userResult <- userEmailSent
          poResult <- poEmailSent.map(_.map(Some(_))).getOrElse(Future.successful(None))
        } yield EmailsSentResult(userResult, poResult)
      }
    } yield emailSentResult).recover {
      case error => logger.warn("Add assumed reporting emails not sent", error)
        EmailsSentResult(false, None)
    }
  }

  def sendUpdateAssumedReportingEmails(operatorId: String,
                                       assumedReportSummary: AssumedReportSummary,
                                       updatedInstant: Instant)
                                      (using HeaderCarrier): Future[EmailsSentResult] = {
    (for {
      platformOperator <- platformOperatorConnector.viewPlatformOperator(operatorId)
      subscriptionInfo <- subscriptionConnector.getSubscription
      emailSentResult <- {
        val poEmailSent = if (!matchingEmails(subscriptionInfo.primaryContact.email, platformOperator.primaryContactDetails.emailAddress)) {
          Some(emailConnector.send(UpdateAssumedReportingPlatformOperator(platformOperator, assumedReportSummary, updatedInstant)))
        } else None
        val userEmailSent = emailConnector.send(UpdateAssumedReportingUser(subscriptionInfo, assumedReportSummary, updatedInstant))
        for {
          userResult <- userEmailSent
          poResult <- poEmailSent.map(_.map(Some(_))).getOrElse(Future.successful(None))
        } yield EmailsSentResult(userResult, poResult)
      }
    } yield emailSentResult).recover {
      case error => logger.warn("Update assumed reporting emails not sent", error)
        EmailsSentResult(false, None)
    }
  }

  def sendDeleteAssumedReportingEmails(operatorId: String,
                                       assumedReportingSubmission: AssumedReportingSubmission,
                                       deletedInstant: Instant)
                                      (using HeaderCarrier): Future[EmailsSentResult] = {
    (for {
      platformOperator <- platformOperatorConnector.viewPlatformOperator(operatorId)
      subscriptionInfo <- subscriptionConnector.getSubscription
      emailSentResult <- {
        val poEmailSent = if (!matchingEmails(subscriptionInfo.primaryContact.email, platformOperator.primaryContactDetails.emailAddress)) {
          Some(emailConnector.send(DeleteAssumedReportingPlatformOperator(platformOperator, assumedReportingSubmission, deletedInstant)))
        } else None
        val userEmailSent = emailConnector.send(DeleteAssumedReportingUser(subscriptionInfo, assumedReportingSubmission, deletedInstant))
        for {
          userResult <- userEmailSent
          poResult <- poEmailSent.map(_.map(Some(_))).getOrElse(Future.successful(None))
        } yield EmailsSentResult(userResult, poResult)
      }
    } yield emailSentResult).recover {
      case error => logger.warn("Update assumed reporting emails not sent", error)
        EmailsSentResult(false, None)
    }
  }

  private def matchingEmails(primaryContactEmail: String, poEmail: String): Boolean =
    primaryContactEmail.trim.toLowerCase() == poEmail.trim.toLowerCase
}
