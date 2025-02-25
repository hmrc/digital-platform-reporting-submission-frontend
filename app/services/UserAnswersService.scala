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

import cats.data.StateT
import cats.implicits.given
import com.google.inject.{Inject, Singleton}
import models.submission.{AssumedReportingSubmission, AssumingPlatformOperator}
import models.{CountriesList, Country, UserAnswers, yearFormat}
import pages.assumed.update as updatePages
import play.api.libs.json.Writes
import queries.*

import scala.util.Try

@Singleton
class UserAnswersService @Inject()(implicit countriesList: CountriesList) {

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
    if (countriesList.ukAndCrownDependantCountries.contains(assumingOperator.residentCountry)) {
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

  private lazy val ukCountryCodes = countriesList.ukAndCrownDependantCountries.map(_.code)
}
