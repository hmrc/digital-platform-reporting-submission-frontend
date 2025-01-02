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
import connectors.EmailConnector
import models.email.requests.*
import models.operator.responses.PlatformOperator
import models.submission.{AssumedReportSummary, AssumedReportingSubmission}
import models.subscription.SubscriptionInfo
import org.apache.pekko.Done
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateTimeFormats.EmailDateTimeFormatter
import viewmodels.PlatformOperatorSummary

import java.time.Instant
import scala.concurrent.Future

class EmailService @Inject()(emailConnector: EmailConnector) {

  def sendAddAssumedReportingEmails(subscriptionInfo: SubscriptionInfo,
                                    platformOperatorSummary: PlatformOperatorSummary,
                                    createdInstant: Instant,
                                    assumedReportSummary: AssumedReportSummary)
                                   (using HeaderCarrier): Future[Done] = {
    val checksCompletedDateTime = EmailDateTimeFormatter.format(createdInstant)
    if (!matchingEmails(subscriptionInfo.primaryContact.email, platformOperatorSummary.operatorPrimaryContactEmail)) {
      emailConnector.send(AddAssumedReportingPlatformOperator(
        platformOperatorSummary.operatorPrimaryContactEmail,
        platformOperatorSummary.operatorPrimaryContactName,
        checksCompletedDateTime,
        assumedReportSummary.assumingOperatorName,
        assumedReportSummary.operatorName,
        assumedReportSummary.reportingPeriod.toString))
    }
    emailConnector.send(AddAssumedReportingUser(
      subscriptionInfo.primaryContact.email,
      subscriptionInfo.primaryContactName,
      checksCompletedDateTime,
      assumedReportSummary.assumingOperatorName,
      assumedReportSummary.operatorName,
      assumedReportSummary.reportingPeriod.toString))
  }

  def sendUpdateAssumedReportingEmails(subscriptionInfo: SubscriptionInfo,
                                       platformOperator: PlatformOperator,
                                       updatedInstant: Instant,
                                       assumedReportSummary: AssumedReportSummary)
                                      (using HeaderCarrier): Future[Done] = {
    val checksCompletedDateTime = EmailDateTimeFormatter.format(updatedInstant)
    if (!matchingEmails(subscriptionInfo.primaryContact.email, platformOperator.primaryContactDetails.emailAddress)) {
      emailConnector.send(UpdateAssumedReportingPlatformOperator(
        platformOperator.primaryContactDetails.emailAddress,
        platformOperator.primaryContactDetails.contactName,
        checksCompletedDateTime,
        assumedReportSummary.assumingOperatorName,
        assumedReportSummary.operatorName,
        assumedReportSummary.reportingPeriod.toString))
    }
    emailConnector.send(UpdateAssumedReportingUser(
      subscriptionInfo.primaryContact.email,
      subscriptionInfo.primaryContactName,
      checksCompletedDateTime,
      assumedReportSummary.assumingOperatorName,
      assumedReportSummary.operatorName,
      assumedReportSummary.reportingPeriod.toString))
  }

  def sendDeleteAssumedReportingEmails(subscriptionInfo: SubscriptionInfo,
                                       platformOperator: PlatformOperator,
                                       assumedReportingSubmission: AssumedReportingSubmission,
                                       deletedInstant: Instant)
                                      (using HeaderCarrier): Future[Done] = {
    val checksCompletedDateTime = EmailDateTimeFormatter.format(deletedInstant)
    if (!matchingEmails(subscriptionInfo.primaryContact.email, platformOperator.primaryContactDetails.emailAddress)) {
      emailConnector.send(DeleteAssumedReportingPlatformOperator(
        platformOperator.primaryContactDetails.emailAddress,
        platformOperator.primaryContactDetails.contactName,
        checksCompletedDateTime,
        assumedReportingSubmission.assumingOperator.name,
        assumedReportingSubmission.operatorName,
        assumedReportingSubmission.reportingPeriod.toString))
    }
    emailConnector.send(DeleteAssumedReportingUser(
      subscriptionInfo.primaryContact.email,
      subscriptionInfo.primaryContactName,
      checksCompletedDateTime,
      assumedReportingSubmission.assumingOperator.name,
      assumedReportingSubmission.operatorName,
      assumedReportingSubmission.reportingPeriod.toString))
  }

  private def matchingEmails(primaryContactEmail: String, poEmail: String): Boolean =
    primaryContactEmail.trim.toLowerCase() == poEmail.trim.toLowerCase
}
