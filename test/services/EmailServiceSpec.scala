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

import base.SpecBase
import builders.AssumedReportSummaryBuilder.anAssumedReportSummary
import builders.AssumedReportingSubmissionBuilder.anAssumedReportingSubmission
import builders.PlatformOperatorBuilder.aPlatformOperator
import builders.PlatformOperatorSummaryBuilder.aPlatformOperatorSummary
import builders.SubscriptionInfoBuilder.aSubscriptionInfo
import connectors.EmailConnector
import models.email.requests.*
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Instant, Year}
import scala.concurrent.Future

class EmailServiceSpec extends SpecBase with BeforeAndAfterEach {

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val dateTime: Instant = Instant.parse("2100-12-31T00:00:00Z")
  private val completedDateTime = "12:00am GMT on 31 December 2100"

  private val mockEmailConnector = mock[EmailConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockEmailConnector)
    super.beforeEach()
  }

  private val underTest = new EmailService(mockEmailConnector)

  ".sendAddAssumedReportingEmails(...)" - {
    val expectedAddAssumedReportingPlatformOperator = AddAssumedReportingPlatformOperator(
      email = aPlatformOperatorSummary.operatorPrimaryContactEmail,
      platformOperatorContactName = aPlatformOperatorSummary.operatorPrimaryContactName,
      checksCompletedDateTime = completedDateTime,
      assumingPlatformOperator = anAssumedReportSummary.assumingOperatorName,
      businessName = anAssumedReportSummary.operatorName,
      reportingPeriod = anAssumedReportSummary.reportingPeriod.toString
    )
    val expectedAddAssumedReportingUser = AddAssumedReportingUser(
      email = aSubscriptionInfo.primaryContact.email,
      name = aSubscriptionInfo.primaryContactName,
      checksCompletedDateTime = completedDateTime,
      assumingPlatformOperator = anAssumedReportSummary.assumingOperatorName,
      businessName = anAssumedReportSummary.operatorName,
      reportingPeriod = anAssumedReportSummary.reportingPeriod.toString
    )

    "non-matching emails must send both AddAssumedReportingUser AddAssumedReportingPlatformOperator" in {
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendAddAssumedReportingEmails(aSubscriptionInfo, aPlatformOperatorSummary, dateTime, anAssumedReportSummary).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddAssumedReportingPlatformOperator))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddAssumedReportingUser))(any())
    }

    "matching emails must send only AddAssumedReportingUser" in {
      val expectedAddAssumedReportingPlatformOperatorSameEmail = expectedAddAssumedReportingPlatformOperator.copy(to = List(aSubscriptionInfo.primaryContact.email))
      val expectedAddAssumedReportingUserSameEmail = expectedAddAssumedReportingUser.copy(to = List(aSubscriptionInfo.primaryContact.email))

      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendAddAssumedReportingEmails(aSubscriptionInfo, aPlatformOperatorSummary, dateTime, anAssumedReportSummary).futureValue

      verify(mockEmailConnector, never()).send(eqTo(expectedAddAssumedReportingPlatformOperatorSameEmail))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddAssumedReportingUserSameEmail))(any())
    }
  }

  ".sendUpdateAssumedReportingEmails(...)" - {
    val expectedUpdateAssumedReportingPlatformOperator = UpdateAssumedReportingPlatformOperator(
      email = aPlatformOperator.primaryContactDetails.emailAddress,
      platformOperatorContactName = aPlatformOperator.primaryContactDetails.contactName,
      checksCompletedDateTime = completedDateTime,
      assumingPlatformOperator = anAssumedReportSummary.assumingOperatorName,
      businessName = anAssumedReportSummary.operatorName,
      reportingPeriod = anAssumedReportSummary.reportingPeriod.toString
    )
    val expectedUpdateAssumedReportingUser = UpdateAssumedReportingUser(
      email = aSubscriptionInfo.primaryContact.email,
      name = aSubscriptionInfo.primaryContactName,
      checksCompletedDateTime = completedDateTime,
      assumingPlatformOperator = anAssumedReportSummary.assumingOperatorName,
      businessName = anAssumedReportSummary.operatorName,
      reportingPeriod = Year.now.toString
    )

    "non-matching emails must send both UpdateAssumedReportingUser UpdateAssumedReportingPlatformOperator" in {
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendUpdateAssumedReportingEmails(aSubscriptionInfo, aPlatformOperator, dateTime, anAssumedReportSummary).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedUpdateAssumedReportingPlatformOperator))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedUpdateAssumedReportingUser))(any())
    }

    "matching emails must send only UpdateAssumedReportingUser" in {
      val expectedUpdateAssumedReportingUserSameEmail = expectedUpdateAssumedReportingUser.copy(to = List(aSubscriptionInfo.primaryContact.email))
      val expectedUpdateAssumedReportingPlatformOperatorSameEmail = expectedUpdateAssumedReportingPlatformOperator.copy(to = List(aSubscriptionInfo.primaryContact.email))

      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendUpdateAssumedReportingEmails(aSubscriptionInfo, aPlatformOperator, dateTime, anAssumedReportSummary).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedUpdateAssumedReportingUserSameEmail))(any())
      verify(mockEmailConnector, never()).send(eqTo(expectedUpdateAssumedReportingPlatformOperatorSameEmail))(any())
    }
  }

  ".sendDeleteAssumedReportingEmails(...)" - {
    val expectedDeleteAssumedReportingPlatformOperator = DeleteAssumedReportingPlatformOperator(
      email = aPlatformOperator.primaryContactDetails.emailAddress,
      platformOperatorContactName = aPlatformOperator.primaryContactDetails.contactName,
      checksCompletedDateTime = completedDateTime,
      assumingPlatformOperator = anAssumedReportingSubmission.assumingOperator.name,
      businessName = anAssumedReportingSubmission.operatorName,
      reportingPeriod = anAssumedReportingSubmission.reportingPeriod.toString
    )
    val expectedDeleteAssumedReportingUser = DeleteAssumedReportingUser(
      email = aSubscriptionInfo.primaryContact.email,
      name = aSubscriptionInfo.primaryContactName,
      checksCompletedDateTime = completedDateTime,
      assumingPlatformOperator = anAssumedReportingSubmission.assumingOperator.name,
      businessName = anAssumedReportingSubmission.operatorName,
      reportingPeriod = anAssumedReportingSubmission.reportingPeriod.toString
    )

    "non-matching emails must send both DeleteAssumedReportingUser DeleteAssumedReportingPlatformOperator" in {
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendDeleteAssumedReportingEmails(aSubscriptionInfo, aPlatformOperator, anAssumedReportingSubmission, dateTime).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedDeleteAssumedReportingPlatformOperator))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedDeleteAssumedReportingUser))(any())
    }

    "matching emails must send only DeleteAssumedReportingUser" in {
      val expectedDeleteAssumedReportingPlatformOperatorSameEmail = expectedDeleteAssumedReportingPlatformOperator.copy(to = List(aSubscriptionInfo.primaryContact.email))
      val expectedDeleteAssumedReportingUserSameEmail = expectedDeleteAssumedReportingUser.copy(to = List(aSubscriptionInfo.primaryContact.email))

      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendDeleteAssumedReportingEmails(aSubscriptionInfo, aPlatformOperator, anAssumedReportingSubmission, dateTime).futureValue

      verify(mockEmailConnector, never()).send(eqTo(expectedDeleteAssumedReportingPlatformOperatorSameEmail))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedDeleteAssumedReportingUserSameEmail))(any())
    }
  }
}