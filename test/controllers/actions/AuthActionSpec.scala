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

import auth.Retrievals._
import base.SpecBase
import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AuthActionSpec extends SpecBase {

  private val application = applicationBuilder(userAnswers = None).build()
  private val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
  private val appConfig = application.injector.instanceOf[FrontendAppConfig]
  private val emptyEnrolments = Enrolments(Set.empty)

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction { request =>
      Results.Ok(s"${request.userId} ${request.dprsId}")
    }
  }

  "Auth Action" - {

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new MissingBearerToken), appConfig, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value must startWith(appConfig.loginUrl)
      }
    }

    "when the user's session has expired" - {

      "must redirect the user to log in " in {

        val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new BearerTokenExpired), appConfig, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value must startWith(appConfig.loginUrl)
      }
    }

    "when the user doesn't have a DPRS enrolments" - {

      "must redirect the user to the unauthorised page" in {

        val authAction = new AuthenticatedIdentifierAction(new FakeAuthConnector(Some("internalId") ~ emptyEnrolments), appConfig, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
      }
    }

    "when the user has a DPRS enrolment" -{

      "must succeed" in {

        val enrolments = Enrolments(Set(Enrolment("HMRC-DPRS", Seq(EnrolmentIdentifier("DPRSID", "dprsId")), "activated", None)))
        val authAction = new AuthenticatedIdentifierAction(new FakeAuthConnector(Some("internalId") ~ enrolments), appConfig, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe OK
        contentAsString(result) mustEqual "internalId dprsId"
      }
    }
  }
}

class FakeAuthConnector[T](value: T) extends AuthConnector {
  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.fromTry(Try(value.asInstanceOf[A]))
}

class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}