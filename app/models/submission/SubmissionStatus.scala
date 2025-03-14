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

package models.submission

import enumeratum.{EnumEntry, PlayEnum}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import viewmodels.govuk.checkbox._

sealed abstract class SubmissionStatus(override val entryName: String) extends EnumEntry

object SubmissionStatus extends PlayEnum[SubmissionStatus] {

  override val values: IndexedSeq[SubmissionStatus] = findValues

  case object Success extends SubmissionStatus("SUCCESS")
  case object Pending extends SubmissionStatus("PENDING")
  case object Rejected extends SubmissionStatus("REJECTED")
  
  def checkboxItems(implicit messages: Messages): Seq[CheckboxItem] =
    Seq(
      CheckboxItemViewModel(
        content = Text(messages(s"viewSubmissions.status.${Success.entryName}")),
        fieldId = "statuses",
        index   = 0,
        value   = Success.entryName
      ),
      CheckboxItemViewModel(
        content = Text(messages(s"viewSubmissions.status.${Rejected.entryName}")),
        fieldId = "statuses",
        index   = 1,
        value   = Rejected.entryName
      )
    )
}
