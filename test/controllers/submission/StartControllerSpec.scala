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

package controllers.submission

import base.SpecBase
import connectors.PlatformOperatorConnector.PlatformOperatorNotFoundFailure
import connectors.PlatformOperatorConnector
import models.UserAnswers
import models.operator.{AddressDetails, ContactDetails}
import models.operator.responses.PlatformOperator
import models.submission.Submission
import models.submission.Submission.State.Ready
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.{ArgumentCaptor, Mockito}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PlatformOperatorSummaryQuery
import repositories.SessionRepository
import viewmodels.PlatformOperatorSummary
import views.html.submission.StartPageView

import scala.concurrent.Future

class StartControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[PlatformOperatorConnector]
  private val mockRepository = mock[SessionRepository]
  private val platformOperatorSummary = PlatformOperatorSummary("operatorId", "operatorName", true)
  private val baseAnswers = emptyUserAnswers.set(PlatformOperatorSummaryQuery, platformOperatorSummary).success.value
  
  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(
      mockConnector,
      mockRepository
    )
  }

  "StartPage Controller" - {

    "onPageLoad" - {

      "must return OK and the correct view for a GET when user answers exist" in {

        val application = applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[PlatformOperatorConnector].toInstance(mockConnector),
            bind[SessionRepository].toInstance(mockRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(routes.StartController.onPageLoad(operatorId))
          val result = route(application, request).value
          val view = application.injector.instanceOf[StartPageView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(operatorId)(request, messages(application)).toString
        }

        verify(mockConnector, never()).viewPlatformOperator(any())(any())
        verify(mockRepository, never()).set(any())
      }

      "must save a platform operator summary and return OK and the correct view for a GET when user answers do not exist" in {

        val operator = PlatformOperator(
          operatorId = operatorId,
          operatorName = "operatorName",
          tinDetails = Nil,
          businessName = None,
          tradingName = None,
          primaryContactDetails = ContactDetails(None, "name", "email"),
          secondaryContactDetails = None,
          addressDetails = AddressDetails("line 1", None, None, None, None, Some("GB")),
          notifications = Nil
        )

        when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.successful(operator))
        when(mockRepository.set(any())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[PlatformOperatorConnector].toInstance(mockConnector),
            bind[SessionRepository].toInstance(mockRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(routes.StartController.onPageLoad(operatorId))
          val result = route(application, request).value
          val view = application.injector.instanceOf[StartPageView]
          val answersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
          val expectedSummary = PlatformOperatorSummary(operator.operatorId, operator.operatorName, false)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(operatorId)(request, messages(application)).toString

          verify(mockConnector, times(1)).viewPlatformOperator(eqTo(operator.operatorId))(any())
          verify(mockRepository, times(1)).set(answersCaptor.capture())

          val answers = answersCaptor.getValue
          answers.get(PlatformOperatorSummaryQuery).value mustEqual expectedSummary
        }
      }

      "must redirect to Select Platform Operator when user answers do not exist and the platform operator cannot be found" in {

        when(mockConnector.viewPlatformOperator(any())(any())).thenReturn(Future.failed(PlatformOperatorNotFoundFailure))
        when(mockRepository.set(any())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[PlatformOperatorConnector].toInstance(mockConnector),
            bind[SessionRepository].toInstance(mockRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(routes.StartController.onPageLoad(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url // TODO change this when the select PO pages exist

          verify(mockRepository, never()).set(any())
        }
      }
    }

    "onSubmit" - {

      "must redirect to Check Platform Operator" in {
        
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(routes.StartController.onSubmit(operatorId))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckPlatformOperatorController.onPageLoad(operatorId).url
        }
      }
    }
  }
}
