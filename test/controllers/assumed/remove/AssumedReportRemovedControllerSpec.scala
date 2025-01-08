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

package controllers.assumed.remove

import base.SpecBase
import connectors.{PlatformOperatorConnector, SubscriptionConnector}
import models.operator.responses.PlatformOperator
import models.submission.{AssumedReportingSubmissionSummary, SubmissionStatus}
import models.subscription._
import models.operator._
import models.operator.{AddressDetails, ContactDetails}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.AssumedReportSummariesQuery
import viewmodels.checkAnswers.assumed.remove.AssumedReportRemovedSummaryList
import views.html.assumed.remove.AssumedReportRemovedView
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future
import java.time.{Clock, Instant, Year, ZoneId}

class AssumedReportRemovedControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val now = Instant.now()
  private val fixedClock = Clock.fixed(now, ZoneId.systemDefault())
  private val submission1 = AssumedReportingSubmissionSummary("submissionId1", "file1", "operatorId", "operatorName", Year.of(2024), now, SubmissionStatus.Success, Some("assuming"), Some("caseId1"), isDeleted = false)
  private val submission2 = AssumedReportingSubmissionSummary("submissionId2", "file2", "operatorId", "operatorName", Year.of(2025), now, SubmissionStatus.Success, Some("assuming"), Some("caseId2"), isDeleted = false)
  private val mockPlatformOperatorConnector = mock[PlatformOperatorConnector]
  private val mockSubscriptionConnector = mock[SubscriptionConnector]
  private val baseAnswers = emptyUserAnswers.set(AssumedReportSummariesQuery, Seq(submission1, submission2)).success.value

  "Assumed Report Removed Controller" - {

    val contact = IndividualContact(Individual("first", "last"), "tax1@team.com", None)

    val subscription :SubscriptionInfo = SubscriptionInfo(
      id = "dprsId",
      gbUser = true,
      tradingName = None,
      primaryContact = contact,
      secondaryContact = None
    )

    val operator = PlatformOperator(
      operatorId = "operatorId",
      operatorName = "operatorName",
      tinDetails = Nil,
      businessName = None,
      tradingName = None,
      primaryContactDetails = ContactDetails(None, "name", "tax2@team.com"),
      secondaryContactDetails = None,
      addressDetails = AddressDetails("line 1", None, None, None, None, Some("GB")),
      notifications = Nil
    )
    "must return OK and the correct view for a GET of a known reportable period" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[Clock].toInstance(fixedClock),
            bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
            bind[SubscriptionConnector].toInstance(mockSubscriptionConnector))
          .build()

      when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))
      when(mockPlatformOperatorConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))

      running(application) {
        given Messages = messages(application)

        val request = FakeRequest(routes.AssumedReportRemovedController.onPageLoad(operatorId, Year.of(2024)))

        val result = route(application, request).value

        val view = application.injector.instanceOf[AssumedReportRemovedView]
        val summaryList = AssumedReportRemovedSummaryList.list(submission1, now)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(summaryList, operatorId, Year.of(2024), subscription.primaryContact.email, operator.primaryContactDetails.emailAddress)(request, implicitly).toString
      }
    }
    
    "must return NOT_FOUND for an unknown reportable period" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[Clock].toInstance(fixedClock),
            bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
            bind[SubscriptionConnector].toInstance(mockSubscriptionConnector))
          .build()

      when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))
      when(mockPlatformOperatorConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))
      
      running(application) {
        given Messages = messages(application)

        val request = FakeRequest(routes.AssumedReportRemovedController.onPageLoad(operatorId, Year.of(2000)))

        val result = route(application, request).value

        status(result) mustEqual NOT_FOUND
      }
    }
  }
}
