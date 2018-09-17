/*
 * Copyright 2018 HM Revenue & Customs
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

import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.Subtype.GenericBinarySubtype
import reactivemongo.bson.{BSONBinary, BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import utils.DateUtils

import scala.concurrent.{ExecutionContext, Future}

abstract class PensionAdministratorCacheRepository(
                                              index: String,
                                              ttl: Option[Int],
                                              component: ReactiveMongoComponent
                                            ) extends ReactiveRepository[JsValue, BSONObjectID](
  index,
  component.mongoConnector.db,
  implicitly
) {

  private case class DataEntry(
                                id: String,
                                data: BSONBinary,
                                lastUpdated: DateTime,
                                expireAt: Option[DateTime])
  // scalastyle:off magic.number
  private object DataEntry {
    def apply(id: String,
              data: Array[Byte],
              lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC),
              expireAt: Option[DateTime] = None): DataEntry = {
      ttl match {
        case None => DataEntry(id, BSONBinary(data, GenericBinarySubtype), lastUpdated, None)
        case Some(_) => DataEntry(id, BSONBinary(
          data,
          GenericBinarySubtype),
          lastUpdated,
          Some(DateUtils.dateTimeFromDateToMidnightOnDay(DateTime.now(DateTimeZone.UTC), 30)))
      }
    }

      private implicit val dateFormat: Format[DateTime] =
      ReactiveMongoFormats.dateTimeFormats
      implicit val format: Format[DataEntry] = Json.format[DataEntry]
    }

  // scalastyle:on magic.number

  private val fieldName = "expireAt"
  private val createdIndexName = "dataExpiry"
  private val expireAfterSeconds = "expireAfterSeconds"

  ensureIndex(fieldName, createdIndexName, ttl)

  private def ensureIndex(field: String, indexName: String, ttl: Option[Int]): Future[Boolean] = {

    import scala.concurrent.ExecutionContext.Implicits.global

    val defaultIndex: Index = Index(Seq((field, IndexType.Ascending)), Some(indexName))

    val index: Index = ttl.fold(defaultIndex) { ttl =>
      Index(
        Seq((field, IndexType.Ascending)),
        Some(indexName),
        options = BSONDocument(expireAfterSeconds -> ttl)
      )
    }

    collection.indexesManager.ensure(index) map {
      result => {
        Logger.debug(s"set [$indexName] with value $ttl -> result : $result")
        result
      }
    } recover {
      case e => Logger.error("Failed to set TTL index", e)
        false
    }
  }

  def upsert(id: String, data: Array[Byte])(implicit ec: ExecutionContext): Future[Boolean] = {

    val document = Json.toJson(DataEntry(id, data))
    val selector = BSONDocument("id" -> id)
    val modifier = BSONDocument("$set" -> document)

    collection.update(selector, modifier, upsert = true)
      .map(_.ok)
  }

  def get(id: String)(implicit ec: ExecutionContext): Future[Option[Array[Byte]]] = {
    collection.find(BSONDocument("id" -> id)).one[DataEntry].map {
      _.map {
        dataEntry =>
          dataEntry.data.byteArray
      }
    }
  }

  def getLastUpdated(id: String)(implicit ec: ExecutionContext): Future[Option[DateTime]] = {
    collection.find(BSONDocument("id" -> id)).one[DataEntry].map {
      _.map {
        dataEntry =>
          dataEntry.lastUpdated
      }
    }
  }

  def remove(id: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val selector = BSONDocument("id" -> id)
    collection.remove(selector).map(_.ok)
  }
}
