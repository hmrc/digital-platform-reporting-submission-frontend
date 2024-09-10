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

import models.upscan.UpscanJourney
import org.mongodb.scala.model.Indexes
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import repositories.UpscanJourneyRepository.UpscanJourneyAlreadyExistsException
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneOffset}
import java.util.concurrent.TimeUnit

class UpscanJourneyRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[UpscanJourney]
    with IntegrationPatience
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with GuiceOneAppPerSuite
    with OptionValues {

  private val now: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
  private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),
      bind[Clock].toInstance(clock)
    )
    .configure(
      "mongodb.upscan-journey.ttl" -> "5minutes"
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  override protected val repository: UpscanJourneyRepository =
    app.injector.instanceOf[UpscanJourneyRepository]

  "must have the correct ttl index" in {
    val ttlIndex = repository.indexes.find(_.getOptions.getName == "created_ttl_idx").value
    ttlIndex.getOptions.getExpireAfter(TimeUnit.MINUTES) mustEqual 5
    ttlIndex.getKeys mustEqual Indexes.ascending("createdAt")
  }

  "initiate" - {

    "must insert a journey if there is no journey for that id" in {

      val expectedJourney = UpscanJourney(
        dprsId = "dprsId",
        submissionId = "submissionId",
        uploadId = "uploadId",
        createdAt = now
      )

      repository.initiate("uploadId", "dprsId", "submissionId").futureValue
      findAll().futureValue must contain only(expectedJourney)
    }

    "must fail if there is already a journey for that upload id" in {

      repository.initiate("uploadId", "dprsId", "submissionId").futureValue
      val result = repository.initiate("uploadId", "dprsId", "submissionId").failed.futureValue

      result mustBe a[UpscanJourneyAlreadyExistsException]
    }
  }

  "getByUploadId" - {

    "must return the journey if there is one that matches the given upload id" in {

      val expectedJourney = UpscanJourney(
        dprsId = "dprsId",
        submissionId = "submissionId",
        uploadId = "uploadId",
        createdAt = now
      )

      insert(expectedJourney).futureValue

      val result = repository.getByUploadId("uploadId").futureValue.value
      result mustEqual expectedJourney
    }

    "must return None if there is no journey for the given upload id" in {
      val result = repository.getByUploadId("uploadId").futureValue
      result mustBe None
    }
  }
}
