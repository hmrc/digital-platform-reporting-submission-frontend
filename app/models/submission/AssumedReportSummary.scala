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

import java.time.Year
import models.{UserAnswers, yearFormat}
import pages.assumed.update.AssumingOperatorNamePage
import play.api.libs.json.{Json, OFormat}
import queries.{PlatformOperatorNameQuery, PlatformOperatorSummaryQuery, ReportingPeriodQuery}

final case class AssumedReportSummary(operatorId: String,
                                      operatorName: String,
                                      assumingOperatorName: String,
                                      reportingPeriod: Year)

object AssumedReportSummary {
  
  implicit lazy val format: OFormat[AssumedReportSummary] = Json.format
  
  def apply(answers: UserAnswers): Option[AssumedReportSummary] =
    for {
      assumingOperatorName <- answers.get(AssumingOperatorNamePage)
      operatorName         <- answers.get(PlatformOperatorNameQuery).orElse(answers.get(PlatformOperatorSummaryQuery).map(_.operatorName))
      reportingPeriod      <- answers.get(ReportingPeriodQuery)
    } yield AssumedReportSummary(answers.operatorId, operatorName, assumingOperatorName, reportingPeriod)
}
