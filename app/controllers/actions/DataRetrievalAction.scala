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

import javax.inject.Inject
import models.requests.{IdentifierRequest, OptionalDataRequest}
import play.api.mvc.ActionTransformer
import repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalAction @Inject()(operatorId: String, caseId: Option[String], repository: SessionRepository)
                                   (implicit val executionContext: ExecutionContext) extends ActionTransformer[IdentifierRequest, OptionalDataRequest] {

  override protected def transform[A](request: IdentifierRequest[A]): Future[OptionalDataRequest[A]] = {

    repository.get(request.userId, operatorId, caseId).map {
      OptionalDataRequest(request.request, request.userId, _, request.dprsId)
    }
  }
}

class DataRetrievalActionProvider @Inject()(repository: SessionRepository)(implicit ec: ExecutionContext) {

  def apply(operatorId: String, caseId: Option[String] = None): DataRetrievalAction =
    new DataRetrievalAction(operatorId, caseId, repository)
}
