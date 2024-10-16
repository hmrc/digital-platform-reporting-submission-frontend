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
import models.operator.TinType.{Chrn, Crn, Empref, Other, Utr, Vrn}
import models.submission.{AssumedReportingSubmissionRequest, AssumingOperatorAddress, AssumingPlatformOperator}
import models.{Country, InternationalAddress, UkAddress, UkTaxIdentifiers, UserAnswers}
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

  "toAssumedReportingSubmissionRequest" - {

    "must return an AssumedReportingSubmissionRequest for a GB operator when optional answers are given" in {

      val expectedRequest = AssumedReportingSubmissionRequest(
        operatorId = "operatorId",
        assumingOperator = AssumingPlatformOperator(
          name = "assumingOperator",
          residentCountry = "GB",
          tinDetails = Seq(
            TinDetails(
              tin = "tin2",
              tinType = Crn,
              issuedBy = "GB"
            ),
            TinDetails(
              tin = "tin4",
              tinType = Empref,
              issuedBy = "GB"
            ),
            TinDetails(
              tin = "tin3",
              tinType = Vrn,
              issuedBy = "GB"
            ),
            TinDetails(
              tin = "tin5",
              tinType = Chrn,
              issuedBy = "GB"
            ),
            TinDetails(
              tin = "tin1",
              tinType = Utr,
              issuedBy = "GB"
            )
          ),
          address = AssumingOperatorAddress(
            line1 = "line1",
            line2 = Some("line2"),
            city = "city",
            region = Some("region"),
            postCode = "postcode",
            country = "GB"
          )
        ),
        reportingPeriod = Year.of(2024)
      )

      val answers = UserAnswers("id", "operatorId")
        .set(AssumingOperatorNamePage, "assumingOperator").success.value
        .set(ReportingPeriodPage, 2024).success.value
        .set(TaxResidentInUkPage, true).success.value
        .set(HasUkTaxIdentifierPage, true).success.value
        .set(UkTaxIdentifiersPage, UkTaxIdentifiers.values.toSet).success.value
        .set(UtrPage, "tin1").success.value
        .set(CrnPage, "tin2").success.value
        .set(VrnPage, "tin3").success.value
        .set(EmprefPage, "tin4").success.value
        .set(ChrnPage, "tin5").success.value
        .set(RegisteredInUkPage, true).success.value
        .set(UkAddressPage, UkAddress(line1 = "line1", line2 = Some("line2"), town = "city", county = Some("region"), postCode = "postcode", country = Country("GB", "United Kingdom"))).success.value

      val result = userAnswersService.toAssumedReportingSubmissionRequest(answers).value
      result mustEqual expectedRequest
    }

    "must return an AssumedReportingSubmissionRequest for a GB operator when optional answers are not given" in {

      val expectedRequest = AssumedReportingSubmissionRequest(
        operatorId = "operatorId",
        assumingOperator = AssumingPlatformOperator(
          name = "assumingOperator",
          residentCountry = "GB",
          tinDetails = Seq.empty,
          address = AssumingOperatorAddress(
            line1 = "line1",
            line2 = None,
            city = "city",
            region = None,
            postCode = "postcode",
            country = "GB"
          )
        ),
        reportingPeriod = Year.of(2024)
      )

      val answers = UserAnswers("id", "operatorId")
        .set(AssumingOperatorNamePage, "assumingOperator").success.value
        .set(ReportingPeriodPage, 2024).success.value
        .set(TaxResidentInUkPage, true).success.value
        .set(HasUkTaxIdentifierPage, false).success.value
        .set(RegisteredInUkPage, true).success.value
        .set(UkAddressPage, UkAddress(line1 = "line1", line2 = None, town = "city", county = None, postCode = "postcode", country = Country("GB", "United Kingdom"))).success.value

      val result = userAnswersService.toAssumedReportingSubmissionRequest(answers).value
      result mustEqual expectedRequest
    }

    "must return an AssumedReportingSubmissionRequest for a non-GB operator when optional answers are given" in {

      val expectedRequest = AssumedReportingSubmissionRequest(
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
          address = AssumingOperatorAddress(
            line1 = "line1",
            line2 = Some("line2"),
            city = "city",
            region = Some("region"),
            postCode = "postcode",
            country = "US"
          )
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
        .set(RegisteredInUkPage, false).success.value
        .set(InternationalAddressPage, InternationalAddress(line1 = "line1", line2 = Some("line2"), city = "city", region = Some("region"), postal = "postcode", country = Country("US", "United States"))).success.value

      val result = userAnswersService.toAssumedReportingSubmissionRequest(answers).value
      result mustEqual expectedRequest
    }

    "must return an AssumedReportingSubmissionRequest for a non-GB operator when optional answers are not given" in {

      val expectedRequest = AssumedReportingSubmissionRequest(
        operatorId = "operatorId",
        assumingOperator = AssumingPlatformOperator(
          name = "assumingOperator",
          residentCountry = "US",
          tinDetails = Seq.empty,
          address = AssumingOperatorAddress(
            line1 = "line1",
            line2 = None,
            city = "city",
            region = None,
            postCode = "postcode",
            country = "US"
          )
        ),
        reportingPeriod = Year.of(2024)
      )

      val answers = UserAnswers("id", "operatorId")
        .set(AssumingOperatorNamePage, "assumingOperator").success.value
        .set(ReportingPeriodPage, 2024).success.value
        .set(TaxResidentInUkPage, false).success.value
        .set(TaxResidencyCountryPage, Country("US", "United States")).success.value
        .set(HasInternationalTaxIdentifierPage, false).success.value
        .set(RegisteredInUkPage, false).success.value
        .set(InternationalAddressPage, InternationalAddress(line1 = "line1", line2 = None, city = "city", region = None, postal = "postcode", country = Country("US", "United States"))).success.value

      val result = userAnswersService.toAssumedReportingSubmissionRequest(answers).value
      result mustEqual expectedRequest
    }
  }
}
