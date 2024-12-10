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

package models.recentsubmissions

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

final case class RecentSubmissionDetails(operatorDetails: Map[String, OperatorSubmissionDetails],
                                         yourContactDetailsCorrect: Option[Boolean])

object RecentSubmissionDetails {

  given format: OFormat[RecentSubmissionDetails] = OFormat(reads, writes)

  private lazy val reads: Reads[RecentSubmissionDetails] = (
    (__ \ "operatorDetails").read[Map[String, OperatorSubmissionDetails]] and
      (__ \ "yourContactDetailsCorrect").readNullable[Boolean]
    )(RecentSubmissionDetails.apply)

  private lazy val writes: OWrites[RecentSubmissionDetails] = (
    (__ \ "operatorDetails").write[Map[String, OperatorSubmissionDetails]] and
      (__ \ "yourContactDetailsCorrect").writeNullable[Boolean]
    )(o => Tuple.fromProductTyped(o))
}