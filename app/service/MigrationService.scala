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

package service

import com.google.inject.Inject
import org.mongodb.scala.bson.{BsonDateTime, BsonDocument, BsonString}
import org.mongodb.scala.model.{Filters, Updates}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

class MigrationService @Inject()(mongoLockRepository: MongoLockRepository,
                                 mongoComponent: MongoComponent)(implicit ec: ExecutionContext) extends Logging {
  private val lock = LockService(mongoLockRepository, "minimal_detail_data_expireAtLock", Duration(10, TimeUnit.MINUTES))

  private def fixExpireAt(collectionName: String) = {
    val collection = mongoComponent.database.getCollection(collectionName)
    logger.info("[PODS-9319] Started minimal detail data migration for " + collectionName + " collection")
    collection.find(BsonDocument("lastUpdated" -> BsonDocument("$type" -> BsonString("string")))).toFuture().flatMap { seq =>
      val ftr = Future.sequence(seq.map(item => {
        val id = item.getObjectId("_id")
        val modifier = Updates.set("lastUpdated", BsonDateTime(java.util.Date.from(Instant.now().plusMillis(1000 * 60 * 5))))
        val selector = Filters.equal("_id", id)
        collection.findOneAndUpdate(selector, modifier).toFuture()
      }))
      val numberOfChanges = ftr.map(_.size)
      numberOfChanges.foreach(count => logger.info(s"[PODS-9319] Updated number of field $count from collection $collectionName"))
      numberOfChanges
    }
  }


  lock withLock {
    for {
      res <- fixExpireAt("minimal-detail")
    } yield res
  } map {
    case Some(result) =>
      logger.debug(s"[PODS-9319] data migration completed, $result rows were migrated successfully")
    case None => logger.debug(s"[PODS-9319] data migration locked by other instance")
  } recover {
    case e => logger.error("Locking finished with error", e)
  }
}
