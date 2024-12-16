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

import cats.data.{EitherNec, NonEmptyChain, StateT}
import cats.implicits.given
import com.google.inject.{Inject, Singleton}
import models.{CountriesList, Country, UserAnswers, yearFormat}
import models.operator.{TinDetails, TinType}
import models.submission.{AssumedReportingSubmission, AssumedReportingSubmissionRequest, AssumingPlatformOperator}
import pages.assumed.create.*
import pages.assumed.update as updatePages
import play.api.libs.json.Writes
import queries.{AssumedReportingSubmissionQuery, PlatformOperatorNameQuery, Query, ReportingPeriodQuery, Settable}

import scala.util.Try

@Singleton
class UserAnswersService @Inject() (implicit countriesList: CountriesList) {

  def fromAssumedReportingSubmission(userId: String, submission: AssumedReportingSubmission): Try[UserAnswers] = {

    val transformation = for {
      _ <- set(PlatformOperatorNameQuery, submission.operatorName)
      _ <- set(ReportingPeriodQuery, submission.reportingPeriod)
      _ <- set(updatePages.AssumingOperatorNamePage, submission.assumingOperator.name)
      _ <- setTaxDetails(submission.assumingOperator)
      _ <- set(updatePages.RegisteredCountryPage, submission.assumingOperator.registeredCountry)
      _ <- set(updatePages.AddressPage, submission.assumingOperator.address)
      _ <- set(AssumedReportingSubmissionQuery, submission)
    } yield ()

    transformation.runS(UserAnswers(userId, submission.operatorId, Some(submission.reportingPeriod)))
  }

  private def set[A](settable: Settable[A], value: A)(implicit writes: Writes[A]): StateT[Try, UserAnswers, Unit] =
    StateT.modifyF[Try, UserAnswers](_.set(settable, value))

  private def setTaxDetails(assumingOperator: AssumingPlatformOperator): StateT[Try, UserAnswers, Unit] =
    if (countriesList.ukCountries.contains(assumingOperator.residentCountry)) {
      setUkTaxDetails(assumingOperator)
    } else {
      for {
        _ <- set(updatePages.TaxResidencyCountryPage, assumingOperator.residentCountry)
        _ <- setInternationalTaxDetails(assumingOperator)
      } yield ()
    }

  private def setUkTaxDetails(assumingOperator: AssumingPlatformOperator): StateT[Try, UserAnswers, Unit] =
    assumingOperator.tinDetails
      .find(tin => ukCountryCodes.contains(tin.issuedBy))
      .map { tin =>
        for {
          _ <- set(updatePages.TaxResidentInUkPage, true)
          _ <- set(updatePages.HasUkTaxIdentifierPage, true)
          _ <- set(updatePages.UkTaxIdentifierPage, tin.tin)
        } yield ()
      }.getOrElse {
        for {
          _ <- set(updatePages.TaxResidentInUkPage, true)
          _ <- set(updatePages.HasUkTaxIdentifierPage, false)
        } yield ()
      }

  private def setInternationalTaxDetails(assumingOperator: AssumingPlatformOperator): StateT[Try, UserAnswers, Unit] =
    assumingOperator.tinDetails
      .headOption
      .map { tin =>
        for {
          _ <- set(updatePages.TaxResidentInUkPage, false)
          _ <- set(updatePages.HasInternationalTaxIdentifierPage, true)
          _ <- set(updatePages.InternationalTaxIdentifierPage, tin.tin)
        } yield ()
      }.getOrElse {
        for {
          _ <- set(updatePages.TaxResidentInUkPage, false)
          _ <- set(updatePages.HasInternationalTaxIdentifierPage, false)
        } yield ()
      }

  private lazy val ukCountryCodes = countriesList.ukCountries.map(_.code)

  def toAssumedReportingSubmission(answers: UserAnswers): EitherNec[Query, AssumedReportingSubmissionRequest] =
    (
      getAssumingOperator(answers),
      answers.getEither(ReportingPeriodPage)
    ).parMapN { (assumingOperator, reportingPeriod) =>
      AssumedReportingSubmissionRequest(
        operatorId = answers.operatorId,
        assumingOperator = assumingOperator,
        reportingPeriod = reportingPeriod
      )
    }

  private def getAssumingOperator(answers: UserAnswers): EitherNec[Query, AssumingPlatformOperator] =
    (
      answers.getEither(AssumingOperatorNamePage),
      getResidentialCountry(answers),
      getTinDetails(answers),
      answers.getEither(RegisteredCountryPage),
      answers.getEither(AddressPage)
    ).parMapN(AssumingPlatformOperator.apply)

  private def getResidentialCountry(answers: UserAnswers): EitherNec[Query, Country] =
    answers.getEither(TaxResidentInUkPage).flatMap {
      case true =>
        Country.gb.rightNec
      case false =>
        answers.getEither(TaxResidencyCountryPage)
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
        issuedBy = Country.gb.code
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

  final case class BuildAssumedReportingSubmissionFailure(errors: NonEmptyChain[Query]) extends Throwable {
    override def getMessage: String = s"unable to build assumed reporting submission request, path(s) missing: ${errors.toChain.toList.map(_.path).mkString(", ")}"
  }

  final case class InvalidCountryCodeFailure(countryCode: String) extends Throwable {
    override def getMessage: String = s"Unable to find country code $countryCode"
  }
}
