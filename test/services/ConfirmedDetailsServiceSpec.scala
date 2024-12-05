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
import models.confirmed.{ConfirmedBusinessDetails, ConfirmedContactDetails, ConfirmedDetails, ConfirmedReportingNotifications}
import org.apache.pekko.Done
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import support.builders.ConfirmedBusinessDetailsBuilder.aConfirmedBusinessDetails
import support.builders.ConfirmedReportingNotificationsBuilder.aConfirmedReportingNotifications
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ConfirmedDetailsServiceSpec extends AnyFreeSpec
  with Matchers
  with MockitoSugar
  with BeforeAndAfterEach {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  private val operatorId = "any-operator-id"

  private val mockConfirmedDetailsConnector = mock[ConfirmedDetailsConnector]

  private val underTest = new ConfirmedDetailsService(mockConfirmedDetailsConnector)

  override def beforeEach(): Unit = {
    Mockito.reset(mockConfirmedDetailsConnector)
    super.beforeEach()
  }

  ".confirmedDetailsFor(...)" - {
    "must return correct ConfirmedDetails object" - {
      "when details confirmed" in {
        when(mockConfirmedDetailsConnector.businessDetails(operatorId)).thenReturn(Future.successful(Some(aConfirmedBusinessDetails)))
        when(mockConfirmedDetailsConnector.reportingNotifications(operatorId)).thenReturn(Future.successful(Some(aConfirmedReportingNotifications)))
        when(mockConfirmedDetailsConnector.contactDetails()).thenReturn(Future.successful(Some(ConfirmedContactDetails())))

        underTest.confirmedDetailsFor(operatorId).futureValue mustBe ConfirmedDetails(true, true, true)
      }

      "when details not confirmed" in {
        when(mockConfirmedDetailsConnector.businessDetails(operatorId)).thenReturn(Future.successful(None))
        when(mockConfirmedDetailsConnector.reportingNotifications(operatorId)).thenReturn(Future.successful(None))
        when(mockConfirmedDetailsConnector.contactDetails()).thenReturn(Future.successful(None))

        underTest.confirmedDetailsFor(operatorId).futureValue mustBe ConfirmedDetails(false, false, false)
      }

      "when error while collecting confirmed details" in {
        when(mockConfirmedDetailsConnector.businessDetails(operatorId)).thenReturn(Future.successful(Some(aConfirmedBusinessDetails)))
        when(mockConfirmedDetailsConnector.reportingNotifications(operatorId)).thenReturn(Future.successful(Some(aConfirmedReportingNotifications)))
        when(mockConfirmedDetailsConnector.contactDetails()).thenReturn(Future.failed(new RuntimeException()))

        underTest.confirmedDetailsFor(operatorId).futureValue mustBe ConfirmedDetails(false, false, false)
      }
    }
  }

  ".confirmBusinessDetailsFor(...)" - {
    "must confirm business details and return correct ConfirmedDetails object" in {
      when(mockConfirmedDetailsConnector.save(ConfirmedBusinessDetails(operatorId))).thenReturn(Future.successful(Done))
      when(mockConfirmedDetailsConnector.businessDetails(operatorId)).thenReturn(Future.successful(Some(aConfirmedBusinessDetails)))
      when(mockConfirmedDetailsConnector.reportingNotifications(operatorId)).thenReturn(Future.successful(Some(aConfirmedReportingNotifications)))
      when(mockConfirmedDetailsConnector.contactDetails()).thenReturn(Future.successful(Some(ConfirmedContactDetails())))

      underTest.confirmBusinessDetailsFor(operatorId).futureValue mustBe ConfirmedDetails(true, true, true)
    }

    "must return correct ConfirmedDetails when failure during confirmation" in {
      when(mockConfirmedDetailsConnector.save(ConfirmedBusinessDetails(operatorId))).thenReturn(Future.failed(new RuntimeException()))
      when(mockConfirmedDetailsConnector.businessDetails(operatorId)).thenReturn(Future.successful(Some(aConfirmedBusinessDetails)))
      when(mockConfirmedDetailsConnector.reportingNotifications(operatorId)).thenReturn(Future.successful(Some(aConfirmedReportingNotifications)))
      when(mockConfirmedDetailsConnector.contactDetails()).thenReturn(Future.successful(Some(ConfirmedContactDetails())))

      underTest.confirmBusinessDetailsFor(operatorId).futureValue mustBe ConfirmedDetails(false, false, false)
    }
  }

  ".confirmReportingNotificationsFor(...)" - {
    "must confirm reporting notifications and return correct ConfirmedDetails object" in {
      when(mockConfirmedDetailsConnector.save(ConfirmedReportingNotifications(operatorId))).thenReturn(Future.successful(Done))
      when(mockConfirmedDetailsConnector.businessDetails(operatorId)).thenReturn(Future.successful(Some(aConfirmedBusinessDetails)))
      when(mockConfirmedDetailsConnector.reportingNotifications(operatorId)).thenReturn(Future.successful(Some(aConfirmedReportingNotifications)))
      when(mockConfirmedDetailsConnector.contactDetails()).thenReturn(Future.successful(Some(ConfirmedContactDetails())))

      underTest.confirmReportingNotificationsFor(operatorId).futureValue mustBe ConfirmedDetails(true, true, true)
    }

    "must return correct ConfirmedDetails when failure during confirmation" in {
      when(mockConfirmedDetailsConnector.save(ConfirmedReportingNotifications(operatorId))).thenReturn(Future.failed
        (new RuntimeException()))
      when(mockConfirmedDetailsConnector.businessDetails(operatorId)).thenReturn(Future.successful(Some(aConfirmedBusinessDetails)))
      when(mockConfirmedDetailsConnector.reportingNotifications(operatorId)).thenReturn(Future.successful(Some(aConfirmedReportingNotifications)))
      when(mockConfirmedDetailsConnector.contactDetails()).thenReturn(Future.successful(Some(ConfirmedContactDetails())))

      underTest.confirmReportingNotificationsFor(operatorId).futureValue mustBe ConfirmedDetails(false, false, false)
    }
  }

  ".confirmContactDetailsFor(...)" - {
    "must confirm reporting notifications and return correct ConfirmedDetails object" in {
      when(mockConfirmedDetailsConnector.saveContactDetails()).thenReturn(Future.successful(Done))
      when(mockConfirmedDetailsConnector.businessDetails(operatorId)).thenReturn(Future.successful(Some(aConfirmedBusinessDetails)))
      when(mockConfirmedDetailsConnector.reportingNotifications(operatorId)).thenReturn(Future.successful(Some(aConfirmedReportingNotifications)))
      when(mockConfirmedDetailsConnector.contactDetails()).thenReturn(Future.successful(Some(ConfirmedContactDetails())))

      underTest.confirmContactDetailsFor(operatorId).futureValue mustBe ConfirmedDetails(true, true, true)
    }

    "must return correct ConfirmedDetails when failure during confirmation" in {
      when(mockConfirmedDetailsConnector.saveContactDetails()).thenReturn(Future.failed(new RuntimeException()))
      when(mockConfirmedDetailsConnector.businessDetails(operatorId)).thenReturn(Future.successful(Some(aConfirmedBusinessDetails)))
      when(mockConfirmedDetailsConnector.reportingNotifications(operatorId)).thenReturn(Future.successful(Some(aConfirmedReportingNotifications)))
      when(mockConfirmedDetailsConnector.contactDetails()).thenReturn(Future.successful(Some(ConfirmedContactDetails())))

      underTest.confirmContactDetailsFor(operatorId).futureValue mustBe ConfirmedDetails(false, false, false)
    }
  }
}
