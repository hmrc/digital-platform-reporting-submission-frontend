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

import models.ViewSubmissionsFilter
import play.api.libs.json.{Json, OWrites}

final case class ViewSubmissionsRequest(assumedReporting: Boolean,
                                        pageNumber: Int,
                                        sortBy: SortBy,
                                        sortOrder: SortOrder,
                                        reportingPeriod: Option[Int],
                                        operatorId: Option[String],
                                        statuses: Seq[SubmissionStatus],
                                        fileName: Option[String] = None)

object ViewSubmissionsRequest {
  
  implicit lazy val writes: OWrites[ViewSubmissionsRequest] = Json.writes

  def apply(filter: ViewSubmissionsFilter): ViewSubmissionsRequest = ViewSubmissionsRequest(
    assumedReporting = false,
    pageNumber       = filter.pageNumber,
    sortBy           = filter.sortBy,
    sortOrder        = filter.sortOrder,
    reportingPeriod  = filter.reportingPeriod.map(_.getValue),
    operatorId       = filter.operatorId,
    statuses         = filter.statuses.toSeq
  )
}
