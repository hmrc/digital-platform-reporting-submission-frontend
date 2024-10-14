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

package pages.assumed.create

import controllers.assumed.create.routes
import models.UkTaxIdentifiers.*
import models.{CheckMode, NormalMode, UkTaxIdentifiers, UserAnswers}
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class UtrPageSpec
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaCheckPropertyChecks {

  ".nextPage" - {

    val emptyAnswers = UserAnswers("id", "operatorId")

    "in Normal Mode" - {

      "must go to CRN if CRN was selected" in {

        val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
          identifiers <- Gen.listOf(Gen.oneOf(values))
        } yield identifiers.toSet + Utr + Crn

        forAll(identifierGen) { identifiers =>
          val answers = emptyAnswers.set(UkTaxIdentifiersPage, identifiers).success.value
          UtrPage.nextPage(NormalMode, answers) mustEqual routes.CrnController.onPageLoad(NormalMode, "operatorId")
        }
      }

      "must go to VRN if VRN was selected and CRN was not selected" in {

        val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
          identifiers <- Gen.listOf(Gen.oneOf(values))
        } yield identifiers.toSet + Utr + Vrn - Crn

        forAll(identifierGen) { identifiers =>
          val answers = emptyAnswers.set(UkTaxIdentifiersPage, identifiers).success.value
          UtrPage.nextPage(NormalMode, answers) mustEqual routes.VrnController.onPageLoad(NormalMode, "operatorId")
        }
      }

      "must go to EMPREF if EMPREF was selected and neither CRN nor VRN was selected" in {

        val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
          identifiers <- Gen.listOf(Gen.oneOf(values))
        } yield identifiers.toSet + Utr + Empref - Crn - Vrn

        forAll(identifierGen) { identifiers =>
          val answers = emptyAnswers.set(UkTaxIdentifiersPage, identifiers).success.value
          UtrPage.nextPage(NormalMode, answers) mustEqual routes.EmprefController.onPageLoad(NormalMode, "operatorId")
        }
      }

      "must go to CHRN if CHRN was selected and neither CRN, VRN nor EMPREF was selected" in {

        val answers = emptyAnswers.set(UkTaxIdentifiersPage, Set[UkTaxIdentifiers](Utr, Chrn)).success.value
        UtrPage.nextPage(NormalMode, answers) mustEqual routes.ChrnController.onPageLoad(NormalMode, "operatorId")
      }

      "must go to Registered in UK when no other tax identifiers were selected" in {

        val answers = emptyAnswers.set(UkTaxIdentifiersPage, Set[UkTaxIdentifiers](Utr)).success.value
        UtrPage.nextPage(NormalMode, answers) mustEqual routes.RegisteredInUkController.onPageLoad(NormalMode, "operatorId")
      }
    }

    "in Check Mode" - {

      "must go to CRN when CRN is selected and not answered" in {

        val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
          identifiers <- Gen.listOf(Gen.oneOf(values))
        } yield identifiers.toSet + Crn + Utr

        forAll(identifierGen) { identifiers =>

          val answers = emptyAnswers.set(UkTaxIdentifiersPage, identifiers).success.value
          UtrPage.nextPage(CheckMode, answers) mustEqual routes.CrnController.onPageLoad(CheckMode, "operatorId")
        }
      }

      "must go to VRN when VRN is selected and has not been answered" - {

        "and CRN is not selected" in {

          val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
            identifiers <- Gen.listOf(Gen.oneOf(values))
          } yield identifiers.toSet + Vrn + Utr - Crn

          forAll(identifierGen) { identifiers =>

            val answers = emptyAnswers.set(UkTaxIdentifiersPage, identifiers).success.value
            UtrPage.nextPage(CheckMode, answers) mustEqual routes.VrnController.onPageLoad(CheckMode, "operatorId")
          }
        }

        "and CRN is selected and answered" in {

          val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
            identifiers <- Gen.listOf(Gen.oneOf(values))
          } yield identifiers.toSet + Vrn + Crn + Utr

          forAll(identifierGen) { identifiers =>

            val answers =
              emptyAnswers
                .set(UkTaxIdentifiersPage, identifiers).success.value
                .set(CrnPage, "crn").success.value

            UtrPage.nextPage(CheckMode, answers) mustEqual routes.VrnController.onPageLoad(CheckMode, "operatorId")
          }
        }
      }

      "must go to EMPREF when EMPREF is selected and has not been answered" - {

        "and CRN and VRN are not selected" in {

          val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
            identifiers <- Gen.listOf(Gen.oneOf(values))
          } yield identifiers.toSet + Empref + Utr - Crn - Vrn

          forAll(identifierGen) { identifiers =>

            val answers = emptyAnswers.set(UkTaxIdentifiersPage, identifiers).success.value
            UtrPage.nextPage(CheckMode, answers) mustEqual routes.EmprefController.onPageLoad(CheckMode, "operatorId")
          }
        }

        "and CRN and VRN are selected and answered" in {

          val identifierGen: Gen[Set[UkTaxIdentifiers]] = for {
            identifiers <- Gen.listOf(Gen.oneOf(values))
          } yield identifiers.toSet + Empref + Vrn + Crn + Utr

          forAll(identifierGen) { identifiers =>

            val answers =
              emptyAnswers
                .set(UkTaxIdentifiersPage, identifiers).success.value
                .set(CrnPage, "crn").success.value
                .set(VrnPage, "vrn").success.value

            UtrPage.nextPage(CheckMode, answers) mustEqual routes.EmprefController.onPageLoad(CheckMode, "operatorId")
          }
        }
      }

      "must go to CHRN when CHRN is selected and has not been answered" - {

        "and CRN, VRN and EMPREF are not selected" in {

          val answers = emptyAnswers.set(UkTaxIdentifiersPage, Set[UkTaxIdentifiers](Chrn)).success.value
          UtrPage.nextPage(CheckMode, answers) mustEqual routes.ChrnController.onPageLoad(CheckMode, "operatorId")
        }

        "and CRN, VRN and EMPREF are selected and  answered" in {

          val answers =
            emptyAnswers
              .set(UkTaxIdentifiersPage, Set[UkTaxIdentifiers](Utr, Crn, Vrn, Empref, Chrn)).success.value
              .set(CrnPage, "crn").success.value
              .set(VrnPage, "vrn").success.value
              .set(EmprefPage, "empref").success.value

          UtrPage.nextPage(CheckMode, answers) mustEqual routes.ChrnController.onPageLoad(CheckMode, "operatorId")
        }
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
                case Utr    => acc.set(UtrPage, "utr").success.value
                case Crn    => acc.set(CrnPage, "crn").success.value
                case Vrn    => acc.set(VrnPage, "vrn").success.value
                case Empref => acc.set(EmprefPage, "empref").success.value
                case Chrn   => acc.set(ChrnPage, "chrn").success.value
              }
            }

            UtrPage.nextPage(CheckMode, answers) mustEqual routes.CheckYourAnswersController.onPageLoad("operatorId")
          }
        }
      }
    }
  }
}