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
import models.UserAnswers
import models.requests.DataRequest
import pages.submission.create.XmlSubmissionSentPage
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class XmlSubmissionSentCheckActionSpec extends SpecBase {

  private class Harness extends XmlSubmissionSentCheckActionImpl() {

    def callFilter(ua: UserAnswers): Future[Option[Result]] = {
      val request = DataRequest(FakeRequest(), "userId", ua, "dprsId")
      filter(request)
    }
  }

  "XmlSubmissionSentCheckAction" - {

    "return None if xml submission has not sent" in {
      val action = new Harness()
      val result = action.callFilter(emptyUserAnswers).futureValue
      result mustBe None
    }

    "redirect to xml submission already sent page if xml submission has already been sent" in {
      val action = new Harness()
      val baseAnswers = emptyUserAnswers.set(XmlSubmissionSentPage, true).success.value
      val result = action.callFilter(baseAnswers).map(_.value)
      redirectLocation(result).value mustBe controllers.submission.routes.XmlSubmissionAlreadySentController.onPageLoad().url
    }
  }
}
