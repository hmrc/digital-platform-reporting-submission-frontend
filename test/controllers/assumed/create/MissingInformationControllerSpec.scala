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

package controllers.assumed.create

import base.SpecBase
import builders.AssumingPlatformOperatorBuilder.anAssumingPlatformOperator
import models.Country.UnitedKingdom
import models.{Country, NormalMode, yearFormat}
import pages.assumed.create.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import support.builders.UserAnswersBuilder.{aUserAnswers, anEmptyUserAnswers}
import views.html.assumed.create.MissingInformationView

import java.time.Year

class MissingInformationControllerSpec extends SpecBase {

  "MissingInformation Controller" - {
    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(aUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.MissingInformationController.onPageLoad(aUserAnswers.operatorId).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[MissingInformationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(aUserAnswers.operatorId)(request, messages(application)).toString
      }
    }

    "must redirect to CheckYourAnswersPage when CreateAssumedReportingSubmissionRequest successfully created" in {
      val userAnswers = aUserAnswers
        .set(ReportingPeriodPage, Year.parse("2024")).success.value
        .set(AssumingOperatorNamePage, anAssumingPlatformOperator.name).success.value
        .set(TaxResidentInUkPage, true).success.value
        .set(HasUkTaxIdentifierPage, true).success.value
        .set(UkTaxIdentifierPage, anAssumingPlatformOperator.tinDetails.head.tin).success.value
        .set(RegisteredCountryPage, UnitedKingdom).success.value
        .set(AddressPage, anAssumingPlatformOperator.address).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.MissingInformationController.onSubmit(anEmptyUserAnswers.operatorId).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(aUserAnswers.operatorId).url
      }
    }

    "must redirect to SelectPlatformOperatorController when CreateAssumedReportingSubmissionRequest creation fails" in {
      val application = applicationBuilder(userAnswers = Some(anEmptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.MissingInformationController.onSubmit(anEmptyUserAnswers.operatorId).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ReportingPeriodController.onPageLoad(NormalMode, anEmptyUserAnswers.operatorId).url
      }
    }
  }
}
