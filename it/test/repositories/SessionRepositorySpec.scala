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

package repositories

import config.FrontendAppConfig
import models.UserAnswers
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalactic.source.Position
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.MDC
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.play.bootstrap.dispatchers.MDCPropagatingExecutorService

import java.time.{Clock, Instant, ZoneId}
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

class SessionRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[UserAnswers]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val userAnswers1 = UserAnswers("id", "operator1", Json.obj("foo" -> "bar"), Instant.ofEpochSecond(1))
  private val userAnswers2 = UserAnswers("id", "operator2", Json.obj("foo" -> "bar"), Instant.ofEpochSecond(1))

  private def byIds(userId: String, operatorId: String) =
    Filters.and(
      Filters.equal("userId", userId),
      Filters.equal("operatorId", operatorId)
    )

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl).thenReturn(1L)

  protected override val repository: SessionRepository = new SessionRepository(
    mongoComponent = mongoComponent,
    appConfig      = mockAppConfig,
    clock          = stubClock
  )(scala.concurrent.ExecutionContext.Implicits.global)

  ".set" - {

    "must set the last updated time on the supplied user answers to `now`, and save them" in {

      insert(userAnswers1).futureValue
      insert(userAnswers2).futureValue

      val expectedRecord1 = userAnswers1.copy(lastUpdated = instant)

      repository.set(userAnswers1).futureValue
      val record1 = find(byIds(userAnswers1.userId, userAnswers1.operatorId)).futureValue.headOption.value
      val record2 = find(byIds(userAnswers2.userId, userAnswers2.operatorId)).futureValue.headOption.value

      record1 mustEqual expectedRecord1
      record2 mustEqual userAnswers2
    }

    mustPreserveMdc(repository.set(userAnswers1))
  }

  ".get" - {

    "when there is a record for this user id and operator id" - {

      "must update the lastUpdated time of all of this user's records and get the specific record" in {

        insert(userAnswers1).futureValue
        insert(userAnswers2).futureValue

        val result1         = repository.get(userAnswers1.userId, userAnswers1.operatorId).futureValue
        val result2         = repository.get(userAnswers2.userId, userAnswers2.operatorId).futureValue
        val expectedResult1 = userAnswers1.copy(lastUpdated = instant)
        val expectedResult2 = userAnswers2.copy(lastUpdated = instant)

        result1.value mustEqual expectedResult1
        result2.value mustEqual expectedResult2
      }
    }

    "when there is no record for this id" - {

      "must return None" in {

        repository.get("id that does not exist", "foo").futureValue must not be defined
      }
    }

    mustPreserveMdc(repository.get(userAnswers1.userId, userAnswers1.operatorId))
  }

  ".clear" - {

    "must remove a record when an operator id is specified" in {

      insert(userAnswers1).futureValue
      insert(userAnswers2).futureValue

      repository.clear(userAnswers1.userId, userAnswers1.operatorId).futureValue

      repository.get(userAnswers1.userId, userAnswers1.operatorId).futureValue must not be defined
      repository.get(userAnswers2.userId, userAnswers2.operatorId).futureValue mustBe defined
    }

    "must remove all records for a user when an operator id is not specified" in {

      insert(userAnswers1).futureValue
      insert(userAnswers2).futureValue

      repository.clear(userAnswers1.userId).futureValue

      repository.get(userAnswers1.userId, userAnswers1.operatorId).futureValue must not be defined
      repository.get(userAnswers2.userId, userAnswers2.operatorId).futureValue must not be defined
    }

    "must return true when there is no record to remove" in {
      val result = repository.clear("id that does not exist", "foo").futureValue

      result mustEqual true
    }

    "when operator id is not specified" - {

      mustPreserveMdc(repository.clear(userAnswers1.userId))
    }

    "when operator id is specified" - {

      mustPreserveMdc(repository.clear(userAnswers1.userId, userAnswers1.operatorId))
    }
  }

  ".keepAlive" - {

    "when there are records for this user id" - {

      "must update lastUpdated to `now` and return true" in {

        insert(userAnswers1).futureValue
        insert(userAnswers2).futureValue

        repository.keepAlive(userAnswers1.userId).futureValue

        val expectedUpdatedAnswers1 = userAnswers1.copy(lastUpdated = instant)
        val expectedUpdatedAnswers2 = userAnswers2.copy(lastUpdated = instant)

        val updatedAnswers1 = find(byIds(userAnswers1.userId, userAnswers1.operatorId)).futureValue.headOption.value
        val updatedAnswers2 = find(byIds(userAnswers2.userId, userAnswers2.operatorId)).futureValue.headOption.value
        updatedAnswers1 mustEqual expectedUpdatedAnswers1
        updatedAnswers2 mustEqual expectedUpdatedAnswers2
      }
    }

    "when there is no record for this id" - {

      "must return true" in {

        repository.keepAlive("id that does not exist").futureValue mustEqual true
      }
    }

    mustPreserveMdc(repository.keepAlive(userAnswers1.userId))
  }

  private def mustPreserveMdc[A](f: => Future[A])(implicit pos: Position): Unit =
    "must preserve MDC" in {

      implicit lazy val ec: ExecutionContext =
        ExecutionContext.fromExecutor(new MDCPropagatingExecutorService(Executors.newFixedThreadPool(2)))

      MDC.put("test", "foo")

      f.map { _ =>
        MDC.get("test") mustEqual "foo"
      }.futureValue
    }
}
