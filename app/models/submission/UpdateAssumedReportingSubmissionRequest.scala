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

import cats.data.EitherNec
import cats.implicits.*
import models.operator.{TinDetails, TinType}
import models.{Country, UserAnswers, yearFormat}
import pages.assumed.create
import pages.assumed.update.*
import queries.Query

import java.time.Year

case class UpdateAssumedReportingSubmissionRequest(operatorId: String,
                                                   assumingOperator: AssumingPlatformOperator,
                                                   reportingPeriod: Year) {

  lazy val asAssumedReportingSubmissionRequest: AssumedReportingSubmissionRequest = AssumedReportingSubmissionRequest(
    operatorId = operatorId,
    assumingOperator = assumingOperator,
    reportingPeriod = reportingPeriod
  )
}

object UpdateAssumedReportingSubmissionRequest {

  def build(answers: UserAnswers): EitherNec[Query, UpdateAssumedReportingSubmissionRequest] =
    (
      getAssumingOperator(answers),
      answers.getEither(create.ReportingPeriodPage)
    ).parMapN { (assumingOperator, reportingPeriod) =>
      UpdateAssumedReportingSubmissionRequest(
        operatorId = answers.operatorId,
        assumingOperator = assumingOperator,
        reportingPeriod = reportingPeriod
      )
    }

  private[submission] def getAssumingOperator(answers: UserAnswers): EitherNec[Query, AssumingPlatformOperator] =
    (
      answers.getEither(AssumingOperatorNamePage),
      getResidentialCountry(answers),
      getTinDetails(answers),
      answers.getEither(RegisteredCountryPage),
      answers.getEither(AddressPage)
    ).parMapN(AssumingPlatformOperator.apply)

  private[submission] def getResidentialCountry(answers: UserAnswers): EitherNec[Query, Country] =
    answers.getEither(TaxResidentInUkPage).flatMap {
      case true => Country.UnitedKingdom.rightNec
      case false => answers.getEither(TaxResidencyCountryPage)
    }

  private[submission] def getTinDetails(answers: UserAnswers): EitherNec[Query, Seq[TinDetails]] =
    answers.getEither(TaxResidentInUkPage).flatMap {
      case true => answers.getEither(HasUkTaxIdentifierPage).flatMap {
        case true => getUkTinDetails(answers)
        case false => Nil.rightNec
      }
      case false => answers.getEither(HasInternationalTaxIdentifierPage).flatMap {
        case true => getInternationalTinDetails(answers)
        case false => Nil.rightNec
      }
    }

  private[submission] def getUkTinDetails(answers: UserAnswers): EitherNec[Query, Seq[TinDetails]] =
    answers.getEither(UkTaxIdentifierPage).map { value =>
      Seq(TinDetails(
        tin = value,
        tinType = TinType.Other,
        issuedBy = Country.UnitedKingdom.code
      ))
    }

  private[submission] def getInternationalTinDetails(answers: UserAnswers): EitherNec[Query, Seq[TinDetails]] =
    (
      answers.getEither(TaxResidencyCountryPage),
      answers.getEither(InternationalTaxIdentifierPage)
    ).parMapN { (country, tin) =>
      Seq(TinDetails(
        tin = tin,
        tinType = TinType.Other,
        issuedBy = country.code
      ))
    }
}
