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

package pages.assumed.update

import config.FrontendAppConfig
import controllers.assumed.update.routes
import models.UserAnswers
import org.mockito.Mockito
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call

import java.time.Year

class CheckReportingNotificationsPageSpec
  extends AnyFreeSpec
    with Matchers
    with MockitoSugar
    with TryValues
    with OptionValues
    with BeforeAndAfterEach {

  private val mockAppConfig = mock[FrontendAppConfig]
  private val page = new CheckReportingNotificationsPage(mockAppConfig)

  override protected def beforeEach(): Unit = {
    Mockito.reset(mockAppConfig)
    super.beforeEach()
  }

  ".nextPage" - {

    val reportingPeriod = Year.of(2024)
    val operatorId = "operatorId"
    val emptyAnswers = UserAnswers("id", operatorId, Some(reportingPeriod))

    "must go to Check Contact Details when the answer is yes" in {

      val answers = emptyAnswers.set(page, true).success.value
      page.nextPage(reportingPeriod, answers).mustEqual(routes.CheckContactDetailsController.onPageLoad(operatorId, reportingPeriod))
    }

    "must go to add a reporting notification when the answer is no" in {

      val answers = emptyAnswers.set(page, false).success.value
      page.nextPage(reportingPeriod, answers).mustEqual(Call("GET", mockAppConfig.manageHomepageUrl))
    }
  }
}
