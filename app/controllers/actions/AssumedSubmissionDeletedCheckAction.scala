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
import pages.assumed.AssumedSubmissionDeletedPage
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AssumedSubmissionDeletedCheckActionImpl @Inject() (implicit val executionContext: ExecutionContext) extends AssumedSubmissionDeletedCheckAction {

  override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] = {
    val assumedSubmissionDeleted = request.userAnswers.get(AssumedSubmissionDeletedPage).getOrElse(false)
    if (assumedSubmissionDeleted) {
      Future.successful(Option(Redirect(routes.AssumedSubmissionAlreadyDeletedController.onPageLoad())))
    } else {
      Future.successful(None)
    }
  }

}

trait AssumedSubmissionDeletedCheckAction extends ActionFilter[DataRequest]
