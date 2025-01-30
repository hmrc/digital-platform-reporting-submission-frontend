/*
 * Copyright 2025 HM Revenue & Customs
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

package models.pageviews.assumed

import builders.AssumedReportingSubmissionSummaryBuilder.anAssumedReportingSubmissionSummary
import builders.PlatformOperatorBuilder.aPlatformOperator
import config.Constants.firstLegislativeYear
import forms.assumed.ViewAssumedReportsFormProvider
import models.operator.responses.PlatformOperator
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.i18n.*
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem

import java.time.Year

class ViewAssumedReportsViewModelSpec extends AnyFreeSpec with Matchers {

  implicit val messages: Messages = mock[Messages]

  private val form = ViewAssumedReportsFormProvider()()

  ".apply(...)" - {
    "must return correct object when" - {
      "platform operators is not empty" in {
        val platformOperators = Seq(aPlatformOperator)
        val summaries = Seq(anAssumedReportingSubmissionSummary)
        val platformOperatorSelectItems = Seq(
          SelectItem(value = Some("all"), text = messages("viewAssumedReports.platformOperator.allValues")),
          SelectItem(value = Some(aPlatformOperator.operatorId), text = aPlatformOperator.operatorName)
        )
        val reportingPeriodSelectItems = SelectItem(value = Some("0"), text = messages("viewAssumedReports.reportingPeriod.allValues")) ::
          (firstLegislativeYear to Year.now.getValue).map(year => SelectItem(value = Some(year.toString), text = year.toString)).toList

        ViewAssumedReportsViewModel.apply(
          platformOperators = platformOperators,
          assumedReportingSubmissionSummaries = summaries,
          currentYear = Year.now,
          form = form
        ) mustBe ViewAssumedReportsViewModel(
          form = form,
          summaries = summaries,
          platformOperatorSelectItems = platformOperatorSelectItems,
          reportingPeriodSelectItems = reportingPeriodSelectItems
        )
      }

      "platform operators is empty" in {
        val summaries = Seq(anAssumedReportingSubmissionSummary)
        val reportingPeriodSelectItems = SelectItem(value = Some("0"), text = messages("viewAssumedReports.reportingPeriod.allValues")) ::
          (firstLegislativeYear to Year.now.getValue).map(year => SelectItem(value = Some(year.toString), text = year.toString)).toList

        ViewAssumedReportsViewModel.apply(
          platformOperators = Seq.empty,
          assumedReportingSubmissionSummaries = summaries,
          currentYear = Year.now,
          form = form
        ) mustBe ViewAssumedReportsViewModel(
          form = form,
          summaries = summaries,
          platformOperatorSelectItems = Seq.empty,
          reportingPeriodSelectItems = reportingPeriodSelectItems
        )
      }
    }
  }
}
