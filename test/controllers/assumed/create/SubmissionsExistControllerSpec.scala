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
import models.yearFormat
import pages.assumed.create.ReportingPeriodPage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PlatformOperatorSummaryQuery
import viewmodels.PlatformOperatorSummary
import views.html.assumed.create.SubmissionsExistView

import java.time.Year

class SubmissionsExistControllerSpec extends SpecBase {

  "SubmissionsExist Controller" - {
    "onPageLoad" - {
      "must return OK and the correct view" in {
        val year2024 = 2024
        val reportingPeriod = Year.of(year2024)
        val platformOperator = PlatformOperatorSummary(operatorId, operatorName, "primaryContactName", "test@test.com", hasReportingNotifications = true)
        val answers =
          emptyUserAnswers
            .set(PlatformOperatorSummaryQuery, platformOperator).success.value
            .set(ReportingPeriodPage, reportingPeriod).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(routes.SubmissionsExistController.onPageLoad(operatorId))
          val result = route(application, request).value
          val view = application.injector.instanceOf[SubmissionsExistView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(platformOperator, reportingPeriod)(request, messages(application)).toString
        }
      }
    }
  }
}