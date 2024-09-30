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

import forms.behaviours.IntFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

import java.time.{Clock, Instant, LocalDate, ZoneOffset}

class ReportingPeriodFormProviderSpec extends IntFieldBehaviours {

  private val requiredKey = "reportingPeriod.error.required"

  private val clock = Clock.fixed(Instant.parse("2100-12-31T00:00:00Z"), ZoneOffset.UTC)
  private val form = new ReportingPeriodFormProvider(clock)()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Gen.choose(LocalDate.now(clock).getYear, LocalDate.now(clock).getYear).map(_.toString)
    )

    behave like mandatoryField(
      form,
      fieldName,
      FormError(fieldName, requiredKey)
    )

    behave like intFieldWithMinimum(
      form,
      fieldName,
      2024,
      FormError(fieldName, "reportingPeriod.error.belowMinimum", Seq(2024, "2024"))
    )

    val maxYear = LocalDate.now(clock).getYear
    behave like intFieldWithMaximum(
      form,
      fieldName,
      maxYear,
      FormError(fieldName, "reportingPeriod.error.aboveMaximum", Seq(maxYear, maxYear.toString))
    )
  }
}
