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
import models.submission.{SubmissionStatus, AssumedReportingSubmissionSummary}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.AssumedReportSummariesQuery
import viewmodels.checkAnswers.assumed.remove.AssumedReportRemovedSummaryList
import views.html.assumed.remove.AssumedReportRemovedView

import java.time.{Clock, Instant, Year, ZoneId}

class AssumedReportRemovedControllerSpec extends SpecBase {

  private val now = Instant.now()
  private val fixedClock = Clock.fixed(now, ZoneId.systemDefault())
  private val submission1 = AssumedReportingSubmissionSummary("submissionId1", "file1", "operatorId", "operatorName", Year.of(2024), now, SubmissionStatus.Success, Some("assuming"), Some("caseId1"), isDeleted = false)
  private val submission2 = AssumedReportingSubmissionSummary("submissionId2", "file2", "operatorId", "operatorName", Year.of(2025), now, SubmissionStatus.Success, Some("assuming"), Some("caseId2"), isDeleted = false)

  private val baseAnswers = emptyUserAnswers.set(AssumedReportSummariesQuery, Seq(submission1, submission2)).success.value

  "Assumed Report Removed Controller" - {

    "must return OK and the correct view for a GET of a known reporting period" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[Clock].toInstance(fixedClock))
          .build()

      running(application) {
        given Messages = messages(application)

        val request = FakeRequest(routes.AssumedReportRemovedController.onPageLoad(operatorId, Year.of(2024)))

        val result = route(application, request).value

        val view = application.injector.instanceOf[AssumedReportRemovedView]
        val summaryList = AssumedReportRemovedSummaryList.list(submission1, now)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(summaryList, operatorId, Year.of(2024))(request, implicitly).toString
      }
    }
    
    "must return NOT_FOUND for an unknown reporting period" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[Clock].toInstance(fixedClock))
          .build()

      running(application) {
        given Messages = messages(application)

        val request = FakeRequest(routes.AssumedReportRemovedController.onPageLoad(operatorId, Year.of(2000)))

        val result = route(application, request).value

        status(result) mustEqual NOT_FOUND
      }
    }
  }
}
