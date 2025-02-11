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

import models.{CountriesList, yearFormat}
import play.api.libs.functional.syntax.*
import play.api.libs.json.*

import java.time.Year

final case class AssumedReportingSubmissionRequest(operatorId: String,
                                                   assumingOperator: AssumingPlatformOperator,
                                                   reportingPeriod: Year)

object AssumedReportingSubmissionRequest {
  given format(using countriesList: CountriesList): OFormat[AssumedReportingSubmissionRequest] = {
    val assumingOperatorFormat = AssumingPlatformOperator.format
    (
      (__ \ "operatorId").format[String] and
        (__ \ "assumingOperator").format[AssumingPlatformOperator](assumingOperatorFormat) and
        (__ \ "reportingPeriod").format[Year]
      )(AssumedReportingSubmissionRequest.apply, arg => (arg.operatorId, arg.assumingOperator, arg.reportingPeriod))
  }
}