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

import forms.behaviours.BooleanFieldBehaviours
import models.DefaultCountriesList
import play.api.data.FormError

class HasInternationalTaxIdentifierFormProviderSpec extends BooleanFieldBehaviours {

  private val countriesList = new DefaultCountriesList

  private val requiredKey = "hasInternationalTaxIdentifier.error.required"
  private val invalidKey = "error.boolean"

  private val assumingOperatorName = "name"
  private val country = countriesList.internationalCountries.head
  private val form = new HasInternationalTaxIdentifierFormProvider()(assumingOperatorName, country)

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey, Seq(assumingOperatorName, country.name))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, Seq(assumingOperatorName, country.name))
    )
  }
}
