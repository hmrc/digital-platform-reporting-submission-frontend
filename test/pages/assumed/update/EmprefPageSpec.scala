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

package pages.assumed.update

import controllers.assumed.update.routes
import models.UkTaxIdentifiers.{Chrn, Crn, Empref, Utr, Vrn, values}
import models.{UkTaxIdentifiers, UserAnswers}
import org.scalacheck.Gen
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class EmprefPageSpec
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaCheckPropertyChecks {

  ".nextPage" - {

    val caseId = "caseId"
    val operatorId = "operatorId"
    val emptyAnswers = UserAnswers("id", operatorId, Some(caseId))

    "must go to CHRN when CHRN is selected and has not been answered" in {

      val answers = emptyAnswers.set(UkTaxIdentifiersPage, Set[UkTaxIdentifiers](Utr, Vrn, Crn, Chrn)).success.value
      EmprefPage.nextPage(caseId, answers).mustEqual(routes.ChrnController.onPageLoad(operatorId, caseId))
    }

    "must go to Check Answers" - {

      "when all selected options have been answered" in {

        val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
          identifiers <- Gen.listOf(Gen.oneOf(values))
        } yield identifiers.toSet

        forAll(identifierGen) { identifiers =>

          val baseAnswers = emptyAnswers.set(UkTaxIdentifiersPage, identifiers).success.value

          val answers = identifiers.foldLeft(baseAnswers) { (acc, next) =>
            next match {
              case Utr => acc.set(UtrPage, "utr").success.value
              case Crn => acc.set(CrnPage, "crn").success.value
              case Vrn => acc.set(VrnPage, "vrn").success.value
              case Empref => acc.set(EmprefPage, "empref").success.value
              case Chrn => acc.set(ChrnPage, "chrn").success.value
            }
          }

          EmprefPage.nextPage(caseId, answers).mustEqual(routes.CheckYourAnswersController.onPageLoad(operatorId, caseId))
        }
      }
    }
  }
}
