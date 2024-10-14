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
import models.UkTaxIdentifiers.{Chrn, Crn, Empref, Utr, Vrn}
import models.UserAnswers
import models.operator.{TinDetails, TinType}
import models.submission.{AssumedReportingSubmissionRequest, AssumingOperatorAddress, AssumingPlatformOperator}
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
      getAddress(answers)
    ).parMapN(AssumingPlatformOperator.apply)

  private def getResidentialCountry(answers: UserAnswers): EitherNec[Query, String] =
    answers.getEither(TaxResidentInUkPage).flatMap {
      case true =>
        "GB".rightNec
      case false =>
        answers.getEither(TaxResidencyCountryPage).map(_.code)
    }

  private def getTinDetails(answers: UserAnswers): EitherNec[Query, Seq[TinDetails]] =
    answers.getEither(HasTaxIdentifierPage).flatMap {
      case false =>
        Seq.empty.rightNec
      case true =>
        answers.getEither(TaxResidentInUkPage).flatMap {
          case true =>
            getUkTinDetails(answers)
          case false =>
            getInternationalTinDetails(answers)
        }
    }

  private def getUkTinDetails(answers: UserAnswers): EitherNec[Query, Seq[TinDetails]] =
    answers.getEither(UkTaxIdentifiersPage).flatMap { tins =>
      tins.toSeq.traverse {
        case Utr => getUkTaxIdentifier(answers, UtrPage, TinType.Utr)
        case Crn => getUkTaxIdentifier(answers, CrnPage, TinType.Crn)
        case Vrn => getUkTaxIdentifier(answers, VrnPage, TinType.Vrn)
        case Empref => getUkTaxIdentifier(answers, EmprefPage, TinType.Empref)
        case Chrn => getUkTaxIdentifier(answers, ChrnPage, TinType.Chrn)
      }
    }

  private def getUkTaxIdentifier(answers: UserAnswers, query: Gettable[String], tinType: TinType): EitherNec[Query, TinDetails] =
    answers.getEither(query).map { value =>
      TinDetails(
        tin = value,
        tinType = tinType,
        issuedBy = "GB"
      )
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

  private def getAddress(answers: UserAnswers): EitherNec[Query, AssumingOperatorAddress] =
    answers.getEither(RegisteredInUkPage).flatMap {
      case true =>
        getUkAddress(answers)
      case false =>
        getInternationalAddress(answers)
    }

  private def getUkAddress(answers: UserAnswers): EitherNec[Query, AssumingOperatorAddress] =
    answers.getEither(UkAddressPage).map { address =>
      AssumingOperatorAddress(
        line1 = address.line1,
        line2 = address.line2,
        city = address.town,
        region = address.county,
        postCode = address.postCode,
        country = address.country.code
      )
    }

  private def getInternationalAddress(answers: UserAnswers): EitherNec[Query, AssumingOperatorAddress] =
    answers.getEither(InternationalAddressPage).map { address =>
      AssumingOperatorAddress(
        line1 = address.line1,
        line2 = address.line2,
        city = address.city,
        region = address.region,
        postCode = address.postal,
        country = address.country.code
      )
    }
}

object UserAnswersService {

  final case class BuildAssumedReportingSubmissionRequestFailure(errors: NonEmptyChain[Query]) extends Throwable {
    override def getMessage: String = s"unable to build assumed reporting submission request, path(s) missing: ${errors.toChain.toList.map(_.path).mkString(", ")}"
  }
}
