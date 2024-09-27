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

class UtrFormProviderSpec extends StringFieldBehaviours {


  private val assumingOperatorName = "name"
  private val form = new UtrFormProvider()(assumingOperatorName)

  private val requiredKey = "utr.error.required"
  private val formatKey = "utr.error.format"

  ".value" - {

    val validData = for {
      first <- Gen.option(Gen.oneOf('K', 'k'))
      numberOfDigits <- Gen.oneOf(10, 13)
      digits <- Gen.listOfN(numberOfDigits, Gen.numChar)
      last <- Gen.option(Gen.oneOf('K', 'k'))
    } yield first.map(_.toString).getOrElse("") + digits.mkString + last.map(_.toString).getOrElse("")

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validData
    )

    "must not bind invalid values" in {

      forAll(arbitrary[String]) { input =>

        whenever(input.trim.nonEmpty && !input.trim.matches(Validation.utrPattern.toString)) {
          val result = form.bind(Map(fieldName -> input)).apply(fieldName)
          result.errors must contain only FormError(fieldName, formatKey, Seq(Validation.utrPattern.toString, assumingOperatorName))
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
