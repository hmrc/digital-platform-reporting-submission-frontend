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

package viewmodels.checkAnswers.assumed.create

import models.submission.AssumedReportSummary
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.DateTimeFormats
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

import java.time.Instant

object AssumedReportCreatedSummary {

  def list(summary: AssumedReportSummary, updated: Instant)(implicit messages: Messages): SummaryList =
    SummaryList(rows = Seq(
      SummaryListRowViewModel(
        key = messages("assumedReportSummary.platformOperator"),
        value = ValueViewModel(HtmlFormat.escape(summary.operatorName).toString)
      ),
      SummaryListRowViewModel(
        key = messages("assumedReportSummary.platformOperatorId"),
        value = ValueViewModel(HtmlFormat.escape(summary.operatorId).toString)
      ),
      SummaryListRowViewModel(
        key = messages("assumedReportSummary.reportingPeriod"),
        value = ValueViewModel(summary.reportingPeriod.toString)
      ),
      SummaryListRowViewModel(
        key = messages("assumedReportSummary.assumingOperator"),
        value = ValueViewModel(HtmlFormat.escape(summary.assumingOperatorName).toString)
      ),
      SummaryListRowViewModel(
        key = messages("assumedReportSummary.createdAt"),
        value = ValueViewModel(DateTimeFormats.formatInstant(updated, DateTimeFormats.fullDateTimeFormatter))
      )
    ))
}
