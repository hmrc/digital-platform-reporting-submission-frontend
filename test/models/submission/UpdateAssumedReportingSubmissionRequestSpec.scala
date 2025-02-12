/*
 * Copyright 2025 HM Revenue & Customs
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

package models.submission

import builders.AssumingPlatformOperatorBuilder.anAssumingPlatformOperator
import models.Country.UnitedKingdom
import models.operator.{TinDetails, TinType}
import models.{Country, yearFormat}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues, TryValues}
import pages.assumed.create
import pages.assumed.update.*
import support.builders.UserAnswersBuilder.aUserAnswers

import java.time.Year

class UpdateAssumedReportingSubmissionRequestSpec extends AnyFreeSpec with Matchers
  with TryValues
  with OptionValues
  with EitherValues {

  private val underTest = UpdateAssumedReportingSubmissionRequest

  "getInternationalTinDetails" - {
    "must return correct TinDetails when corresponding values available" in {
      val answers = aUserAnswers
        .set(TaxResidencyCountryPage, Country("US", "United States")).success.value
        .set(InternationalTaxIdentifierPage, "some-tax-id").success.value

      underTest.getInternationalTinDetails(answers) mustBe Right(Seq(TinDetails(
        tin = "some-tax-id",
        tinType = TinType.Other,
        issuedBy = "US"
      )))
    }

    "must return list of errors when data not available" in {
      val answers = aUserAnswers
        .remove(TaxResidencyCountryPage).success.value
        .remove(InternationalTaxIdentifierPage).success.value

      val result = underTest.getInternationalTinDetails(answers)
      result.left.value.toChain.toList must contain theSameElementsAs Seq(TaxResidencyCountryPage, InternationalTaxIdentifierPage)
    }
  }

  "getUkTinDetails" - {
    "must return correct TinDetails when corresponding values available" in {
      val answers = aUserAnswers.set(UkTaxIdentifierPage, "some-uk-tin").success.value

      underTest.getUkTinDetails(answers) mustBe Right(Seq(TinDetails(
        tin = "some-uk-tin",
        tinType = TinType.Other,
        issuedBy = UnitedKingdom.code
      )))
    }

    "must return list of errors when data not available" in {
      val answers = aUserAnswers
        .remove(UkTaxIdentifierPage).success.value

      val result = underTest.getUkTinDetails(answers)
      result.left.value.toChain.toList must contain theSameElementsAs Seq(UkTaxIdentifierPage)
    }
  }

  "getTinDetails" - {
    "must return correct TinDetails" - {
      "when TaxResidentInUkPage is true" - {
        "HasUkTaxIdentifierPage is true and has relevant data" in {
          val answers = aUserAnswers
            .set(TaxResidentInUkPage, true).success.value
            .set(HasUkTaxIdentifierPage, true).success.value
            .set(UkTaxIdentifierPage, "some-uk-tin").success.value

          underTest.getTinDetails(answers) mustBe Right(Seq(TinDetails(
            tin = "some-uk-tin",
            tinType = TinType.Other,
            issuedBy = UnitedKingdom.code
          )))
        }

        "HasUkTaxIdentifierPage is false" in {
          val answers = aUserAnswers
            .set(TaxResidentInUkPage, true).success.value
            .set(HasUkTaxIdentifierPage, false).success.value

          underTest.getTinDetails(answers) mustBe Right(Seq.empty)
        }
      }

      "when TaxResidentInUkPage is false" - {
        "HasInternationalTaxIdentifierPage is true and has relevant data" in {
          val answers = aUserAnswers
            .set(TaxResidentInUkPage, false).success.value
            .set(HasInternationalTaxIdentifierPage, true).success.value
            .set(TaxResidencyCountryPage, Country("US", "United States")).success.value
            .set(InternationalTaxIdentifierPage, "some-tax-id").success.value

          underTest.getTinDetails(answers) mustBe Right(Seq(TinDetails(
            tin = "some-tax-id",
            tinType = TinType.Other,
            issuedBy = "US"
          )))
        }

        "HasInternationalTaxIdentifierPage is false" in {
          val answers = aUserAnswers
            .set(TaxResidentInUkPage, false).success.value
            .set(HasInternationalTaxIdentifierPage, false).success.value

          underTest.getTinDetails(answers) mustBe Right(Seq.empty)
        }
      }
    }

    "must return list of errors when" - {
      "TaxResidentInUkPage is missing" in {
        val answers = aUserAnswers
          .remove(TaxResidentInUkPage).success.value

        val result = underTest.getTinDetails(answers)
        result.left.value.toChain.toList must contain theSameElementsAs Seq(TaxResidentInUkPage)
      }

      "TaxResidentInUkPage is true" - {
        "and HasUkTaxIdentifierPage is missing" in {
          val answers = aUserAnswers
            .set(TaxResidentInUkPage, true).success.value
            .remove(HasUkTaxIdentifierPage).success.value

          val result = underTest.getTinDetails(answers)
          result.left.value.toChain.toList must contain theSameElementsAs Seq(HasUkTaxIdentifierPage)
        }

        "HasUkTaxIdentifierPage is true and UkTaxIdentifierPage is missing" in {
          val answers = aUserAnswers
            .set(TaxResidentInUkPage, true).success.value
            .set(HasUkTaxIdentifierPage, true).success.value
            .remove(UkTaxIdentifierPage).success.value

          val result = underTest.getTinDetails(answers)
          result.left.value.toChain.toList must contain theSameElementsAs Seq(UkTaxIdentifierPage)
        }
      }

      "TaxResidentInUkPage is false" - {
        "and HasInternationalTaxIdentifierPage is missing" in {
          val answers = aUserAnswers
            .set(TaxResidentInUkPage, false).success.value
            .remove(HasInternationalTaxIdentifierPage).success.value

          val result = underTest.getTinDetails(answers)
          result.left.value.toChain.toList must contain theSameElementsAs Seq(HasInternationalTaxIdentifierPage)
        }

        "HasInternationalTaxIdentifierPage is true and TaxResidencyCountryPage and InternationalTaxIdentifierPage are missing" in {
          val answers = aUserAnswers
            .set(TaxResidentInUkPage, false).success.value
            .set(HasInternationalTaxIdentifierPage, true).success.value
            .remove(TaxResidencyCountryPage).success.value
            .remove(InternationalTaxIdentifierPage).success.value

          val result = underTest.getTinDetails(answers)
          result.left.value.toChain.toList must contain theSameElementsAs Seq(TaxResidencyCountryPage, InternationalTaxIdentifierPage)
        }
      }
    }
  }

  "getResidentialCountry" - {
    "must return correct country" - {
      "when TaxResidentInUkPage is true" in {
        val answers = aUserAnswers.set(TaxResidentInUkPage, true).success.value

        underTest.getResidentialCountry(answers) mustBe Right(UnitedKingdom)
      }

      "when TaxResidentInUkPage is false and relevant data exists" in {
        val answers = aUserAnswers
          .set(TaxResidentInUkPage, false).success.value
          .set(TaxResidencyCountryPage, Country("US", "United States")).success.value

        underTest.getResidentialCountry(answers) mustBe Right(Country("US", "United States"))
      }
    }

    "must return list of errors" - {
      "when TaxResidentInUkPage is false and TaxResidencyCountryPage is missing" in {
        val answers = aUserAnswers
          .set(TaxResidentInUkPage, false).success.value
          .remove(TaxResidencyCountryPage).success.value

        val result = underTest.getResidentialCountry(answers)
        result.left.value.toChain.toList must contain theSameElementsAs Seq(TaxResidencyCountryPage)
      }
    }
  }

  "getAssumingOperator" - {
    "must return correct AssumingPlatformOperator when relevant data exists" in {
      val answers = aUserAnswers
        .set(AssumingOperatorNamePage, anAssumingPlatformOperator.name).success.value
        .set(TaxResidentInUkPage, true).success.value
        .set(HasUkTaxIdentifierPage, true).success.value
        .set(UkTaxIdentifierPage, anAssumingPlatformOperator.tinDetails.head.tin).success.value
        .set(RegisteredCountryPage, UnitedKingdom).success.value
        .set(AddressPage, anAssumingPlatformOperator.address).success.value

      underTest.getAssumingOperator(answers) mustBe Right(anAssumingPlatformOperator.copy(residentCountry = UnitedKingdom))
    }

    "must return list of errors when data does not exist" in {
      val answers = aUserAnswers
        .remove(AssumingOperatorNamePage).success.value
        .remove(TaxResidentInUkPage).success.value
        .remove(TaxResidentInUkPage).success.value
        .remove(RegisteredCountryPage).success.value
        .remove(AddressPage).success.value

      val result = underTest.getAssumingOperator(answers)
      result.left.value.toChain.toList must contain theSameElementsAs Seq(
        AssumingOperatorNamePage,
        TaxResidentInUkPage,
        TaxResidentInUkPage,
        RegisteredCountryPage,
        AddressPage
      )
    }
  }

  "build" - {
    "must return correct UpdateAssumedReportingSubmissionRequest when relevant data exists" in {
      val answers = aUserAnswers
        .set(create.ReportingPeriodPage, Year.parse("2024")).success.value
        .set(AssumingOperatorNamePage, anAssumingPlatformOperator.name).success.value
        .set(TaxResidentInUkPage, true).success.value
        .set(HasUkTaxIdentifierPage, true).success.value
        .set(UkTaxIdentifierPage, anAssumingPlatformOperator.tinDetails.head.tin).success.value
        .set(RegisteredCountryPage, UnitedKingdom).success.value
        .set(AddressPage, anAssumingPlatformOperator.address).success.value

      underTest.build(answers) mustBe Right(UpdateAssumedReportingSubmissionRequest(
        operatorId = answers.operatorId,
        assumingOperator = anAssumingPlatformOperator.copy(residentCountry = UnitedKingdom),
        reportingPeriod = Year.parse("2024")
      ))
    }

    "must return list of errors when data does not exist" in {
      val answers = aUserAnswers
        .remove(create.ReportingPeriodPage).success.value
        .remove(AssumingOperatorNamePage).success.value
        .remove(TaxResidentInUkPage).success.value
        .remove(TaxResidentInUkPage).success.value
        .remove(RegisteredCountryPage).success.value
        .remove(AddressPage).success.value

      val result = underTest.build(answers)
      result.left.value.toChain.toList must contain theSameElementsAs Seq(
        create.ReportingPeriodPage,
        AssumingOperatorNamePage,
        TaxResidentInUkPage,
        TaxResidentInUkPage,
        RegisteredCountryPage,
        AddressPage
      )
    }
  }

  ".asAssumedReportingSubmissionRequest" - {
    "must convert UpdateAssumedReportingSubmissionRequest to AssumedReportingSubmissionRequest object" in {
      val underTest = UpdateAssumedReportingSubmissionRequest(
        operatorId = "any-operator-id",
        assumingOperator = anAssumingPlatformOperator,
        reportingPeriod = Year.parse("2024")
      )

      underTest.asAssumedReportingSubmissionRequest mustBe AssumedReportingSubmissionRequest(
        operatorId = underTest.operatorId,
        assumingOperator = underTest.assumingOperator,
        reportingPeriod = underTest.reportingPeriod
      )
    }
  }
}
