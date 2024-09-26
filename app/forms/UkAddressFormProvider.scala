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

import forms.mappings.Mappings
import models.{Country, UkAddress}
import play.api.data.Form
import play.api.data.Forms._

import javax.inject.Inject

class UkAddressFormProvider @Inject() extends Mappings {

  def apply(): Form[UkAddress] = Form(
    mapping(
      "line1" -> text("ukAddress.error.line1.required")
        .verifying(firstError(
          maxLength(35, "ukAddress.error.line1.length"),
          regexp(Validation.textInputPattern.toString, "ukAddress.error.line1.format")
        )),
      "line2" -> optional(text("")
        .verifying(firstError(
          maxLength(35, "ukAddress.error.line2.length"),
          regexp(Validation.textInputPattern.toString, "ukAddress.error.line2.format")
        ))),
      "town" -> text("ukAddress.error.town.required")
        .verifying(firstError(
          maxLength(35, "ukAddress.error.town.length"),
          regexp(Validation.textInputPattern.toString, "ukAddress.error.town.format")
        )),
      "county" -> optional(text("")
        .verifying(firstError(
          maxLength(35, "ukAddress.error.county.length"),
          regexp(Validation.textInputPattern.toString, "ukAddress.error.county.format")
        ))),
      "postCode" -> text("ukAddress.error.postCode.required")
        .verifying(regexp(Validation.ukPostcodePattern.toString, "ukAddress.error.postCode.format")),
      "country" -> text("ukAddress.error.country.required")
        .verifying("ukAddress.error.country.required", value => Country.ukCountries.exists(_.code == value))
        .transform[Country](value => Country.ukCountries.find(_.code == value).get, _.code)
    )(UkAddress.apply)(x => Some((x.line1, x.line2, x.town, x.county, x.postCode, x.country)))
  )
}
