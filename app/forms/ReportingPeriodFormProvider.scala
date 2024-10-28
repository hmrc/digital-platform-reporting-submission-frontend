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

import config.Constants
import forms.mappings.Mappings
import play.api.data.Form

import java.time.{Clock, LocalDate, Year}
import javax.inject.Inject

class ReportingPeriodFormProvider @Inject()(clock: Clock) extends Mappings {

  def apply(): Form[Year] = {

    val maxYear = LocalDate.now(clock).getYear
    val minYear = Constants.firstLegislativeYear

    Form(
      "value" -> int("reportingPeriod.error.required", "reportingPeriod.error.wholeNumber", "reportingPeriod.error.nonNumeric")
        .verifying(minimumValue(minYear, "reportingPeriod.error.belowMinimum", args = minYear.toString))
        .verifying(maximumValue(maxYear, "reportingPeriod.error.aboveMaximum", args = maxYear.toString))
        .transform[Year](year => Year.of(year), year => year.getValue)
    )
  }
}
