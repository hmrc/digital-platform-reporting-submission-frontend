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

import builders.ViewAssumedReportsFormDataBuilder.aViewAssumedReportsFormData
import forms.behaviours.StringFieldBehaviours
import models.submission.{SortBy, SortOrder}
import org.scalatest.freespec.AnyFreeSpec

import java.time.Year

class ViewAssumedReportsFormProviderSpec extends StringFieldBehaviours {

  private val underTest = ViewAssumedReportsFormProvider()()

  "form" - {
    "must bind with default values when no data is provided" in {
      val result = underTest.bind(Map.empty[String, String])
      result.value.value mustEqual aViewAssumedReportsFormData.copy(sortBy = SortBy.SubmissionDate, sortOrder = SortOrder.Descending)
      result.hasErrors mustBe false
    }

    "must bind when valid values are provided" in {
      val result = underTest.bind(Map(
        "sortBy" -> SortBy.ReportingPeriod.entryName,
        "sortOrder" -> SortOrder.Ascending.entryName,
        "operatorId" -> "some-order-id",
        "reportingPeriod" -> s"${Year.now().getValue}"
      ))
      result.value.value mustEqual aViewAssumedReportsFormData.copy(
        sortBy = SortBy.ReportingPeriod,
        sortOrder = SortOrder.Ascending,
        operatorId = Some("some-order-id"),
        reportingPeriod = Some(Year.now())
      )
      result.hasErrors mustBe false
    }

    "must bind a reportable period of 0 as None" in {
      val result = underTest.bind(Map("reportingPeriod" -> "0"))
      result.value.value.reportingPeriod must not be defined
    }

    "must bind an operator Id of `all` as None" in {
      val result = underTest.bind(Map("operatorId" -> "all"))
      result.value.value.operatorId must not be defined
    }

    "must not bind when invalid values are provided" in {
      val result = underTest.bind(Map(
        "sortBy" -> "invalid value",
        "sortOrder" -> "invalid value",
        "reportingPeriod" -> "invalid value"
      ))
      result.hasErrors mustBe true
      result.errors.size mustEqual 3
    }

    "must unbind default values" in {
      val formData = aViewAssumedReportsFormData.copy(
        sortBy = SortBy.SubmissionDate,
        sortOrder = SortOrder.Descending,
        operatorId = None,
        reportingPeriod = None
      )
      val result = underTest.fill(formData)
      result.data mustEqual Map.empty[String, String]
    }
  }
}
