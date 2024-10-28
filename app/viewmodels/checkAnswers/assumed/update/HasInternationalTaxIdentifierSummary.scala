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
import pages.assumed.update.{AssumingOperatorNamePage, HasInternationalTaxIdentifierPage, TaxResidencyCountryPage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

import java.time.Year

object HasInternationalTaxIdentifierSummary  {

  def row(reportingPeriod: Year, answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    for {
      answer               <- answers.get(HasInternationalTaxIdentifierPage)
      taxResidencyCountry  <- answers.get(TaxResidencyCountryPage)
      assumingOperatorName <- answers.get(AssumingOperatorNamePage)
    } yield {

      val value = if (answer) "site.yes" else "site.no"

      SummaryListRowViewModel(
        key     = messages("hasInternationalTaxIdentifier.checkYourAnswersLabel", assumingOperatorName, taxResidencyCountry.name),
        value   = ValueViewModel(value),
        actions = Seq(
          ActionItemViewModel("site.change", routes.HasInternationalTaxIdentifierController.onPageLoad(answers.operatorId, reportingPeriod).url)
            .withVisuallyHiddenText(messages("hasInternationalTaxIdentifier.change.hidden", assumingOperatorName, taxResidencyCountry.name))
        )
      )
    }
}
