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

import models.operator.TinDetails
import models.{Country, ExtendedCountriesList}
import play.api.libs.functional.syntax.*
import play.api.libs.json.*

final case class AssumingPlatformOperator(
                                           name: String,
                                           residentCountry: Country,
                                           tinDetails: Seq[TinDetails],
                                           registeredCountry: Country,
                                           address: String
                                         )

object AssumingPlatformOperator {

  private given Reads[Country] =
    JsPath.read[String].flatMap { countryCode =>
      (new ExtendedCountriesList).allCountries.find(_.code == countryCode)
        .map(Reads.pure)
        .getOrElse(Reads.failed("Unrecognised country code"))
    }

  private lazy val reads: Reads[AssumingPlatformOperator] = (
    (__ \ "name").read[String] and
      (__ \ "residentCountry").read[Country] and
      (__ \ "tinDetails").read[Seq[TinDetails]] and
      (__ \ "registeredCountry").read[Country] and
      (__ \ "address").read[String]
    )(AssumingPlatformOperator.apply)

  private lazy val writes: OWrites[AssumingPlatformOperator] = (
    (__ \ "name").write[String] and
      (__ \ "residentCountry").write[String] and
      (__ \ "tinDetails").write[Seq[TinDetails]] and
      (__ \ "registeredCountry").write[String] and
      (__ \ "address").write[String]
    )(operator => (operator.name, operator.residentCountry.code, operator.tinDetails, operator.registeredCountry.code, operator.address))

  given OFormat[AssumingPlatformOperator] = OFormat(reads, writes)
}
