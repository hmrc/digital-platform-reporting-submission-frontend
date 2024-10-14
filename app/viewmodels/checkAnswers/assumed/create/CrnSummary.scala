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

import controllers.assumed.create.routes
import models.{CheckMode, UserAnswers}
import pages.assumed.create.{AssumingOperatorNamePage, CrnPage}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object CrnSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    for {
      answer               <- answers.get(CrnPage)
      assumingOperatorName <- answers.get(AssumingOperatorNamePage)
    } yield {

      SummaryListRowViewModel(
        key     = "crn.checkYourAnswersLabel",
        value   = ValueViewModel(HtmlFormat.escape(answer).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.CrnController.onPageLoad(CheckMode, answers.operatorId).url)
            .withVisuallyHiddenText(messages("crn.change.hidden", assumingOperatorName))
        )
      )
    }
}
