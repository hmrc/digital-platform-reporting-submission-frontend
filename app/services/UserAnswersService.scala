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

import cats.data.{EitherNec, NonEmptyChain}
import cats.implicits.given
import com.google.inject.{Inject, Singleton}
import models.UserAnswers
import models.operator.{TinDetails, TinType}
import models.submission.{AssumedReportingSubmissionRequest, AssumingPlatformOperator}
import pages.assumed.create.*
import queries.{Gettable, Query}

import java.time.Year
import scala.util.Try

@Singleton
class UserAnswersService @Inject() () {

  def toAssumedReportingSubmissionRequest(answers: UserAnswers): EitherNec[Query, AssumedReportingSubmissionRequest] =
    (
      getAssumingOperator(answers),
      getReportingPeriod(answers)
    ).parMapN { (assumingOperator, reportingPeriod) =>
      AssumedReportingSubmissionRequest(
        operatorId = answers.operatorId,
        assumingOperator = assumingOperator,
        reportingPeriod = reportingPeriod
      )
    }

  private def getReportingPeriod(answers: UserAnswers): EitherNec[Query, Year] =
    answers.getEither(ReportingPeriodPage).flatMap { number =>
      Try(Year.of(number)).toEither.left.map(_ => NonEmptyChain.one(ReportingPeriodPage))
    }

  private def getAssumingOperator(answers: UserAnswers): EitherNec[Query, AssumingPlatformOperator] =
    (
      answers.getEither(AssumingOperatorNamePage),
      getResidentialCountry(answers),
      getTinDetails(answers),
      answers.getEither(RegisteredCountryPage).map(_.code),
      answers.getEither(AddressPage)
    ).parMapN(AssumingPlatformOperator.apply)

  private def getResidentialCountry(answers: UserAnswers): EitherNec[Query, String] =
    answers.getEither(TaxResidentInUkPage).flatMap {
      case true =>
        "GB".rightNec
      case false =>
        answers.getEither(TaxResidencyCountryPage).map(_.code)
    }

  private def getTinDetails(answers: UserAnswers): EitherNec[Query, Seq[TinDetails]] =
    answers.getEither(TaxResidentInUkPage).flatMap {
      case true =>
        answers.getEither(HasUkTaxIdentifierPage).flatMap {
          case true  => getUkTinDetails(answers)
          case false => Nil.rightNec
        }
      case false =>
        answers.getEither(HasInternationalTaxIdentifierPage).flatMap {
          case true  => getInternationalTinDetails(answers)
          case false => Nil.rightNec
        }
    }

  private def getUkTinDetails(answers: UserAnswers): EitherNec[Query, Seq[TinDetails]] =
    answers.getEither(UkTaxIdentifierPage).map { value =>
      Seq(TinDetails(
        tin = value,
        tinType = TinType.Other,
        issuedBy = "GB"
      ))
    }

  private def getInternationalTinDetails(answers: UserAnswers): EitherNec[Query, Seq[TinDetails]] =
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

object UserAnswersService {

  final case class BuildAssumedReportingSubmissionRequestFailure(errors: NonEmptyChain[Query]) extends Throwable {
    override def getMessage: String = s"unable to build assumed reporting submission request, path(s) missing: ${errors.toChain.toList.map(_.path).mkString(", ")}"
  }
}
