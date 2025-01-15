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
import builders.ContactDetailsBuilder.aContactDetails
import builders.PlatformOperatorBuilder.aPlatformOperator
import builders.PlatformOperatorSummaryBuilder.aPlatformOperatorSummary
import builders.SubscriptionInfoBuilder.aSubscriptionInfo
import connectors.PlatformOperatorConnector.ViewPlatformOperatorFailure
import connectors.SubscriptionConnector.GetSubscriptionFailure
import connectors.{EmailConnector, PlatformOperatorConnector, SubscriptionConnector}
import models.email.EmailsSentResult
import models.email.requests.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailServiceSpec extends SpecBase with BeforeAndAfterEach {

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val anyBoolean = true
  private val dateTime: Instant = Instant.parse("2100-12-31T00:00:00Z")

  private val mockEmailConnector = mock[EmailConnector]
  private val mockSubscriptionConnector = mock[SubscriptionConnector]
  private val mockPlatformOperatorConnector = mock[PlatformOperatorConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockEmailConnector, mockSubscriptionConnector)
    super.beforeEach()
  }

  private val underTest = new EmailService(mockEmailConnector, mockSubscriptionConnector, mockPlatformOperatorConnector)

  ".sendAddAssumedReportingEmails(...)" - {
    val expectedAddAssumedReportingPlatformOperator = AddAssumedReportingPlatformOperator(
      platformOperatorSummary = aPlatformOperatorSummary,
      assumedReportSummary = anAssumedReportSummary,
      createdInstant = dateTime
    )
    val expectedAddAssumedReportingUser = AddAssumedReportingUser(
      subscriptionInfo = aSubscriptionInfo,
      assumedReportSummary = anAssumedReportSummary,
      createdInstant = dateTime
    )

    "return correct response when getSubscription fails" in {
      when(mockSubscriptionConnector.getSubscription(any)).thenReturn(Future.failed(GetSubscriptionFailure(500)))

      underTest.sendAddAssumedReportingEmails(aPlatformOperatorSummary, dateTime,
        anAssumedReportSummary).futureValue mustBe EmailsSentResult(false, None)

      verify(mockEmailConnector, never()).send(any)(any)
    }

    "non-matching emails must send both AddAssumedReportingUser AddAssumedReportingPlatformOperator" in {
      when(mockSubscriptionConnector.getSubscription(any)).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(anyBoolean))

      underTest.sendAddAssumedReportingEmails(aPlatformOperatorSummary, dateTime,
        anAssumedReportSummary).futureValue mustBe EmailsSentResult(anyBoolean, Some(anyBoolean))

      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddAssumedReportingPlatformOperator))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddAssumedReportingUser))(any())
    }

    "matching emails must send only AddAssumedReportingUser" in {
      val platformOperatorSummary = aPlatformOperatorSummary.copy(operatorPrimaryContactEmail = aSubscriptionInfo.primaryContact.email)
      val expectedAddAssumedReportingPlatformOperatorSameEmail = expectedAddAssumedReportingPlatformOperator.copy(to = List(aSubscriptionInfo.primaryContact.email))
      val expectedAddAssumedReportingUserSameEmail = expectedAddAssumedReportingUser.copy(to = List(aSubscriptionInfo.primaryContact.email))

      when(mockSubscriptionConnector.getSubscription(any)).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(anyBoolean))

      underTest.sendAddAssumedReportingEmails(platformOperatorSummary, dateTime, anAssumedReportSummary).futureValue mustBe EmailsSentResult(anyBoolean, None)

      verify(mockEmailConnector, never()).send(eqTo(expectedAddAssumedReportingPlatformOperatorSameEmail))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddAssumedReportingUserSameEmail))(any())
    }
  }

  ".sendUpdateAssumedReportingEmails(...)" - {
    val expectedUpdateAssumedReportingPlatformOperator = UpdateAssumedReportingPlatformOperator(
      platformOperator = aPlatformOperator,
      assumedReportSummary = anAssumedReportSummary,
      updatedInstant = dateTime
    )
    val expectedUpdateAssumedReportingUser = UpdateAssumedReportingUser(
      subscriptionInfo = aSubscriptionInfo,
      assumedReportSummary = anAssumedReportSummary,
      updatedInstant = dateTime
    )

    "return correct response when viewPlatformOperator fails" in {
      when(mockPlatformOperatorConnector.viewPlatformOperator(any)(any)).thenReturn(Future.failed(ViewPlatformOperatorFailure(500)))

      underTest.sendUpdateAssumedReportingEmails(aPlatformOperator.operatorId, anAssumedReportSummary,
        dateTime).futureValue mustBe EmailsSentResult(false, None)

      verify(mockEmailConnector, never()).send(any)(any)
    }

    "return correct response when getSubscription fails" in {
      when(mockPlatformOperatorConnector.viewPlatformOperator(any)(any)).thenReturn(Future.successful(aPlatformOperator))
      when(mockSubscriptionConnector.getSubscription(any)).thenReturn(Future.failed(GetSubscriptionFailure(500)))

      underTest.sendUpdateAssumedReportingEmails(aPlatformOperator.operatorId, anAssumedReportSummary,
        dateTime).futureValue mustBe EmailsSentResult(false, None)

      verify(mockEmailConnector, never()).send(any)(any)
    }

    "non-matching emails must send both UpdateAssumedReportingUser UpdateAssumedReportingPlatformOperator" in {
      when(mockPlatformOperatorConnector.viewPlatformOperator(any)(any)).thenReturn(Future.successful(aPlatformOperator))
      when(mockSubscriptionConnector.getSubscription(any)).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(true))

      underTest.sendUpdateAssumedReportingEmails(aPlatformOperator.operatorId, anAssumedReportSummary, dateTime)
        .futureValue mustBe EmailsSentResult(true, Some(true))

      verify(mockEmailConnector, times(1)).send(eqTo(expectedUpdateAssumedReportingPlatformOperator))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedUpdateAssumedReportingUser))(any())
    }

    "matching emails must send only UpdateAssumedReportingUser" in {
      val platformOperator = aPlatformOperator.copy(primaryContactDetails = aContactDetails.copy(emailAddress = aSubscriptionInfo.primaryContact.email))
      val expectedUpdateAssumedReportingUserSameEmail = expectedUpdateAssumedReportingUser.copy(to = List(aSubscriptionInfo.primaryContact.email))
      val expectedUpdateAssumedReportingPlatformOperatorSameEmail = expectedUpdateAssumedReportingPlatformOperator.copy(to = List(aSubscriptionInfo.primaryContact.email))

      when(mockPlatformOperatorConnector.viewPlatformOperator(any)(any)).thenReturn(Future.successful(platformOperator))
      when(mockSubscriptionConnector.getSubscription(any)).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(true))

      underTest.sendUpdateAssumedReportingEmails(platformOperator.operatorId, anAssumedReportSummary, dateTime)
        .futureValue mustBe EmailsSentResult(true, None)

      verify(mockEmailConnector, times(1)).send(eqTo(expectedUpdateAssumedReportingUserSameEmail))(any())
      verify(mockEmailConnector, never()).send(eqTo(expectedUpdateAssumedReportingPlatformOperatorSameEmail))(any())
    }
  }

  ".sendDeleteAssumedReportingEmails(...)" - {
    val expectedDeleteAssumedReportingPlatformOperator = DeleteAssumedReportingPlatformOperator(
      platformOperator = aPlatformOperator,
      assumedReportingSubmission = anAssumedReportingSubmission,
      deletedInstant = dateTime
    )
    val expectedDeleteAssumedReportingUser = DeleteAssumedReportingUser(
      subscriptionInfo = aSubscriptionInfo,
      assumedReportingSubmission = anAssumedReportingSubmission,
      deletedInstant = dateTime
    )

    "return correct response when viewPlatformOperator fails" in {
      when(mockPlatformOperatorConnector.viewPlatformOperator(any)(any)).thenReturn(Future.failed(ViewPlatformOperatorFailure(500)))

      underTest.sendDeleteAssumedReportingEmails(aPlatformOperator.operatorId, anAssumedReportingSubmission,
        dateTime).futureValue mustBe EmailsSentResult(false, None)

      verify(mockEmailConnector, never()).send(any)(any)
    }

    "return correct response when getSubscription fails" in {
      when(mockPlatformOperatorConnector.viewPlatformOperator(any)(any)).thenReturn(Future.successful(aPlatformOperator))
      when(mockSubscriptionConnector.getSubscription(any)).thenReturn(Future.failed(GetSubscriptionFailure(500)))

      underTest.sendDeleteAssumedReportingEmails(aPlatformOperator.operatorId, anAssumedReportingSubmission,
        dateTime).futureValue mustBe EmailsSentResult(false, None)

      verify(mockEmailConnector, never()).send(any)(any)
    }

    "non-matching emails must send both DeleteAssumedReportingUser DeleteAssumedReportingPlatformOperator" in {
      when(mockPlatformOperatorConnector.viewPlatformOperator(any)(any)).thenReturn(Future.successful(aPlatformOperator))
      when(mockSubscriptionConnector.getSubscription(any)).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(true))

      underTest.sendDeleteAssumedReportingEmails(aPlatformOperator.operatorId, anAssumedReportingSubmission,
        dateTime).futureValue mustBe EmailsSentResult(true, Some(true))

      verify(mockEmailConnector, times(1)).send(eqTo(expectedDeleteAssumedReportingPlatformOperator))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedDeleteAssumedReportingUser))(any())
    }

    "matching emails must send only DeleteAssumedReportingUser" in {
      val platformOperator = aPlatformOperator.copy(primaryContactDetails = aContactDetails.copy(emailAddress = aSubscriptionInfo.primaryContact.email))
      val expectedDeleteAssumedReportingPlatformOperatorSameEmail = expectedDeleteAssumedReportingPlatformOperator.copy(to = List(aSubscriptionInfo.primaryContact.email))
      val expectedDeleteAssumedReportingUserSameEmail = expectedDeleteAssumedReportingUser.copy(to = List(aSubscriptionInfo.primaryContact.email))

      when(mockPlatformOperatorConnector.viewPlatformOperator(any)(any)).thenReturn(Future.successful(platformOperator))
      when(mockSubscriptionConnector.getSubscription(any)).thenReturn(Future.successful(aSubscriptionInfo))
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(true))

      underTest.sendDeleteAssumedReportingEmails(aPlatformOperator.operatorId, anAssumedReportingSubmission,
        dateTime).futureValue mustBe EmailsSentResult(true, None)

      verify(mockEmailConnector, never()).send(eqTo(expectedDeleteAssumedReportingPlatformOperatorSameEmail))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedDeleteAssumedReportingUserSameEmail))(any())
    }
  }
}