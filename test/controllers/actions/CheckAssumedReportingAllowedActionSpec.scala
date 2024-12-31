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

package controllers.actions

import base.SpecBase
import config.FrontendAppConfig
import controllers.routes
import models.requests.IdentifierRequest
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckAssumedReportingAllowedActionSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockAppConfig = mock[FrontendAppConfig]

  override def beforeEach(): Unit = {
    reset(mockAppConfig)
    super.beforeEach()
  }

  class Harness() extends CheckAssumedReportingAllowedAction(mockAppConfig) {
    def callFilter[A](request: IdentifierRequest[A]): Future[Option[Result]] = filter(request)
  }

  private val action = new Harness()
  private val identifierRequest = IdentifierRequest(FakeRequest(), "userId", "dprsId")

  "Check Assumed Reporting Allowed" - {

    "must return None when submissions are enabled" in {
      when(mockAppConfig.submissionsEnabled).thenReturn(true)
      val result = action.callFilter(identifierRequest).futureValue
      result must not be defined
    }

    "must return a redirect to Assumed Reporting Disabled when submissions are not enabled" in {
      when(mockAppConfig.submissionsEnabled).thenReturn(false)
      val result = action.callFilter(identifierRequest).futureValue
      result.value mustEqual Redirect(routes.AssumedReportingDisabledController.onPageLoad())
    }
  }
}
