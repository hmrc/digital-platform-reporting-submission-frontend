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

package services

import models.operator.TinDetails
import models.operator.TinType.Other
import models.submission.{AssumedReportingSubmission, AssumingPlatformOperator}
import models.{Country,  UkTaxIdentifiers, UserAnswers}
import models.operator.TinType.{Chrn, Crn, Empref, Other, Utr, Vrn}
import org.scalatest.{EitherValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import pages.assumed.create.*

import java.time.Year

class UserAnswersServiceSpec
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with GuiceOneAppPerSuite
    with EitherValues {

  private lazy val userAnswersService: UserAnswersService = app.injector.instanceOf[UserAnswersService]

  "toAssumedReportingSubmission" - {

    "must return an AssumedReportingSubmission for a GB operator when optional answers are given" in {

      val expectedRequest = AssumedReportingSubmission(
        operatorId = "operatorId",
        assumingOperator = AssumingPlatformOperator(
          name = "assumingOperator",
          residentCountry = "GB",
          tinDetails = Seq(
            TinDetails(
              tin = "tin1",
              tinType = Other,
              issuedBy = "GB"
            )
          ),
          registeredCountry = "US",
          address = "address"
        ),
        reportingPeriod = Year.of(2024)
      )

      val answers = UserAnswers("id", "operatorId")
        .set(AssumingOperatorNamePage, "assumingOperator").success.value
        .set(ReportingPeriodPage, 2024).success.value
        .set(TaxResidentInUkPage, true).success.value
        .set(HasUkTaxIdentifierPage, true).success.value
        .set(UkTaxIdentifierPage, "tin1").success.value
        .set(RegisteredCountryPage, Country("US", "United States")).success.value
        .set(AddressPage, "address").success.value

      val result = userAnswersService.toAssumedReportingSubmission(answers).value
      result mustEqual expectedRequest
    }

    "must return an AssumedReportingSubmission for a GB operator when optional answers are not given" in {

      val expectedRequest = AssumedReportingSubmission(
        operatorId = "operatorId",
        assumingOperator = AssumingPlatformOperator(
          name = "assumingOperator",
          residentCountry = "GB",
          tinDetails = Seq.empty,
          registeredCountry = "US",
          address = "address"
        ),
        reportingPeriod = Year.of(2024)
      )

      val answers = UserAnswers("id", "operatorId")
        .set(AssumingOperatorNamePage, "assumingOperator").success.value
        .set(ReportingPeriodPage, 2024).success.value
        .set(TaxResidentInUkPage, true).success.value
        .set(HasUkTaxIdentifierPage, false).success.value
        .set(RegisteredCountryPage, Country("US", "UnitedStates")).success.value
        .set(AddressPage, "address").success.value

      val result = userAnswersService.toAssumedReportingSubmission(answers).value
      result mustEqual expectedRequest
    }

    "must return an AssumedReportingSubmission for a non-GB operator when optional answers are given" in {

      val expectedRequest = AssumedReportingSubmission(
        operatorId = "operatorId",
        assumingOperator = AssumingPlatformOperator(
          name = "assumingOperator",
          residentCountry = "US",
          tinDetails = Seq(
            TinDetails(
              tin = "tin",
              tinType = Other,
              issuedBy = "US"
            )
          ),
          registeredCountry = "GB",
          address = "address"
        ),
        reportingPeriod = Year.of(2024)
      )

      val answers = UserAnswers("id", "operatorId")
        .set(AssumingOperatorNamePage, "assumingOperator").success.value
        .set(ReportingPeriodPage, 2024).success.value
        .set(TaxResidentInUkPage, false).success.value
        .set(TaxResidencyCountryPage, Country("US", "United States")).success.value
        .set(HasInternationalTaxIdentifierPage, true).success.value
        .set(InternationalTaxIdentifierPage, "tin").success.value
        .set(RegisteredCountryPage, Country("GB", "United Kingdom")).success.value
        .set(AddressPage, "address").success.value

      val result = userAnswersService.toAssumedReportingSubmission(answers).value
      result mustEqual expectedRequest
    }

    "must return an AssumedReportingSubmission for a non-GB operator when optional answers are not given" in {

      val expectedRequest = AssumedReportingSubmission(
        operatorId = "operatorId",
        assumingOperator = AssumingPlatformOperator(
          name = "assumingOperator",
          residentCountry = "US",
          tinDetails = Seq.empty,
          registeredCountry = "GB",
          address = "address"
        ),
        reportingPeriod = Year.of(2024)
      )

      val answers = UserAnswers("id", "operatorId")
        .set(AssumingOperatorNamePage, "assumingOperator").success.value
        .set(ReportingPeriodPage, 2024).success.value
        .set(TaxResidentInUkPage, false).success.value
        .set(TaxResidencyCountryPage, Country("US", "United States")).success.value
        .set(HasInternationalTaxIdentifierPage, false).success.value
        .set(RegisteredCountryPage, Country("GB", "United Kingdom")).success.value
        .set(AddressPage, "address").success.value

      val result = userAnswersService.toAssumedReportingSubmission(answers).value
      result mustEqual expectedRequest
    }
  }
}
