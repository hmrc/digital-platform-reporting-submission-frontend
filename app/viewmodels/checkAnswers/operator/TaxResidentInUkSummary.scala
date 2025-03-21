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

import models.Country.UnitedKingdom
import models.operator.responses.PlatformOperator
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object TaxResidentInUkSummary {

  def row(operator: PlatformOperator)(implicit messages: Messages): Option[SummaryListRow] = {
    
    val value = if (operator.tinDetails.isEmpty) {
      None
    } else {
      if (operator.tinDetails.map(_.issuedBy).contains(UnitedKingdom.code)) Some("site.yes") else Some("site.no")
    }

    value.map { answer =>
      SummaryListRowViewModel(
        key = messages("taxResidentInUk.checkYourAnswersLabel", operator.operatorName),
        value = ValueViewModel(answer),
        actions = Nil
      )
    }
  }
}
