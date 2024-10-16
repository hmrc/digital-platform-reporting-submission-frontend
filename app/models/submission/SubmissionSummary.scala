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

import controllers.assumed.remove.{routes as removeRoutes}
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import viewmodels.Link

import java.time.Instant

final case class SubmissionSummary(submissionId: String,
                                   fileName: String,
                                   operatorId: String,
                                   operatorName: String,
                                   reportingPeriod: String,
                                   submissionDateTime: Instant,
                                   submissionStatus: SubmissionStatus,
                                   assumingReporterName: Option[String],
                                   submissionCaseId: Option[String]) {

  private def removeAssumedReportLink(implicit messages: Messages): Option[Link] =
    submissionCaseId.map { caseId =>
      Link(messages("site.delete"), removeRoutes.RemoveAssumedReportController.onPageLoad(operatorId, caseId).url)
    }

  // TODO: Add update link when pages exist
  // TODO: Consider status when deciding which links to show?
  def links(implicit messages: Messages): Seq[Link] = Seq(
    removeAssumedReportLink
  ).flatten
}

object SubmissionSummary {
  
  implicit lazy val format: OFormat[SubmissionSummary] = Json.format
}
