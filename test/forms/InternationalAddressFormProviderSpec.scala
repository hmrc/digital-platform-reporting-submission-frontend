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
import models.Country
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import play.api.data.FormError

class InternationalAddressFormProviderSpec extends StringFieldBehaviours {

  val form = new InternationalAddressFormProvider()()

  ".line1" - {

    val fieldName = "line1"
    val requiredKey = "internationalAddress.error.line1.required"
    val lengthKey = "internationalAddress.error.line1.length"
    val formatKey = "internationalAddress.error.line1.format"
    val maxLength = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      safeTextInputsWithMaxLength(maxLength)
    )

    behave like fieldThatDoesNotBindInvalidData(
      form,
      fieldName,
      unsafeTextInputsWithMaxLength(maxLength),
      FormError(fieldName, formatKey, Seq(Validation.textInputPattern.toString))
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".line2" - {

    val fieldName = "line2"
    val lengthKey = "internationalAddress.error.line2.length"
    val formatKey = "internationalAddress.error.line2.format"
    val maxLength = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      safeTextInputsWithMaxLength(maxLength)
    )

    behave like fieldThatDoesNotBindInvalidData(
      form,
      fieldName,
      unsafeTextInputsWithMaxLength(maxLength),
      FormError(fieldName, formatKey, Seq(Validation.textInputPattern.toString))
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )
  }

  ".city" - {

    val fieldName = "city"
    val requiredKey = "internationalAddress.error.city.required"
    val lengthKey = "internationalAddress.error.city.length"
    val formatKey = "internationalAddress.error.city.format"
    val maxLength = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      safeTextInputsWithMaxLength(maxLength)
    )

    behave like fieldThatDoesNotBindInvalidData(
      form,
      fieldName,
      unsafeTextInputsWithMaxLength(maxLength),
      FormError(fieldName, formatKey, Seq(Validation.textInputPattern.toString))
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".region" - {

    val fieldName = "region"
    val lengthKey = "internationalAddress.error.region.length"
    val formatKey = "internationalAddress.error.region.format"
    val maxLength = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      safeTextInputsWithMaxLength(maxLength)
    )

    behave like fieldThatDoesNotBindInvalidData(
      form,
      fieldName,
      unsafeTextInputsWithMaxLength(maxLength),
      FormError(fieldName, formatKey, Seq(Validation.textInputPattern.toString))
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )
  }

  ".postal" - {

    val fieldName = "postal"
    val requiredKey = "internationalAddress.error.postal.required"
    val lengthKey = "internationalAddress.error.postal.length"
    val formatKey = "internationalAddress.error.postal.format"
    val maxLength = 25

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      safeTextInputsWithMaxLength(maxLength)
    )

    behave like fieldThatDoesNotBindInvalidData(
      form,
      fieldName,
      unsafeTextInputsWithMaxLength(maxLength),
      FormError(fieldName, formatKey, Seq(Validation.textInputPattern.toString))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )
  }

  ".country" - {

    val fieldName = "country"
    val requiredKey = "internationalAddress.error.country.required"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Gen.oneOf(Country.internationalCountries.map(_.code))
    )

    behave like fieldThatDoesNotBindInvalidData(
      form,
      fieldName,
      arbitrary[String].suchThat(_.nonEmpty).suchThat(!Country.allCountries.map(_.code).contains(_)),
      FormError(fieldName, requiredKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
