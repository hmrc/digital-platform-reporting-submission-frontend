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

import config.Constants.firstLegislativeYear
import forms.assumed.ViewAssumedReportsFormData
import models.operator.responses.PlatformOperator
import models.submission.AssumedReportingSubmissionSummary
import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem

import java.time.Year

case class ViewAssumedReportsViewModel(form: Form[ViewAssumedReportsFormData],
                                       summaries: Seq[AssumedReportingSubmissionSummary],
                                       platformOperatorSelectItems: Seq[SelectItem],
                                       reportingPeriodSelectItems: Seq[SelectItem])

object ViewAssumedReportsViewModel {

  def apply(platformOperators: Seq[PlatformOperator],
            assumedReportingSubmissionSummaries: Seq[AssumedReportingSubmissionSummary],
            currentYear: Year,
            form: Form[ViewAssumedReportsFormData])
           (implicit messages: Messages): ViewAssumedReportsViewModel = {
    ViewAssumedReportsViewModel(
      form = form,
      summaries = assumedReportingSubmissionSummaries,
      platformOperatorSelectItems = platformOperatorSelectItems(platformOperators),
      reportingPeriodSelectItems = reportingPeriodSelectItems(currentYear)
    )
  }

  private def platformOperatorSelectItems(operators: Seq[PlatformOperator])
                                         (implicit messages: Messages): Seq[SelectItem] = operators match {
    case Nil => Nil
    case _ => SelectItem(value = Some("all"), text = messages("viewAssumedReports.platformOperator.allValues")) ::
      operators.map(operator => SelectItem(value = Some(operator.operatorId), text = operator.operatorName)).toList
  }


  private def reportingPeriodSelectItems(currentYear: Year)
                                        (implicit messages: Messages): Seq[SelectItem] =
    SelectItem(value = Some("0"), text = messages("viewAssumedReports.reportingPeriod.allValues")) ::
      (firstLegislativeYear to currentYear.getValue)
        .map(year => SelectItem(value = Some(year.toString), text = year.toString)).toList
}
