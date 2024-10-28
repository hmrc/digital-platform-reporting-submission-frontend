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

package controllers.assumed.update

import base.SpecBase
import connectors.SubmissionConnector
import models.yearFormat
import models.submission.AssumedReportSummary
import pages.assumed.update.AssumingOperatorNamePage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AssumedReportSummaryQuery, PlatformOperatorNameQuery, ReportingPeriodQuery}
import viewmodels.PlatformOperatorSummary
import views.html.assumed.update.SubmissionConfirmationView

import java.time.Year

class SubmissionConfirmationControllerSpec extends SpecBase {

  "SubmissionConfirmation Controller" - {

    "onPageLoad" - {
      
      "must return OK and the correct view" in {
        
        val reportingPeriod = Year.of(2024)
        val answers =
          emptyUserAnswers
            .copy(reportingPeriod = Some(reportingPeriod))
            .set(AssumedReportSummaryQuery, AssumedReportSummary(operatorId, operatorName, "assumingOperator", reportingPeriod)).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()
        
        running(application) {
          val request = FakeRequest(routes.SubmissionConfirmationController.onPageLoad(operatorId, reportingPeriod))
          val result = route(application, request).value
          val view = application.injector.instanceOf[SubmissionConfirmationView]
          
          status(result) mustEqual OK
          contentAsString(result) mustEqual view(operatorId, "assumingOperator", operatorName, Year.of(2024))(request, messages(application)).toString
        }
      }
    }
  }
}
