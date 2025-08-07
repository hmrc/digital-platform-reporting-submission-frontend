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

import com.mongodb.client.model.IndexModel
import models.upscan.UpscanJourney
import org.apache.pekko.Done
import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.model.*
import play.api.Configuration
import repositories.UpscanJourneyRepository.UpscanJourneyAlreadyExistsException
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Clock, Duration}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpscanJourneyRepository @Inject()(
                                         mongoComponent: MongoComponent,
                                         configuration: Configuration,
                                         clock: Clock
                                       )(using ExecutionContext) extends PlayMongoRepository[UpscanJourney](
  collectionName = "uploaded-files",
  mongoComponent = mongoComponent,
  domainFormat   = UpscanJourney.mongoFormat,
  indexes        = UpscanJourneyRepository.indexes(configuration)
) {

  def initiate(uploadId: String, dprsId: String, submissionId: String): Future[Done] =
    collection.insertOne(UpscanJourney(
      dprsId = dprsId,
      submissionId = submissionId,
      uploadId = uploadId,
      createdAt = clock.instant()
    )).toFuture().map(_ => Done).recoverWith {
      case e: MongoWriteException if e.getCode == 11000 =>
        Future.failed(UpscanJourneyAlreadyExistsException(uploadId))
    }

  def getByUploadId(id: String): Future[Option[UpscanJourney]] =
    collection.find(Filters.eq("uploadId", id)).limit(1).headOption()
}

object UpscanJourneyRepository {

  def indexes(configuration: Configuration): Seq[IndexModel] = Seq(
    IndexModel(
      Indexes.ascending("createdAt"),
      IndexOptions()
        .name("created_ttl_idx")
        .expireAfter(configuration.get[Duration]("mongodb.upscan-journey.ttl").toMinutes, TimeUnit.MINUTES)
    ),
    IndexModel(
      Indexes.ascending("uploadId"),
      IndexOptions()
        .name("uploadId")
        .unique(true)
    )
  )

  final case class UpscanJourneyAlreadyExistsException(uploadId: String) extends Throwable
}

