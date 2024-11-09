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

package controllers.submission

import base.SpecBase
import connectors.{PlatformOperatorConnector, SubmissionConnector}
import forms.ViewSubmissionsFormProvider
import models.ViewSubmissionsFilter
import models.operator.{AddressDetails, ContactDetails}
import models.operator.responses.{PlatformOperator, ViewPlatformOperatorsResponse}
import models.submission.SortBy.SubmissionDate
import models.submission.SortOrder.Descending
import models.submission.{SubmissionStatus, SubmissionSummary, SubmissionsSummary}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.ViewSubmissionsViewModel
import views.html.submission.ViewSubmissionsView

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, Year, ZoneId}
import scala.concurrent.Future

class ViewSubmissionsControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSubmissionConnector = mock[SubmissionConnector]
  private val mockPlatformOperatorConnector = mock[PlatformOperatorConnector]
  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
  private val thisYear = Year.now(stubClock)
  private val form = ViewSubmissionsFormProvider()()

  override def beforeEach(): Unit = {
    Mockito.reset(mockSubmissionConnector, mockPlatformOperatorConnector)
    super.beforeEach()
  }

  "ViewSubmissions Controller" - {

    "must return OK and the correct view for a GET" in {

      val submissionSummary = SubmissionSummary(
        submissionId = "submissionId",
        fileName = "filename",
        operatorId = "operatorId",
        operatorName = "operatorName",
        reportingPeriod = thisYear,
        submissionDateTime = instant,
        submissionStatus = SubmissionStatus.Success,
        assumingReporterName = None,
        submissionCaseId = Some("caseId")
      )
      val summary = SubmissionsSummary(Seq(submissionSummary), 1, true, 1)
      
      val platformOperator = PlatformOperator(
        operatorId = "operatorId",
        operatorName = "operatorName",
        tinDetails = Nil,
        businessName = None,
        tradingName = None,
        primaryContactDetails = ContactDetails(None, "name", "email"),
        secondaryContactDetails = None,
        addressDetails = AddressDetails("line 1", None, None, None, None, Some("GB")),
        notifications = Nil
      )
      val platformOperatorResponse = ViewPlatformOperatorsResponse(Seq(platformOperator))
      
      val defaultFilter = ViewSubmissionsFilter(
        pageNumber = 1,
        sortBy = SubmissionDate,
        sortOrder = Descending,
        statuses = Set.empty,
        operatorId = None,
        reportingPeriod = None
      )

      when(mockSubmissionConnector.listDeliveredSubmissions(any())(using any())).thenReturn(Future.successful(Some(summary)))
      when(mockPlatformOperatorConnector.viewPlatformOperators(any())).thenReturn(Future.successful(platformOperatorResponse))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[SubmissionConnector].toInstance(mockSubmissionConnector),
          bind[PlatformOperatorConnector].toInstance(mockPlatformOperatorConnector),
          bind[Clock].toInstance(stubClock)
        )
        .build()

      running(application) {
        val request = FakeRequest(routes.ViewSubmissionsController.onPageLoad())

        val result = route(application, request).value

        implicit val msgs: Messages = messages(application)
        val view = application.injector.instanceOf[ViewSubmissionsView]
        val viewModel = ViewSubmissionsViewModel(Some(summary), Seq(platformOperator), defaultFilter, thisYear)(request)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, viewModel)(request, implicitly).toString
      }
    }
  }
}
