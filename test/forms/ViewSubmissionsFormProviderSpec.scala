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

import forms.behaviours.StringFieldBehaviours
import models.ViewSubmissionsFilter
import models.submission.SubmissionStatus.{Pending, Success}
import models.submission.{SortBy, SortOrder}

import java.time.Year

class ViewSubmissionsFormProviderSpec extends StringFieldBehaviours {

  private val form = new ViewSubmissionsFormProvider()()
  
  "form" - {
    
    "must bind with default values when no data is provided" in {
      
      val result = form.bind(Map.empty[String, String])
      result.value.value mustEqual ViewSubmissionsFilter(1, SortBy.SubmissionDate, SortOrder.Descending, Set.empty, None, None)
      result.hasErrors mustBe false
    }

    "must bind when valid values are provided" in {

      val result = form.bind(Map(
        "page" -> "2",
        "sortBy" -> SortBy.ReportingPeriod.entryName,
        "sortOrder" -> SortOrder.Ascending.entryName,
        "statuses[0]" -> "SUCCESS",
        "statuses[1]" -> "PENDING",
        "operatorId" -> "foo",
        "reportingPeriod" -> "2024"
      ))
      result.value.value mustEqual ViewSubmissionsFilter(2, SortBy.ReportingPeriod, SortOrder.Ascending, Set(Success, Pending), Some("foo"), Some(Year.of(2024)))
      result.hasErrors mustBe false
    }

    "must not bind when invalid values are provided" in {

      val result = form.bind(Map(
        "page" -> "invalid value",
        "sortBy" -> "invalid value",
        "sortOrder" -> "invalid value",
        "statuses[0]" -> "invalid value",
        "reportingPeriod" -> "invalid value"
      ))
      result.hasErrors mustBe true
      result.errors.size mustEqual 5
    }
  }
}
