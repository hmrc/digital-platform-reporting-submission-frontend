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

package controllers.assumed.create

import base.SpecBase
import controllers.routes as baseRoutes
import forms.TaxResidencyCountryFormProvider
import models.{Country, DefaultCountriesList, NormalMode}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.assumed.AssumedSubmissionSentPage
import pages.assumed.create.{AssumingOperatorNamePage, TaxResidencyCountryPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.assumed.create.TaxResidencyCountryView

import scala.concurrent.Future

class TaxResidencyCountryControllerSpec extends SpecBase with MockitoSugar {

  private val countriesList = new DefaultCountriesList
  private val formProvider = new TaxResidencyCountryFormProvider(countriesList)
  private val assumingOperatorName = "name"
  private val baseAnswers = emptyUserAnswers.set(AssumingOperatorNamePage, assumingOperatorName).success.value

  private lazy val taxResidencyCountryRoute = routes.TaxResidencyCountryController.onPageLoad(NormalMode, operatorId).url

  "TaxResidencyCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, taxResidencyCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[TaxResidencyCountryView]
        val form = formProvider(assumingOperatorName)(messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, operatorId, assumingOperatorName)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = baseAnswers.set(TaxResidencyCountryPage, countriesList.internationalCountries.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, taxResidencyCountryRoute)

        val view = application.injector.instanceOf[TaxResidencyCountryView]
        val form = formProvider(assumingOperatorName)(messages(application))

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(countriesList.internationalCountries.head), NormalMode, operatorId, assumingOperatorName)(request, messages(application)).toString
      }
    }

    "must redirect to AssumedReportingDisabled for a GET when submissions are disabled" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .configure("features.submissions-enabled" -> false)
          .build()

      running(application) {
        val request = FakeRequest(GET, taxResidencyCountryRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
      }
    }

    "must redirect to AssumedSubmissionAlreadySent for a GET when AssumedSubmissionSentPage is true" in {

      val userAnswers = baseAnswers.set(AssumedSubmissionSentPage, true).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, taxResidencyCountryRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.assumed.routes.AssumedSubmissionAlreadySentController.onPageLoad().url
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, taxResidencyCountryRoute)
            .withFormUrlEncodedBody(("value", countriesList.internationalCountries.head.code))

        val result = route(application, request).value
        val answers = baseAnswers.set(TaxResidencyCountryPage, countriesList.internationalCountries.head).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaxResidencyCountryPage.nextPage(NormalMode, answers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, taxResidencyCountryRoute)
            .withFormUrlEncodedBody(("value", ""))

        val form = formProvider(assumingOperatorName)(messages(application))
        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[TaxResidencyCountryView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, operatorId, assumingOperatorName)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, taxResidencyCountryRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, taxResidencyCountryRoute)
            .withFormUrlEncodedBody(("value", countriesList.internationalCountries.head.code))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to AssumedReportingDisabled for a POST when submissions are disabled" in {

      val application =
        applicationBuilder(userAnswers = None)
          .configure("features.submissions-enabled" -> false)
          .build()

      running(application) {
        val request =
          FakeRequest(POST, taxResidencyCountryRoute)
            .withFormUrlEncodedBody(("value", countriesList.internationalCountries.head.code))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
      }
    }
  }
}
