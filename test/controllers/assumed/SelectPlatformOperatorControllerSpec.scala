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

package controllers.assumed

import base.SpecBase
import connectors.PlatformOperatorConnector
import forms.SelectPlatformOperatorFormProvider
import models.operator.responses.{PlatformOperator, ViewPlatformOperatorsResponse}
import models.operator.{AddressDetails, ContactDetails}
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.assumed.SelectPlatformOperatorPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.BusinessNameQuery
import repositories.SessionRepository
import viewmodels.PlatformOperatorViewModel
import views.html.assumed.{SelectPlatformOperatorSingleChoiceView, SelectPlatformOperatorView}

import scala.concurrent.Future

class SelectPlatformOperatorControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[PlatformOperatorConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector)
    super.beforeEach()
  }

  private lazy val selectPlatformOperatorRoute = routes.SelectPlatformOperatorController.onPageLoad.url

  private val operator1 = PlatformOperator(
    operatorId = "operatorId",
    operatorName = "operatorName1",
    tinDetails = Nil,
    businessName = None,
    tradingName = None,
    primaryContactDetails = ContactDetails(None, "name", "email"),
    secondaryContactDetails = None,
    addressDetails = AddressDetails("line 1", None, None, None, None, None),
    notifications = Nil
  )

  private val operator2 = PlatformOperator(
    operatorId = "operatorId2",
    operatorName = "operatorName2",
    tinDetails = Nil,
    businessName = None,
    tradingName = None,
    primaryContactDetails = ContactDetails(None, "name", "email"),
    secondaryContactDetails = None,
    addressDetails = AddressDetails("line 1", None, None, None, None, None),
    notifications = Nil
  )
  private val formProvider = new SelectPlatformOperatorFormProvider()

  "Select Platform Operator Controller" - {

    "must return OK and the correct view for a GET" - {

      "when there is only one platform operator" in {

        val viewOperatorInfo = ViewPlatformOperatorsResponse(Seq(operator1))

        when(mockConnector.viewPlatformOperators(any())).thenReturn(Future.successful(viewOperatorInfo))

        val application =
          applicationBuilder(userAnswers = None)
            .overrides(bind[PlatformOperatorConnector].toInstance(mockConnector))
            .build()

        running(application) {
          val request = FakeRequest(GET, selectPlatformOperatorRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SelectPlatformOperatorSingleChoiceView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(operator1.operatorId, operator1.operatorName)(request, messages(application)).toString
          verify(mockConnector, times(1)).viewPlatformOperators(any())
        }
      }

      "when there are two or more platform operators" in {

        val form = formProvider(Set(operator1.operatorId, operator2.operatorId))
        val viewOperatorInfo = ViewPlatformOperatorsResponse(Seq(operator1, operator2))

        when(mockConnector.viewPlatformOperators(any())).thenReturn(Future.successful(viewOperatorInfo))

        val application =
          applicationBuilder(userAnswers = None)
            .overrides(bind[PlatformOperatorConnector].toInstance(mockConnector))
            .build()

        running(application) {
          val request = FakeRequest(GET, selectPlatformOperatorRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SelectPlatformOperatorView]
          val expectedViewModels = Seq(
            PlatformOperatorViewModel("operatorId", "operatorName1", false),
            PlatformOperatorViewModel("operatorId2", "operatorName2", false)
          )

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(form, expectedViewModels)(request, messages(application)).toString
          verify(mockConnector, times(1)).viewPlatformOperators(any())
        }
      }
    }

    "must save the platform operator name and redirect to the next page when valid data is submitted" in {

      val viewOperatorInfo = ViewPlatformOperatorsResponse(Seq(operator1))
      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockConnector.viewPlatformOperators(any())).thenReturn(Future.successful(viewOperatorInfo))

      val application =
        applicationBuilder(userAnswers = None)
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[PlatformOperatorConnector].toInstance(mockConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, selectPlatformOperatorRoute)
            .withFormUrlEncodedBody(("value", "operatorId"))

        val result = route(application, request).value
        val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual SelectPlatformOperatorPage.nextPage(NormalMode, emptyUserAnswers).url
        verify(mockSessionRepository, times(1)).set(answersCaptor.capture())

        val answers = answersCaptor.getValue
        answers.operatorId mustEqual "operatorId"
        answers.get(BusinessNameQuery).value mustEqual "operatorName1"
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val form = formProvider(Set(operator1.operatorId, operator2.operatorId))
      val viewOperatorInfo = ViewPlatformOperatorsResponse(Seq(operator1, operator2))
      when(mockConnector.viewPlatformOperators(any())).thenReturn(Future.successful(viewOperatorInfo))

      val application =
        applicationBuilder(userAnswers = None)
          .overrides(bind[PlatformOperatorConnector].toInstance(mockConnector))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, selectPlatformOperatorRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[SelectPlatformOperatorView]
        val viewModels = Seq(
          PlatformOperatorViewModel("operatorId", "operatorName1", false),
          PlatformOperatorViewModel("operatorId2", "operatorName2", false)
        )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, viewModels)(request, messages(application)).toString
      }
    }
  }
}
