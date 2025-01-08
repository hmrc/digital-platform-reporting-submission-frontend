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

package controllers.assumed.create

import base.SpecBase
import connectors.{PlatformOperatorConnector, SubscriptionConnector}
import models.operator.responses.PlatformOperator
import models.operator.{AddressDetails, ContactDetails}
import models.submission.AssumedReportSummary
import models.subscription.{Individual, IndividualContact, SubscriptionInfo}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import queries.AssumedReportSummaryQuery
import viewmodels.checkAnswers.assumed.create.AssumedReportCreatedSummary
import views.html.assumed.create.SubmissionConfirmationView
import scala.concurrent.Future

import java.time.{Clock, Instant, Year, ZoneId}


class SubmissionConfirmationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val now = Instant.now()
  private val fixedClock = Clock.fixed(now, ZoneId.systemDefault())
  
  private val mockPlatformOperatorConnector = mock[PlatformOperatorConnector]
  private val mockSubscriptionConnector = mock[SubscriptionConnector]
  
  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset( mockSubscriptionConnector, mockPlatformOperatorConnector)
  }
  "SubmissionConfirmation Controller" - {

    "onPageLoad" - {
      val contact = IndividualContact(Individual("first", "last"), "tax1@team.com", None)
      val subscription = SubscriptionInfo(
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
      "must return OK and the correct view" in {

        val reportingPeriod = Year.of(2024)
        val summary = AssumedReportSummary(operatorId, operatorName, "assumingOperator", reportingPeriod)
        val answers =
          emptyUserAnswers
            .copy(reportingPeriod = Some(reportingPeriod))
            .set(AssumedReportSummaryQuery, summary).success.value

        val application =
          applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[Clock].toInstance(fixedClock),
              bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
              bind[SubscriptionConnector].toInstance(mockSubscriptionConnector))
            .build()
            
        when(mockSubscriptionConnector.getSubscription(any())).thenReturn(Future.successful(subscription))
        when(mockPlatformOperatorConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))

        running(application) {
          given Messages = messages(application)
          
          val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, reportingPeriod))
          val result = route(application, request).value
          val view = application.injector.instanceOf[SubmissionConfirmationView]
          val summaryList = AssumedReportCreatedSummary.list(summary, now)
          
          status(result) mustEqual OK
          contentAsString(result) mustEqual view(operatorId, summaryList, subscription.primaryContact.email, operator.primaryContactDetails.emailAddress)(request, implicitly).toString
        }
      }
      

    }
  }
}