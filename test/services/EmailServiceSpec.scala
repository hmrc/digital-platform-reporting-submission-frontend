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
import connectors.EmailConnector
import models.email.requests.{AddAssumedReportingPlatformOperator, AddAssumedReportingUser, UpdateAssumedReportingPlatformOperator, UpdateAssumedReportingUser}
import models.submission.AssumedReportSummary
import models.subscription._
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.PlatformOperatorSummary

import java.time.{Instant, Year}
import scala.concurrent.Future

class EmailServiceSpec extends SpecBase with BeforeAndAfterEach {

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val mockEmailConnector = mock[EmailConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockEmailConnector)
    super.beforeEach()
  }

  private val underTest = new EmailService(mockEmailConnector)

  ".sendAddAssumedReportingEmails(...)" - {

    val dateTime: Instant = Instant.parse("2100-12-31T00:00:00Z")
    val checksCompletedDateTime = "12:00am GMT on 31 December 2100"

    val expectedIndividual = IndividualContact(Individual("first", "last"), "user.email", None)
    val subscriptionInfo = SubscriptionInfo("DPRS123", true, None, expectedIndividual, None)

    val expectedAddAssumedReportingUser = AddAssumedReportingUser("user.email", "first last", checksCompletedDateTime, "assumingOperator", operatorName, "2024")
    val expectedAddAssumedReportingPlatformOperator = AddAssumedReportingPlatformOperator("po.email", "primaryContactName", checksCompletedDateTime, "assumingOperator", operatorName, "2024")

    val reportingPeriod = Year.of(2024)

    val assumedReportingSummary = AssumedReportSummary(operatorId, operatorName, "assumingOperator", reportingPeriod)
    val platformOperatorSummary = PlatformOperatorSummary(operatorId, operatorName, "primaryContactName", "po.email", hasReportingNotifications = false)


    "non-matching emails must send both AddAssumedReportingUser AddAssumedReportingPlatformOperator" in {
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendAddAssumedReportingEmails(subscriptionInfo, platformOperatorSummary, dateTime, assumedReportingSummary).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddAssumedReportingUser))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddAssumedReportingPlatformOperator))(any())
    }

    "matching emails must send only AddAssumedReportingUser" in {

      val expectedIndividualSameEmail = expectedIndividual.copy(email = "po.email")
      val subscriptionInfoSameEmil = subscriptionInfo.copy(primaryContact = expectedIndividualSameEmail)

      val expectedAddAssumedReportingUserSameEmail = expectedAddAssumedReportingUser.copy(to = List("po.email"))
      val expectedAddAssumedReportingPlatformOperatorSameEmail = expectedAddAssumedReportingPlatformOperator.copy(to = List("po.email"))

      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendAddAssumedReportingEmails(subscriptionInfoSameEmil, platformOperatorSummary, dateTime, assumedReportingSummary).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedAddAssumedReportingUserSameEmail))(any())
      verify(mockEmailConnector, never()).send(eqTo(expectedAddAssumedReportingPlatformOperatorSameEmail))(any())
    }
  }


  ".sendUpdateAssumedReportingEmails(...)" - {

    val dateTime: Instant = Instant.parse("2100-12-31T00:00:00Z")
    val checksCompletedDateTime = "12:00am GMT on 31 December 2100"

    val expectedIndividual = IndividualContact(Individual("first", "last"), "user.email", None)
    val subscriptionInfo = SubscriptionInfo("DPRS123", true, None, expectedIndividual, None)

    val expectedUpdateAssumedReportingUser = UpdateAssumedReportingUser("user.email", "first last", checksCompletedDateTime, "assumingOperator", operatorName, "2024")
    val expectedUpdateAssumedReportingPlatformOperator = UpdateAssumedReportingPlatformOperator("po.email", "primaryContactName", checksCompletedDateTime, "assumingOperator", operatorName, "2024")

    val reportingPeriod = Year.of(2024)

    val assumedReportingSummary = AssumedReportSummary(operatorId, operatorName, "assumingOperator", reportingPeriod)
    val platformOperatorSummary = PlatformOperatorSummary(operatorId, operatorName, "primaryContactName", "po.email", hasReportingNotifications = false)


    "non-matching emails must send both UpdateAssumedReportingUser UpdateAssumedReportingPlatformOperator" in {
      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendUpdateAssumedReportingEmails(subscriptionInfo, platformOperatorSummary, dateTime, assumedReportingSummary).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedUpdateAssumedReportingUser))(any())
      verify(mockEmailConnector, times(1)).send(eqTo(expectedUpdateAssumedReportingPlatformOperator))(any())
    }

    "matching emails must send only UpdateAssumedReportingUser" in {

      val expectedIndividualSameEmail = expectedIndividual.copy(email = "po.email")
      val subscriptionInfoSameEmil = subscriptionInfo.copy(primaryContact = expectedIndividualSameEmail)

      val expectedUpdateAssumedReportingUserSameEmail = expectedUpdateAssumedReportingUser.copy(to = List("po.email"))
      val expectedUpdateAssumedReportingPlatformOperatorSameEmail = expectedUpdateAssumedReportingPlatformOperator.copy(to = List("po.email"))

      when(mockEmailConnector.send(any())(any())).thenReturn(Future.successful(Done))

      underTest.sendUpdateAssumedReportingEmails(subscriptionInfoSameEmil, platformOperatorSummary, dateTime, assumedReportingSummary).futureValue

      verify(mockEmailConnector, times(1)).send(eqTo(expectedUpdateAssumedReportingUserSameEmail))(any())
      verify(mockEmailConnector, never()).send(eqTo(expectedUpdateAssumedReportingPlatformOperatorSameEmail))(any())
    }
  }

}