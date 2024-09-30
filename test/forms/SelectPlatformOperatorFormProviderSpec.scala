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

import forms.behaviours.OptionFieldBehaviours
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import play.api.data.FormError

class SelectPlatformOperatorFormProviderSpec extends OptionFieldBehaviours {

  private val formProvider = new SelectPlatformOperatorFormProvider()
  private val operators = Set("operator1", "operator2")
  private val form = formProvider(operators)

  ".value" - {

    val fieldName = "value"
    val requiredKey = "selectPlatformOperator.error.required"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Gen.oneOf(operators)
    )

    behave like fieldThatDoesNotBindInvalidData(
      form,
      fieldName,
      arbitrary[String].suchThat(_.trim.nonEmpty).suchThat(!operators.contains(_)),
      FormError(fieldName, requiredKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      FormError(fieldName, requiredKey)
    )
  }
}
