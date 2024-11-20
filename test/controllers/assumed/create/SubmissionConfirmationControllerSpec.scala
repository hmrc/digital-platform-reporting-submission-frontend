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
import models.submission.AssumedReportSummary
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.AssumedReportSummaryQuery
import viewmodels.checkAnswers.assumed.create.AssumedReportCreatedSummary
import views.html.assumed.create.SubmissionConfirmationView

import java.time.{Clock, Instant, Year, ZoneId}


class SubmissionConfirmationControllerSpec extends SpecBase {

  private val now = Instant.now()
  private val fixedClock = Clock.fixed(now, ZoneId.systemDefault())

  "SubmissionConfirmation Controller" - {

    "onPageLoad" - {

      "must return OK and the correct view" in {

        val reportingPeriod = Year.of(2024)
        val summary = AssumedReportSummary(operatorId, operatorName, "assumingOperator", reportingPeriod)
        val answers =
          emptyUserAnswers
            .copy(reportingPeriod = Some(reportingPeriod))
            .set(AssumedReportSummaryQuery, summary).success.value

        val application =
          applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[Clock].toInstance(fixedClock))
            .build()

        running(application) {
          given Messages = messages(application)
          
          val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, reportingPeriod))
          val result = route(application, request).value
          val view = application.injector.instanceOf[SubmissionConfirmationView]
          val summaryList = AssumedReportCreatedSummary.list(summary, now)
          
          status(result) mustEqual OK
          contentAsString(result) mustEqual view(operatorId, summaryList)(request, implicitly).toString
        }
      }
      

    }
  }
}