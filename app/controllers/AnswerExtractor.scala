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

package controllers

import models.requests.DataRequest
import play.api.libs.json.Reads
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import queries.Gettable

import scala.concurrent.Future

trait AnswerExtractor {

  def getAnswer[A](query: Gettable[A])
                  (block: A => Result)
                  (implicit request: DataRequest[AnyContent], ev: Reads[A]): Result =
    request.userAnswers
      .get(query)
      .map(block(_))
      .getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))

  def getAnswers[A, B](queryA: Gettable[A],
                       queryB: Gettable[B])
                      (block: (A, B) => Result)
                      (implicit request: DataRequest[AnyContent], evA: Reads[A], evB: Reads[B]): Result =
    request.userAnswers
      .get(queryA)
      .flatMap { a =>
        request.userAnswers
          .get(queryB)
          .map(block(a, _))
      }.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))

  def getAnswerAsync[A](query: Gettable[A])
                       (block: A => Future[Result])
                       (implicit request: DataRequest[AnyContent], ev: Reads[A]): Future[Result] =
    request.userAnswers
      .get(query)
      .map(block(_))
      .getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad())))

  def getAnswersAsync[A, B](queryA: Gettable[A],
                            queryB: Gettable[B])
                           (block: (A, B) => Future[Result])
                           (implicit request: DataRequest[AnyContent], evA: Reads[A], evB: Reads[B]): Future[Result] =
    request.userAnswers
      .get(queryA)
      .flatMap { a =>
        request.userAnswers
          .get(queryB)
          .map(block(a, _))
      }.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad())))
}
