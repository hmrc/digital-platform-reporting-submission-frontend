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

package generators

import models._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

trait ModelGenerators {

  def ukPostcodeGen: Gen[String] =
    for {
      firstChars <- Gen.choose(1, 2)
      first <- Gen.listOfN(firstChars, Gen.alphaUpperChar).map(_.mkString)
      second <- Gen.numChar.map(_.toString)
      third <- Gen.oneOf(Gen.alphaUpperChar, Gen.numChar).map(_.toString)
      fourth <- Gen.numChar.map(_.toString)
      fifth <- Gen.listOfN(2, Gen.alphaUpperChar).map(_.mkString)
    } yield s"$first$second$third$fourth$fifth"
    
  implicit lazy val arbitraryUkTaxIdentifiers: Arbitrary[UkTaxIdentifiers] =
    Arbitrary {
      Gen.oneOf(UkTaxIdentifiers.values)
    }
}
