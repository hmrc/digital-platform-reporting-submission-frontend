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

package viewmodels.checkAnswers.operator

import models.Country
import models.operator.responses.PlatformOperator
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object AddressSummary {

  def row(operator: PlatformOperator)(implicit messages: Messages): Option[SummaryListRow] = {

    val value = Seq(
      Some(HtmlFormat.escape(operator.addressDetails.line1).toString),
      operator.addressDetails.line2.map(HtmlFormat.escape(_).toString),
      operator.addressDetails.line3.map(HtmlFormat.escape(_).toString),
      operator.addressDetails.line4.map(HtmlFormat.escape(_).toString),
      operator.addressDetails.postCode.map(HtmlFormat.escape(_).toString),
      operator.addressDetails.countryCode.flatMap(code => Country.allCountries.map(_.code).find(_ == code))
    ).flatten.mkString("<br/>")

    Some(SummaryListRowViewModel(
      key = "address.checkYourAnswersLabel",
      value = ValueViewModel(HtmlContent(value)),
      actions = Nil
    ))
  }
}
