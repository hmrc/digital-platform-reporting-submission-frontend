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
import com.fasterxml.jackson.core.JsonParseException
import org.mongodb.scala.bson.BsonDocument
import models.{UserAnswers, yearFormat}
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalactic.source.Position
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import play.api.Configuration
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.MDC
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.play.bootstrap.dispatchers.MDCPropagatingExecutorService

import java.security.SecureRandom
import java.time.{Clock, Instant, Year, ZoneId}
import java.time.temporal.ChronoUnit
import java.util.Base64
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

  private val userAnswers1 = UserAnswers("id", "operator1", None, Json.obj("foo" -> "bar"), Instant.ofEpochSecond(1))
  private val userAnswers2 = UserAnswers("id", "operator1", Some(Year.of(2024)), Json.obj("foo" -> "bar"), Instant.ofEpochSecond(1))

  private def byIds(userId: String, operatorId: String, reportingPeriod: Option[Year]) =
    Filters.and(
      Filters.equal("userId", userId),
      Filters.equal("operatorId", operatorId),
      reportingPeriod.map(Filters.equal("reportingPeriod", _)).getOrElse(Filters.exists("reportingPeriod", exists = false))
    )

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl).thenReturn(1L)
  when(mockAppConfig.dataEncryptionEnabled).thenReturn(true)

  private val aesKey = {
    val aesKey = new Array[Byte](32)
    new SecureRandom().nextBytes(aesKey)
    Base64.getEncoder.encodeToString(aesKey)
  }
  private val configuration = Configuration("crypto.key" -> aesKey)
  private implicit val crypto: Encrypter & Decrypter =
    SymmetricCryptoFactory.aesGcmCryptoFromConfig("crypto", configuration.underlying)

  protected override val repository: SessionRepository = new SessionRepository(
    mongoComponent = mongoComponent,
    appConfig      = mockAppConfig,
    clock          = stubClock
  )(scala.concurrent.ExecutionContext.Implicits.global, implicitly)

  ".set" - {

    "must set the last updated time on the supplied user answers to `now`, and save them" in {

      insert(userAnswers1).futureValue
      insert(userAnswers2).futureValue

      val expectedRecord1 = userAnswers1.copy(lastUpdated = instant)

      repository.set(userAnswers1).futureValue
      val record1 = find(byIds(userAnswers1.userId, userAnswers1.operatorId, userAnswers1.reportingPeriod)).futureValue.headOption.value
      val record2 = find(byIds(userAnswers2.userId, userAnswers2.operatorId, userAnswers2.reportingPeriod)).futureValue.headOption.value

      record1 mustEqual expectedRecord1
      record2 mustEqual userAnswers2
    }

    "must store the data section as encrypted bytes" in {

      repository.set(userAnswers1).futureValue

      val record = repository.collection
        .find[BsonDocument](Filters.equal("userId", userAnswers1.userId))
        .headOption()
        .futureValue
        .value

      val json = Json.parse(record.toJson)

      val data = (json \ "data").as[String]

      assertThrows[JsonParseException] {
        Json.parse(data)
      }
    }

    mustPreserveMdc(repository.set(userAnswers1))
  }

  ".get" - {

    "when there is a record for this user id and operator id" - {

      "must update the lastUpdated time of all of this user's records and get the specific record" in {

        insert(userAnswers1).futureValue
        insert(userAnswers2).futureValue

        val result1         = repository.get(userAnswers1.userId, userAnswers1.operatorId, userAnswers1.reportingPeriod).futureValue
        val result2         = repository.get(userAnswers2.userId, userAnswers2.operatorId, userAnswers2.reportingPeriod).futureValue
        val expectedResult1 = userAnswers1.copy(lastUpdated = instant)
        val expectedResult2 = userAnswers2.copy(lastUpdated = instant)

        result1.value mustEqual expectedResult1
        result2.value mustEqual expectedResult2
      }
    }

    "when there is no record for this id" - {

      "must return None" in {

        repository.get("id that does not exist", "foo", None).futureValue must not be defined
      }
    }

    mustPreserveMdc(repository.get(userAnswers1.userId, userAnswers1.operatorId, userAnswers1.reportingPeriod))
  }

  ".clear" - {

    "must remove a record when an operator id and reportingPeriod is specified" in {

      insert(userAnswers1).futureValue
      insert(userAnswers2).futureValue

      repository.clear(userAnswers1.userId, userAnswers1.operatorId, userAnswers1.reportingPeriod).futureValue

      repository.get(userAnswers1.userId, userAnswers1.operatorId, userAnswers1.reportingPeriod).futureValue must not be defined
      repository.get(userAnswers2.userId, userAnswers2.operatorId, userAnswers2.reportingPeriod).futureValue mustBe defined
    }

    "must remove all records for a user when an operator id is not specified" in {

      insert(userAnswers1).futureValue
      insert(userAnswers2).futureValue

      repository.clear(userAnswers1.userId).futureValue

      repository.get(userAnswers1.userId, userAnswers1.operatorId, userAnswers1.reportingPeriod).futureValue must not be defined
      repository.get(userAnswers2.userId, userAnswers2.operatorId, userAnswers2.reportingPeriod).futureValue must not be defined
    }

    "must return true when there is no record to remove" in {
      val result = repository.clear("id that does not exist", "foo", None).futureValue

      result mustEqual true
    }

    "when operator id and case id are not specified" - {

      mustPreserveMdc(repository.clear(userAnswers1.userId))
    }

    "when operator id and case id are specified" - {

      mustPreserveMdc(repository.clear(userAnswers1.userId, userAnswers1.operatorId, userAnswers1.reportingPeriod))
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

        val updatedAnswers1 = find(byIds(userAnswers1.userId, userAnswers1.operatorId, userAnswers1.reportingPeriod)).futureValue.headOption.value
        val updatedAnswers2 = find(byIds(userAnswers2.userId, userAnswers2.operatorId, userAnswers2.reportingPeriod)).futureValue.headOption.value
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
