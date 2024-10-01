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

package pages.assumed

import controllers.assumed.routes
import config.FrontendAppConfig
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.{BeforeAndAfterEach, OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call

class CheckPlatformOperatorPageSpec
  extends AnyFreeSpec
    with Matchers
    with MockitoSugar
    with TryValues
    with OptionValues
    with BeforeAndAfterEach {

  private val mockAppConfig = mock[FrontendAppConfig]
  private val page = new CheckPlatformOperatorPage(mockAppConfig)

  override protected def beforeEach(): Unit = {
    Mockito.reset(mockAppConfig)
    super.beforeEach()
  }

  ".nextPage" - {

    val emptyAnswers = UserAnswers("userId", "operatorId")

    "must go to Check Reporting Notifications when the answer is yes" in {

      val answers = emptyAnswers.set(page, true).success.value
      page.nextPage(NormalMode, answers) mustEqual routes.CheckReportingNotificationsController.onPageLoad("operatorId")
    }

    "must go to update the operator when the answer is no" in {

      when(mockAppConfig.updateOperatorUrl(eqTo("operatorId"))).thenReturn("/foo")
      val answers = emptyAnswers.set(page, false).success.value
      page.nextPage(NormalMode, answers) mustEqual Call("GET", "/foo")
    }
  }
}
