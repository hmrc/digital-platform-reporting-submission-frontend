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

package forms

import enumeratum.Forms.enumMapping
import forms.mappings.Mappings
import models.ViewSubmissionsFilter
import models.submission.{SortBy, SortOrder, SubmissionStatus}
import play.api.data.Form
import play.api.data.Forms.*

import javax.inject.Inject
import java.time.Year

class ViewSubmissionsFormProvider @Inject() extends Mappings {

  def apply(): Form[ViewSubmissionsFilter] = Form(
    mapping(
      "page"            -> optional(int("")),
      "sortBy"          -> optional(enumMapping(SortBy)),
      "sortOrder"       -> optional(enumMapping(SortOrder)),
      "statuses"        -> optional(set(enumMapping(SubmissionStatus))),
      "operatorId"      -> optional(text("")),
      "reportingPeriod" -> optional(int(""))
    )((page, sortBy, sortOrder, statuses, operatorId, reportingPeriod) =>
      ViewSubmissionsFilter(
        page.getOrElse(1),
        sortBy.getOrElse(SortBy.SubmissionDate),
        sortOrder.getOrElse(SortOrder.Descending),
        statuses.getOrElse(Set.empty),
        operatorId,
        reportingPeriod.map(x => Year.of(x))
      )
    )(filter => Some(
      (
        if (filter.pageNumber == 1) None else Some(filter.pageNumber),
        if (filter.sortBy == SortBy.SubmissionDate) None else Some(filter.sortBy),
        if (filter.sortOrder == SortOrder.Descending) None else Some(filter.sortOrder),
        if (filter.statuses.isEmpty) None else Some(filter.statuses),
        filter.operatorId,
        filter.reportingPeriod.map(_.getValue)
      )
    ))
  )
}
