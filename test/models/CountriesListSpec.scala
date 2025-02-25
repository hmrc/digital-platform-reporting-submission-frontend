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

package models

import models.Country.{Guernsey, IsleOfMan, Jersey, UnitedKingdom}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class CountriesListSpec extends AnyFreeSpec with Matchers {

  private val underTest = new CountriesList {}

  "CountryList" - {
    "ukAndCrownDependantCountries" - {
      "must contain only UnitedKingdom, Guernsey, Jersey and Isle of Man" in {
        val expectedList = Seq(UnitedKingdom, Guernsey, Jersey, IsleOfMan)

        underTest.ukAndCrownDependantCountries must contain theSameElementsAs expectedList
      }
    }

    "internationalCountries" - {
      "must not contain crown dependencies" in {
        underTest.internationalCountries must not contain underTest.ukAndCrownDependantCountries
      }
      "must contain all other countries" in {
        val expectedList: Seq[Country] = underTest.allCountries diff underTest.ukAndCrownDependantCountries

        underTest.internationalCountries must contain theSameElementsAs expectedList
      }
    }

    "nonUkCountries" - {
      "must not contain all countries except the United Kingdom" in {
        val expectedList = underTest.allCountries.filterNot(_ == UnitedKingdom)

        underTest.nonUkCountries must contain theSameElementsAs expectedList
      }
    }
  }
}
