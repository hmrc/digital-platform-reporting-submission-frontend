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

package viewmodels.checkAnswers.operator

import models.{Country, DefaultCountriesList}
import models.operator.{AddressDetails, ContactDetails}
import models.operator.responses.PlatformOperator
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import viewmodels.govuk.summarylist.ValueViewModel
import viewmodels.implicits.*

class RegisteredInUkSummarySpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  private val countriesList = new DefaultCountriesList

  ".row" - {

    implicit val messages: Messages = stubMessages()
    val baseAddress = AddressDetails("line 1", None, None, None, None, None)
    val baseOperator = PlatformOperator(
      operatorId = "operatorId",
      operatorName = "operatorName",
      tinDetails = Nil,
      businessName = None,
      tradingName = None,
      primaryContactDetails = ContactDetails(None, "name", "email"),
      secondaryContactDetails = None,
      addressDetails = baseAddress,
      notifications = Nil
    )

    "must have a value of `yes` when the operator's country is in the UK" in {

      forAll(Gen.oneOf(countriesList.ukCountries)) { country =>
        val operator = baseOperator.copy(addressDetails = baseAddress.copy(countryCode = Some(country.code)))
        val row = RegisteredInUkSummary.row(operator, countriesList).value
        row.value mustEqual ValueViewModel(messages("site.yes"))
      }
    }

    "must have a value of `no` when the operator's country is not in the UK" in {

      forAll(Gen.oneOf(countriesList.internationalCountries)) { country =>
        val operator = baseOperator.copy(addressDetails = baseAddress.copy(countryCode = Some(country.code)))
        val row = RegisteredInUkSummary.row(operator, countriesList).value
        row.value mustEqual ValueViewModel(messages("site.no"))
      }
    }

    "must be None when the operator does not have a country" in {

      RegisteredInUkSummary.row(baseOperator, countriesList) must not be defined
    }
  }
}
