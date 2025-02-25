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

import controllers.assumed.routes
import models.requests.DataRequest
import pages.assumed.AssumedSubmissionSentPage
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AssumedSubmissionSentCheckActionImpl @Inject() (implicit val executionContext: ExecutionContext) extends AssumedSubmissionSentCheckAction {

  override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] = {
    val assumedSubmissionSent = request.userAnswers.get(AssumedSubmissionSentPage).getOrElse(false)
    if (assumedSubmissionSent) {
      Future.successful(Option(Redirect(routes.AssumedSubmissionAlreadySentController.onPageLoad())))
    } else {
      Future.successful(None)
    }
  }

}

trait AssumedSubmissionSentCheckAction extends ActionFilter[DataRequest]
