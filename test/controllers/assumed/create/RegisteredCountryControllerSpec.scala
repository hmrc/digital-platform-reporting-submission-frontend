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
import forms.RegisteredCountryFormProvider
import models.{Country, DefaultCountriesList, NormalMode}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.assumed.create.{AssumingOperatorNamePage, RegisteredCountryPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.assumed.create.RegisteredCountryView

import scala.concurrent.Future

class RegisteredCountryControllerSpec extends SpecBase with MockitoSugar {

  private val countriesList = new DefaultCountriesList
  private val formProvider = new RegisteredCountryFormProvider(countriesList)
  private val assumingOperatorName = "name"
  private val baseAnswers = emptyUserAnswers.set(AssumingOperatorNamePage, assumingOperatorName).success.value

  private lazy val registeredCountryRoute = routes.RegisteredCountryController.onPageLoad(NormalMode, operatorId).url

  "RegisteredCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, registeredCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RegisteredCountryView]
        val form = formProvider(assumingOperatorName)(messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, operatorId, assumingOperatorName)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = baseAnswers.set(RegisteredCountryPage, countriesList.allCountries.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, registeredCountryRoute)

        val view = application.injector.instanceOf[RegisteredCountryView]
        val form = formProvider(assumingOperatorName)(messages(application))

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(countriesList.allCountries.head), NormalMode, operatorId, assumingOperatorName)(request, messages(application)).toString
      }
    }

    "must redirect to AssumedReportingDisabled for a GET when submissions are disabled" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .configure("features.submissions-enabled" -> false)
          .build()

      running(application) {
        val request = FakeRequest(GET, registeredCountryRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
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
          FakeRequest(POST, registeredCountryRoute)
            .withFormUrlEncodedBody(("value", countriesList.allCountries.head.code))

        val result = route(application, request).value
        val answers = baseAnswers.set(RegisteredCountryPage, countriesList.allCountries.head).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual RegisteredCountryPage.nextPage(NormalMode, answers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, registeredCountryRoute)
            .withFormUrlEncodedBody(("value", ""))

        val view = application.injector.instanceOf[RegisteredCountryView]
        val form = formProvider(assumingOperatorName)(messages(application))
        val boundForm = form.bind(Map("value" -> ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, operatorId, assumingOperatorName)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, registeredCountryRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, registeredCountryRoute)
            .withFormUrlEncodedBody(("value", countriesList.allCountries.head.code))

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
          FakeRequest(POST, registeredCountryRoute)
            .withFormUrlEncodedBody(("value", countriesList.allCountries.head.code))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.AssumedReportingDisabledController.onPageLoad().url
      }
    }
  }
}
