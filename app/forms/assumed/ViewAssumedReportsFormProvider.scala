/*
 * Copyright 2025 HM Revenue & Customs
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

package forms.assumed

import enumeratum.Forms.enumMapping
import forms.mappings.Mappings
import models.submission.{SortBy, SortOrder}
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}

import java.time.Year
import javax.inject.Inject

class ViewAssumedReportsFormProvider @Inject() extends Mappings {

  def apply(): Form[ViewAssumedReportsFormData] = Form(mapping(
    "sortBy" -> optional(enumMapping(SortBy)),
    "sortOrder" -> optional(enumMapping(SortOrder)),
    "operatorId" -> optional(text("")),
    "reportingPeriod" -> optional(int(""))
  )(applyForm)(unapplyForm))

  private def applyForm: (Option[SortBy], Option[SortOrder], Option[String], Option[Int]) => ViewAssumedReportsFormData = {
    (sortBy, sortOrder, operatorId, reportingPeriod) =>
      ViewAssumedReportsFormData(
        sortBy.getOrElse(SortBy.SubmissionDate),
        sortOrder.getOrElse(SortOrder.Descending),
        operatorId match {
          case Some("all") => None
          case other => other
        },
        reportingPeriod match {
          case Some(x) if x > 0 => Some(Year.of(x))
          case _ => None
        }
      )
  }

  private def unapplyForm: ViewAssumedReportsFormData => Option[(Option[SortBy], Option[SortOrder], Option[String], Option[Int])] = { filter =>
    Some((
      if (filter.sortBy == SortBy.SubmissionDate) None else Some(filter.sortBy),
      if (filter.sortOrder == SortOrder.Descending) None else Some(filter.sortOrder),
      filter.operatorId,
      filter.reportingPeriod.map(_.getValue)
    ))
  }
}
