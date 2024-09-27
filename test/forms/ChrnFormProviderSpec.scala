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

class ChrnFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "chrn.error.required"
  val formatKey = "chrn.error.format"

  val assumingOperatorName = "name"
  val form = new ChrnFormProvider()(assumingOperatorName)

  ".value" - {

    val fieldName = "value"

    val validReferences = for {
      firstChars <- Gen.option(Gen.listOfN(2, Gen.alphaChar))
      numberOfDigits <- Gen.choose(1, 5)
      digits <- Gen.listOfN(numberOfDigits, Gen.numChar)
    } yield firstChars.map(_.mkString).getOrElse("X") + digits.mkString

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validReferences
    )

    "must not bind single characters (other than X) followed by numbers" in {

      val invalidGen = for {
        char <- Gen.alphaChar.suchThat(c => c != 'X' && c != 'x')
        numberOfDigits <- Gen.choose(1, 5)
        digits <- Gen.listOfN(numberOfDigits, Gen.numChar)
      } yield s"$char" + digits.mkString

      forAll(invalidGen) { invalidNumber =>
        val result = form.bind(Map(fieldName -> invalidNumber)).apply(fieldName)
        result.errors must contain only FormError(fieldName, formatKey, Seq(Validation.chrnPattern.toString, assumingOperatorName))
      }
    }

    "must not bind invalid values" in {

      forAll(arbitrary[String]) { input =>

        whenever(input.trim.nonEmpty && !input.trim.matches(Validation.chrnPattern.toString)) {
          val result = form.bind(Map(fieldName -> input)).apply(fieldName)
          result.errors must contain only FormError(fieldName, formatKey, Seq(Validation.chrnPattern.toString, assumingOperatorName))
        }
      }
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, Seq(assumingOperatorName))
    )
  }
}
