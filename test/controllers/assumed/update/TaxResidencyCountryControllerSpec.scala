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

package controllers.assumed.update

import base.SpecBase
import controllers.routes as baseRoutes
import forms.TaxResidencyCountryFormProvider
import models.{Country, NormalMode}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.assumed.update.{AssumingOperatorNamePage, TaxResidencyCountryPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.assumed.update.TaxResidencyCountryView

import scala.concurrent.Future

class TaxResidencyCountryControllerSpec extends SpecBase with MockitoSugar {

  private val reportingPeriod = "reportingPeriod"
  private val formProvider = new TaxResidencyCountryFormProvider()
  private val assumingOperatorName = "name"
  private val baseAnswers = emptyUserAnswers.copy(reportingPeriod = Some(reportingPeriod)).set(AssumingOperatorNamePage, assumingOperatorName).success.value

  lazy val taxResidencyCountryRoute = routes.TaxResidencyCountryController.onPageLoad(operatorId, reportingPeriod).url

  "TaxResidencyCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, taxResidencyCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[TaxResidencyCountryView]
        val form = formProvider(assumingOperatorName)(messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, operatorId, reportingPeriod, assumingOperatorName)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = baseAnswers.set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, taxResidencyCountryRoute)

        val view = application.injector.instanceOf[TaxResidencyCountryView]
        val form = formProvider(assumingOperatorName)(messages(application))

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(Country.internationalCountries.head), operatorId, reportingPeriod, assumingOperatorName)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, taxResidencyCountryRoute)
            .withFormUrlEncodedBody(("value", Country.internationalCountries.head.code))

        val result = route(application, request).value
        val answers = baseAnswers.set(TaxResidencyCountryPage, Country.internationalCountries.head).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual TaxResidencyCountryPage.nextPage(reportingPeriod, answers).url
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
        contentAsString(result) mustEqual view(boundForm, operatorId, reportingPeriod, assumingOperatorName)(request, messages(application)).toString
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
            .withFormUrlEncodedBody(("value", Country.internationalCountries.head.code))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
