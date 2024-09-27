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
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import play.api.data.FormError

class EmprefFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "empref.error.required"
  val formatKey = "empref.error.format"

  val operatorName = "name"
  val form = new EmprefFormProvider()(operatorName)

  ".value" - {

    val fieldName = "value"

    val validReferences = for {
      firstChars <- Gen.listOfN(3, Gen.numChar)
      secondChars <- Gen.listOfN(5, Gen.alphaNumChar)
    } yield firstChars.mkString + "/" + secondChars.mkString

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validReferences
    )

    "must not bind invalid values" in {

      forAll(arbitrary[String]) { input =>

        whenever(input.trim.nonEmpty && !input.trim.matches(Validation.emprefPattern.toString)) {
          val result = form.bind(Map(fieldName -> input)).apply(fieldName)
          result.errors must contain only FormError(fieldName, formatKey, Seq(Validation.emprefPattern.toString, operatorName))
        }
      }
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, Seq(operatorName))
    )
  }
}
