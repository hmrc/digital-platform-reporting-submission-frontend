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
import models.{CountriesList, Country, DefaultCountriesList, yearFormat}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues, TryValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import pages.assumed.update as updatePages
import queries.{AssumedReportingSubmissionQuery, PlatformOperatorNameQuery, ReportingPeriodQuery}

import java.time.Year

class UserAnswersServiceSpec extends AnyFreeSpec
  with Matchers
  with TryValues
  with GuiceOneAppPerSuite
  with EitherValues
  with OptionValues {

  private implicit val countriesList: CountriesList = new DefaultCountriesList

  private lazy val userAnswersService: UserAnswersService = app.injector.instanceOf[UserAnswersService]

  "fromAssumedReportingSubmission" - {

    val userId = "userId"

    "must return UserAnswers for a GB operator when optional answers are given" in {

      val submission = AssumedReportingSubmission(
        operatorId = "operatorId",
        operatorName = "operatorName",
        assumingOperator = AssumingPlatformOperator(
          name = "assumingOperator",
          residentCountry = Country.UnitedKingdom,
          tinDetails = Seq(
            TinDetails(
              tin = "tin1",
              tinType = Other,
              issuedBy = "GB"
            )
          ),
          registeredCountry = Country("US", "United States"),
          address = "address"
        ),
        reportingPeriod = Year.of(2024),
        isDeleted = false
      )

      val result = userAnswersService.fromAssumedReportingSubmission(userId, submission).success.value

      result.userId mustEqual userId
      result.operatorId mustEqual "operatorId"
      result.reportingPeriod.value mustEqual Year.of(2024)
      result.get(AssumedReportingSubmissionQuery).value mustEqual submission
      result.get(ReportingPeriodQuery).value mustEqual Year.of(2024)
      result.get(PlatformOperatorNameQuery).value mustEqual "operatorName"
      result.get(updatePages.AssumingOperatorNamePage).value mustEqual "assumingOperator"
      result.get(updatePages.TaxResidentInUkPage).value mustEqual true
      result.get(updatePages.HasUkTaxIdentifierPage).value mustEqual true
      result.get(updatePages.UkTaxIdentifierPage).value mustEqual "tin1"
      result.get(updatePages.RegisteredCountryPage).value mustEqual Country("US", "United States")
      result.get(updatePages.AddressPage).value mustEqual "address"
      result.get(updatePages.TaxResidencyCountryPage) must not be defined
      result.get(updatePages.HasInternationalTaxIdentifierPage) must not be defined
      result.get(updatePages.InternationalTaxIdentifierPage) must not be defined
    }

    "must return UserAnswers for a GB operator when optional answers are not given" in {

      val submission = AssumedReportingSubmission(
        operatorId = "operatorId",
        operatorName = "operatorName",
        assumingOperator = AssumingPlatformOperator(
          name = "assumingOperator",
          residentCountry = Country.UnitedKingdom,
          tinDetails = Seq.empty,
          registeredCountry = Country("US", "United States"),
          address = "address"
        ),
        reportingPeriod = Year.of(2024),
        isDeleted = false
      )

      val result = userAnswersService.fromAssumedReportingSubmission(userId, submission).success.value

      result.userId mustEqual userId
      result.operatorId mustEqual "operatorId"
      result.reportingPeriod.value mustEqual Year.of(2024)
      result.get(AssumedReportingSubmissionQuery).value mustEqual submission
      result.get(ReportingPeriodQuery).value mustEqual Year.of(2024)
      result.get(PlatformOperatorNameQuery).value mustEqual "operatorName"
      result.get(updatePages.AssumingOperatorNamePage).value mustEqual "assumingOperator"
      result.get(updatePages.TaxResidentInUkPage).value mustEqual true
      result.get(updatePages.HasUkTaxIdentifierPage).value mustEqual false
      result.get(updatePages.RegisteredCountryPage).value mustEqual Country("US", "United States")
      result.get(updatePages.AddressPage).value mustEqual "address"
      result.get(updatePages.UkTaxIdentifierPage) must not be defined
      result.get(updatePages.TaxResidencyCountryPage) must not be defined
      result.get(updatePages.HasInternationalTaxIdentifierPage) must not be defined
      result.get(updatePages.InternationalTaxIdentifierPage) must not be defined
    }

    "must return UserAnswers for a non-GB operator when optional answers are given" in {

      val submission = AssumedReportingSubmission(
        operatorId = "operatorId",
        operatorName = "operatorName",
        assumingOperator = AssumingPlatformOperator(
          name = "assumingOperator",
          residentCountry = Country("US", "United States"),
          tinDetails = Seq(
            TinDetails(
              tin = "tin",
              tinType = Other,
              issuedBy = "US"
            )
          ),
          registeredCountry = Country.UnitedKingdom,
          address = "address"
        ),
        reportingPeriod = Year.of(2024),
        isDeleted = false
      )

      val result = userAnswersService.fromAssumedReportingSubmission(userId, submission).success.value

      result.userId mustEqual userId
      result.operatorId mustEqual "operatorId"
      result.reportingPeriod.value mustEqual Year.of(2024)
      result.get(AssumedReportingSubmissionQuery).value mustEqual submission
      result.get(ReportingPeriodQuery).value mustEqual Year.of(2024)
      result.get(PlatformOperatorNameQuery).value mustEqual "operatorName"
      result.get(updatePages.AssumingOperatorNamePage).value mustEqual "assumingOperator"
      result.get(updatePages.TaxResidentInUkPage).value mustEqual false
      result.get(updatePages.TaxResidencyCountryPage).value mustEqual Country("US", "United States")
      result.get(updatePages.HasInternationalTaxIdentifierPage).value mustEqual true
      result.get(updatePages.InternationalTaxIdentifierPage).value mustEqual "tin"
      result.get(updatePages.RegisteredCountryPage).value mustEqual Country("GB", "United Kingdom")
      result.get(updatePages.AddressPage).value mustEqual "address"
      result.get(updatePages.HasUkTaxIdentifierPage) must not be defined
      result.get(updatePages.UkTaxIdentifierPage) must not be defined
    }

    "must return UserAnswers for a non-GB operator when optional answers are not given" in {

      val submission = AssumedReportingSubmission(
        operatorId = "operatorId",
        operatorName = "operatorName",
        assumingOperator = AssumingPlatformOperator(
          name = "assumingOperator",
          residentCountry = Country("US", "United States"),
          tinDetails = Seq.empty,
          registeredCountry = Country.UnitedKingdom,
          address = "address"
        ),
        reportingPeriod = Year.of(2024),
        isDeleted = false
      )

      val result = userAnswersService.fromAssumedReportingSubmission(userId, submission).success.value

      result.userId mustEqual userId
      result.operatorId mustEqual "operatorId"
      result.reportingPeriod.value mustEqual Year.of(2024)
      result.get(AssumedReportingSubmissionQuery).value mustEqual submission
      result.get(ReportingPeriodQuery).value mustEqual Year.of(2024)
      result.get(PlatformOperatorNameQuery).value mustEqual "operatorName"
      result.get(updatePages.AssumingOperatorNamePage).value mustEqual "assumingOperator"
      result.get(updatePages.TaxResidentInUkPage).value mustEqual false
      result.get(updatePages.TaxResidencyCountryPage).value mustEqual Country("US", "United States")
      result.get(updatePages.HasInternationalTaxIdentifierPage).value mustEqual false
      result.get(updatePages.RegisteredCountryPage).value mustEqual Country("GB", "United Kingdom")
      result.get(updatePages.AddressPage).value mustEqual "address"
      result.get(updatePages.InternationalTaxIdentifierPage) must not be defined
      result.get(updatePages.HasUkTaxIdentifierPage) must not be defined
      result.get(updatePages.UkTaxIdentifierPage) must not be defined
    }
  }
}
