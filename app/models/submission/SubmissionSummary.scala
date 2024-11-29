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

import controllers.submission.routes
import models.yearFormat
import models.submission.SubmissionStatus.*
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.govukfrontend.views.viewmodels.tag.Tag
import viewmodels.Link
import viewmodels.govuk.tag.*
import viewmodels.implicits.*

import java.time.{Instant, Year}

final case class SubmissionSummary(submissionId: String,
                                   fileName: String,
                                   operatorId: String,
                                   operatorName: String,
                                   reportingPeriod: Year,
                                   submissionDateTime: Instant,
                                   submissionStatus: SubmissionStatus,
                                   assumingReporterName: Option[String],
                                   submissionCaseId: Option[String],
                                   localDataExists: Boolean) {

  def link(implicit messages: Messages): Option[Link] = submissionStatus match {
    case Pending  => Some(Link(messages("viewSubmissions.refreshStatus"), routes.CheckFileController.onPageLoad(operatorId, submissionId).url))
    case Success  => Some(Link(messages("viewSubmissions.confirmation"), routes.SubmissionConfirmationController.onPageLoad(operatorId, submissionId).url))
    case Rejected => if (localDataExists) {
       Some(Link(messages("viewSubmissions.checkErrors"), routes.FileErrorsController.onPageLoad(operatorId, submissionId).url))
    } else {
      None
    }
  }

  def statusTag(implicit messages: Messages): Tag = submissionStatus match {
    case Success  => TagViewModel(messages("viewSubmissions.success")).green()
    case Pending  => TagViewModel(messages("viewSubmissions.pending")).yellow()
    case Rejected => TagViewModel(messages("viewSubmissions.rejected")).red()
  }
}

object SubmissionSummary {
  
  implicit lazy val format: OFormat[SubmissionSummary] = Json.format
}
