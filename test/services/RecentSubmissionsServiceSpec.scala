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

import connectors.RecentSubmissionsConnector
import models.recentsubmissions.{ConfirmedDetails, RecentSubmissionDetails}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import support.builders.OperatorSubmissionDetailsBuilder.anOperatorSubmissionDetails
import support.builders.RecentSubmissionDetailsBuilder.aRecentSubmissionDetails
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RecentSubmissionsServiceSpec extends AnyFreeSpec
  with Matchers
  with MockitoSugar
  with BeforeAndAfterEach {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  private val submissionDetails = aRecentSubmissionDetails.copy(
    operatorDetails = Map("known-operator-id" -> anOperatorSubmissionDetails.copy(operatorId = "known-operator-id")),
    yourContactDetailsCorrect = None
  )

  private val mockRecentSubmissionsConnector = mock[RecentSubmissionsConnector]

  private val underTest = new RecentSubmissionsService(mockRecentSubmissionsConnector)

  override def beforeEach(): Unit = {
    Mockito.reset(mockRecentSubmissionsConnector)
    super.beforeEach()
  }

  ".handleRecentSubmissionWith(...)" - {
    "must return correct ConfirmedDetails when" - {
      "getting recent submissions result in failure" in {
        when(mockRecentSubmissionsConnector.getRecentUserSubmissions()(any())).thenReturn(Future.failed(new RuntimeException()))

        val result = underTest.handleRecentSubmissionWith(_ => ConfirmedDetails(true, true, true))(hc).futureValue
        result mustBe ConfirmedDetails(false, false, false)
      }

      "getting recent submissions returns None" in {
        when(mockRecentSubmissionsConnector.getRecentUserSubmissions()(any())).thenReturn(Future.successful(None))

        val result = underTest.handleRecentSubmissionWith(_ => ConfirmedDetails(true, true, true)).futureValue
        result mustBe ConfirmedDetails(false, false, false)

        verify(mockRecentSubmissionsConnector, times(1)).save(RecentSubmissionDetails(Map(), None))
      }

      "getting recent submissions returns Some value" in {
        when(mockRecentSubmissionsConnector.getRecentUserSubmissions()(any())).thenReturn(Future.successful(Some(aRecentSubmissionDetails)))

        val result = underTest.handleRecentSubmissionWith(_ => ConfirmedDetails(true, true, true)).futureValue
        result mustBe ConfirmedDetails(true, true, true)

        verify(mockRecentSubmissionsConnector, never()).save(any())(any())
      }
    }
  }

  ".getConfirmedDetailsFor(...)" - {
    "must return correct ConfirmedDetails when" - {
      "data found for operatorId" in {
        when(mockRecentSubmissionsConnector.getRecentUserSubmissions()(any())).thenReturn(Future.successful(Some(submissionDetails)))

        underTest.getConfirmedDetailsFor("known-operator-id").futureValue mustBe ConfirmedDetails(
          businessDetails = anOperatorSubmissionDetails.businessDetailsCorrect,
          reportingNotifications = anOperatorSubmissionDetails.reportingNotificationsCorrect.contains(true),
          yourContactDetails = submissionDetails.yourContactDetailsCorrect.contains(true)
        )
      }

      "no data found for operatorId" in {
        when(mockRecentSubmissionsConnector.getRecentUserSubmissions()(any())).thenReturn(Future.successful(Some(submissionDetails)))

        underTest.getConfirmedDetailsFor("unknown-operator-id").futureValue mustBe ConfirmedDetails(
          businessDetails = false,
          reportingNotifications = false,
          yourContactDetails = submissionDetails.yourContactDetailsCorrect.contains(true)
        )
      }
    }
  }

  ".confirmBusinessDetailsFor(...)" - {
    "must update details and return correct ConfirmedDetails when" - {
      "data found for operatorId" in {
        val expectedUpdatedDetails = aRecentSubmissionDetails.copy(
          operatorDetails = Map("known-operator-id" -> anOperatorSubmissionDetails.copy(operatorId = "known-operator-id", businessDetailsCorrect = true)),
          yourContactDetailsCorrect = None
        )

        when(mockRecentSubmissionsConnector.getRecentUserSubmissions()(any())).thenReturn(Future.successful(Some(submissionDetails)))

        underTest.confirmBusinessDetailsFor("known-operator-id").futureValue mustBe ConfirmedDetails(
          businessDetails = true,
          reportingNotifications = false,
          yourContactDetails = submissionDetails.yourContactDetailsCorrect.contains(true)
        )

        verify(mockRecentSubmissionsConnector, times(1)).save(expectedUpdatedDetails)
      }

      "no data found for operatorId" in {
        val expectedUpdatedDetails = aRecentSubmissionDetails.copy(
          operatorDetails = Map(
            "known-operator-id" -> anOperatorSubmissionDetails.copy("known-operator-id", true),
            "unknown-operator-id" -> anOperatorSubmissionDetails.copy("unknown-operator-id", true, None)
          ),
          yourContactDetailsCorrect = None
        )

        when(mockRecentSubmissionsConnector.getRecentUserSubmissions()(any())).thenReturn(Future.successful(Some(submissionDetails)))

        underTest.confirmBusinessDetailsFor("unknown-operator-id").futureValue mustBe ConfirmedDetails(
          businessDetails = true,
          reportingNotifications = false,
          yourContactDetails = submissionDetails.yourContactDetailsCorrect.contains(true)
        )

        verify(mockRecentSubmissionsConnector, times(1)).save(expectedUpdatedDetails)
      }
    }
  }

  ".confirmReportingNotificationsFor(...)" - {
    "must update details and return correct ConfirmedDetails when" - {
      "data found for operatorId" in {
        val expectedUpdatedDetails = aRecentSubmissionDetails.copy(
          operatorDetails = Map("known-operator-id" -> anOperatorSubmissionDetails.copy("known-operator-id", true, Some(true))),
          yourContactDetailsCorrect = None
        )

        when(mockRecentSubmissionsConnector.getRecentUserSubmissions()(any())).thenReturn(Future.successful(Some(submissionDetails)))

        underTest.confirmReportingNotificationsFor("known-operator-id").futureValue mustBe ConfirmedDetails(
          businessDetails = true,
          reportingNotifications = true,
          yourContactDetails = submissionDetails.yourContactDetailsCorrect.contains(true)
        )

        verify(mockRecentSubmissionsConnector, times(1)).save(expectedUpdatedDetails)
      }

      "no data found for operatorId" in {
        val expectedUpdatedDetails = aRecentSubmissionDetails.copy(
          operatorDetails = Map(
            "known-operator-id" -> anOperatorSubmissionDetails.copy("known-operator-id", true),
            "unknown-operator-id" -> anOperatorSubmissionDetails.copy("unknown-operator-id", false, Some(true))
          ),
          yourContactDetailsCorrect = None
        )

        when(mockRecentSubmissionsConnector.getRecentUserSubmissions()(any())).thenReturn(Future.successful(Some(submissionDetails)))

        underTest.confirmReportingNotificationsFor("unknown-operator-id").futureValue mustBe ConfirmedDetails(
          businessDetails = false,
          reportingNotifications = true,
          yourContactDetails = submissionDetails.yourContactDetailsCorrect.contains(true)
        )

        verify(mockRecentSubmissionsConnector, times(1)).save(expectedUpdatedDetails)
      }
    }
  }

  ".confirmYourContactDetailsFor(...)" - {
    "must update details and return correct ConfirmedDetails when" - {
      "data found for operatorId" in {
        val expectedUpdatedDetails = aRecentSubmissionDetails.copy(
          operatorDetails = Map("known-operator-id" -> anOperatorSubmissionDetails.copy("known-operator-id", true)),
          yourContactDetailsCorrect = Some(true)
        )

        when(mockRecentSubmissionsConnector.getRecentUserSubmissions()(any())).thenReturn(Future.successful(Some(submissionDetails)))

        underTest.confirmYourContactDetailsFor("known-operator-id").futureValue mustBe ConfirmedDetails(
          businessDetails = true,
          reportingNotifications = anOperatorSubmissionDetails.reportingNotificationsCorrect.contains(true),
          yourContactDetails = true
        )

        verify(mockRecentSubmissionsConnector, times(1)).save(expectedUpdatedDetails)
      }

      "no data found for operatorId" in {
        val expectedUpdatedDetails = aRecentSubmissionDetails.copy(
          operatorDetails = Map("known-operator-id" -> anOperatorSubmissionDetails.copy(operatorId = "known-operator-id")),
          yourContactDetailsCorrect = Some(true)
        )

        when(mockRecentSubmissionsConnector.getRecentUserSubmissions()(any())).thenReturn(Future.successful(Some(submissionDetails)))

        underTest.confirmYourContactDetailsFor("unknown-operator-id").futureValue mustBe ConfirmedDetails(
          businessDetails = false,
          reportingNotifications = false,
          yourContactDetails = true
        )

        verify(mockRecentSubmissionsConnector, times(1)).save(expectedUpdatedDetails)
      }
    }
  }
}
