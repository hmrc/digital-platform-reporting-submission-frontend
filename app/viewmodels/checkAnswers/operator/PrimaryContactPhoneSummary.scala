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

import models.operator.responses.PlatformOperator
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object PrimaryContactPhoneSummary {

  def row(operator: PlatformOperator)(implicit messages: Messages): Option[SummaryListRow] = {
    operator.primaryContactDetails.phoneNumber.map { phone =>
      SummaryListRowViewModel(
        key = "primaryContactPhone.checkYourAnswersLabel",
        value = ValueViewModel(phone),
        actions = Nil
      )
    }
  }
}
