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

import controllers.submission.routes
import models.requests.DataRequest
import pages.submission.create.XmlSubmissionSentPage
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class XmlSubmissionSentCheckActionImpl @Inject() (implicit val executionContext: ExecutionContext) extends XmlSubmissionSentCheckAction {

  override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] = {
    val xmlSubmissionSent = request.userAnswers.get(XmlSubmissionSentPage).getOrElse(false)
    if (xmlSubmissionSent) {
      Future.successful(Option(Redirect(routes.XmlSubmissionAlreadySentController.onPageLoad())))
    } else {
      Future.successful(None)
    }
  }

}

trait XmlSubmissionSentCheckAction extends ActionFilter[DataRequest]
