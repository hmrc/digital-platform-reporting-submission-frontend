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

package viewmodels.checkAnswers.assumed.update

import controllers.assumed.update.routes
import models.UserAnswers
import pages.assumed.update.{AssumingOperatorNamePage, InternationalAddressPage}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object InternationalAddressSummary  {

  def row(caseId: String, answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    for {
      answer               <- answers.get(InternationalAddressPage)
      assumingOperatorName <- answers.get(AssumingOperatorNamePage)
    } yield {
        
      val value: String = Seq(
        Some(HtmlFormat.escape(answer.line1)),
        answer.line2.map(HtmlFormat.escape),
        Some(HtmlFormat.escape(answer.city)),
        answer.region.map(HtmlFormat.escape),
        Some(HtmlFormat.escape(answer.postal)),
        Some(HtmlFormat.escape(answer.country.name))
      ).flatten.map(_.toString).mkString("<br/>")
      
      SummaryListRowViewModel(
        key     = messages("internationalAddress.checkYourAnswersLabel", assumingOperatorName),
        value   = ValueViewModel(HtmlContent(value)),
        actions = Seq(
          ActionItemViewModel("site.change", routes.InternationalAddressController.onPageLoad(answers.operatorId, caseId).url)
            .withVisuallyHiddenText(messages("internationalAddress.change.hidden", assumingOperatorName))
        )
      )
    }
}
