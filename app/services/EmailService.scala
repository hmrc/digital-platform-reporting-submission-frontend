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
import models.email.requests.{AddAssumedReportingPlatformOperator, AddAssumedReportingUser, SendEmailRequest, UpdateAssumedReportingPlatformOperator, UpdateAssumedReportingUser}
import models.submission.AssumedReportSummary
import models.subscription.SubscriptionInfo
import org.apache.pekko.Done
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateTimeFormats.EmailDateTimeFormatter
import viewmodels.PlatformOperatorSummary

import java.time.Instant
import scala.concurrent.Future

class EmailService @Inject()(emailConnector: EmailConnector) {

  def sendAddAssumedReportingEmails(subscriptionInfo: SubscriptionInfo,
                                    platformOperator: PlatformOperatorSummary,
                                    submissionCreated: Instant,
                                    summary: AssumedReportSummary)
                                   (implicit hc: HeaderCarrier): Future[Done] = {
    val checksCompletedDateTime: String = EmailDateTimeFormatter.format(submissionCreated).replace("AM", "am").replace("PM", "pm")
    if (!matchingEmails(subscriptionInfo.primaryContact.email, platformOperator.operatorPrimaryContactEmail)) {
      sendEmail(AddAssumedReportingPlatformOperator(
        platformOperator.operatorPrimaryContactEmail,
        platformOperator.operatorPrimaryContactName,
        checksCompletedDateTime,
        summary.assumingOperatorName,
        summary.operatorName,
        summary.reportingPeriod.toString))
    }
    sendEmail(AddAssumedReportingUser(
     subscriptionInfo.primaryContact.email,
     subscriptionInfo.primaryContactName,
     checksCompletedDateTime,
     summary.assumingOperatorName,
     summary.operatorName,
     summary.reportingPeriod.toString))
  }

  def sendUpdateAssumedReportingEmails(subscriptionInfo: SubscriptionInfo,
                                    platformOperator: PlatformOperatorSummary,
                                    submissionCreated: Instant,
                                    summary: AssumedReportSummary)
                                   (implicit hc: HeaderCarrier): Future[Done] = {
    val checksCompletedDateTime: String = EmailDateTimeFormatter.format(submissionCreated).replace("AM", "am").replace("PM", "pm")
    if (!matchingEmails(subscriptionInfo.primaryContact.email, platformOperator.operatorPrimaryContactEmail)) {
      sendEmail(UpdateAssumedReportingPlatformOperator(
        platformOperator.operatorPrimaryContactEmail,
        platformOperator.operatorPrimaryContactName,
        checksCompletedDateTime,
        summary.assumingOperatorName,
        summary.operatorName,
        summary.reportingPeriod.toString))
    }
    sendEmail(UpdateAssumedReportingUser(
      subscriptionInfo.primaryContact.email,
      subscriptionInfo.primaryContactName,
      checksCompletedDateTime,
      summary.assumingOperatorName,
      summary.operatorName,
      summary.reportingPeriod.toString))
  }

  private def matchingEmails(primaryContactEmail: String, poEmail: String): Boolean =
    primaryContactEmail.trim.toLowerCase() == poEmail.trim.toLowerCase

  private def sendEmail(requestBuild: SendEmailRequest)(implicit hc: HeaderCarrier): Future[Done] =
     emailConnector.send(requestBuild)

}
