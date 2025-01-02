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
import models.operator.TinType.Other
import models.{CountriesList, Country, DefaultCountriesList}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, Json}

class AssumingPlatformOperatorSpec extends AnyFreeSpec with Matchers {

  implicit private val countriesList: CountriesList = new DefaultCountriesList

  "must read from a valid json structure" - {

    "with optional values present" in {

      val json = Json.obj(
        "name" -> "name",
        "residentCountry" -> "GB",
        "tinDetails" -> Json.arr(
          Json.obj(
            "tin" -> "tin",
            "tinType" -> "OTHER",
            "issuedBy" -> "GB"
          )
        ),
        "registeredCountry" -> "US",
        "address" -> "address"
      )

      val expectedOperator = AssumingPlatformOperator(
        "name",
        Country.GB,
        Seq(TinDetails("tin", Other, "GB")),
        Country("US", "United States"),
        "address"
      )

      json.as[AssumingPlatformOperator] mustEqual expectedOperator
    }

    "with optional values omitted" in {

      val json = Json.obj(
        "name" -> "name",
        "residentCountry" -> "GB",
        "tinDetails" -> Json.arr(),
        "registeredCountry" -> "US",
        "address" -> "address"
      )

      val expectedOperator = AssumingPlatformOperator(
        "name",
        Country.GB,
        Nil,
        Country("US", "United States"),
        "address"
      )

      json.as[AssumingPlatformOperator] mustEqual expectedOperator
    }
  }

  "must fail to read when resident country is not recognised" in {

    val json = Json.obj(
      "name" -> "name",
      "residentCountry" -> "not a country code",
      "tinDetails" -> Json.arr(),
      "registeredCountry" -> "US",
      "address" -> "address"
    )

    json.validate[AssumingPlatformOperator] mustBe a[JsError]
  }

  "must fail to read when registered country is not recognised" in {

    val json = Json.obj(
      "name" -> "name",
      "residentCountry" -> "GB",
      "tinDetails" -> Json.arr(),
      "registeredCountry" -> "not a country code",
      "address" -> "address"
    )

    json.validate[AssumingPlatformOperator] mustBe a[JsError]
  }

  "must write to the correct json structure" - {

    "with optional values present" in {

      val operator = AssumingPlatformOperator(
        "name",
        Country.GB,
        Seq(TinDetails("tin", Other, "GB")),
        Country("US", "United States"),
        "address"
      )

      val expectedJson = Json.obj(
        "name" -> "name",
        "residentCountry" -> "GB",
        "tinDetails" -> Json.arr(
          Json.obj(
            "tin" -> "tin",
            "tinType" -> "OTHER",
            "issuedBy" -> "GB"
          )
        ),
        "registeredCountry" -> "US",
        "address" -> "address"
      )

      Json.toJson(operator) mustEqual expectedJson
    }

    "with optional values missing" in {

      val operator = AssumingPlatformOperator(
        "name",
        Country.GB,
        Nil,
        Country("US", "United States"),
        "address"
      )

      val expectedJson = Json.obj(
        "name" -> "name",
        "residentCountry" -> "GB",
        "tinDetails" -> Json.arr(),
        "registeredCountry" -> "US",
        "address" -> "address"
      )

      Json.toJson(operator) mustEqual expectedJson
    }
  }
}
