/*
 * Copyright 2019 HM Revenue & Customs
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

import java.nio.charset.StandardCharsets

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._
import play.api.{Configuration, Logger}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.Subtype.GenericBinarySubtype
import reactivemongo.bson.{BSONBinary, BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.crypto.{Crypted, CryptoWithKeysFromConfig, PlainText}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}

abstract class ManageCacheRepository(
                                              index: String,
                                              ttl: Option[Int],
                                              component: ReactiveMongoComponent,
                                              encryptionKey: String,
                                              config: Configuration
                                            )(implicit ec: ExecutionContext) extends ReactiveRepository[JsValue, BSONObjectID](
  index,
  component.mongoConnector.db,
  implicitly
) {
  private val encrypted: Boolean = config.getOptional[Boolean]("encrypted").getOrElse(true)
  private val jsonCrypto: CryptoWithKeysFromConfig = new CryptoWithKeysFromConfig(baseConfigKey = encryptionKey, config.underlying)

  private case class DataEntry(
                                id: String,
                                data: BSONBinary,
                                lastUpdated: DateTime)
  // scalastyle:off magic.number
  private object DataEntry {
    def apply(id: String,
              data: Array[Byte],
              lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC)): DataEntry =
     DataEntry(id, BSONBinary(data, GenericBinarySubtype), lastUpdated)

      private implicit val dateFormat: Format[DateTime] =
      ReactiveMongoFormats.dateTimeFormats
      implicit val format: Format[DataEntry] = Json.format[DataEntry]
    }

  // scalastyle:on magic.number

  private case class JsonDataEntry(
                                    id: String,
                                    data: JsValue,
                                    lastUpdated: DateTime
                                  )

  private object JsonDataEntry {
    private implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
    implicit val format: Format[JsonDataEntry] = Json.format[JsonDataEntry]
  }

  private val fieldName = "lastUpdated"
  private val createdIndexName = "dataExpiry"
  private val expireAfterSeconds = "expireAfterSeconds"

  (for {
    _ <- CollectionDiagnostics.checkIndexTtl(collection, createdIndexName, ttl)
    _ <- ensureIndex(fieldName, createdIndexName, ttl)
  } yield {
    ()
  }) recoverWith {
    case t: Throwable => Future.successful(Logger.error(s"Error ensuring indexes on collection ${collection.name}", t))
  } andThen {
    case _ => CollectionDiagnostics.logCollectionInfo(collection)
  }

  private def ensureIndex(field: String, indexName: String, ttl: Option[Int]): Future[Boolean] = {

    val defaultIndex: Index = Index(Seq((field, IndexType.Ascending)), Some(indexName))

    val index: Index = ttl.fold(defaultIndex) { ttl =>
      Index(
        Seq((field, IndexType.Ascending)),
        Some(indexName),
        background = true,
        options = BSONDocument(expireAfterSeconds -> ttl)
      )
    }

    collection.indexesManager.ensure(index) map {
      result => {
        Logger.warn(s"Created index $indexName on collection ${collection.name} with TTL value $ttl -> result: $result")
        result
      }
    } recover {
      case e => Logger.error("Failed to set TTL index", e)
        false
    }
  }

  def upsert(id: String, data: JsValue)(implicit ec: ExecutionContext): Future[Boolean] = {
    val document: JsValue = {
      if (encrypted) {
        val unencrypted = PlainText(Json.stringify(data))
        val encryptedData = jsonCrypto.encrypt(unencrypted).value
        val dataAsByteArray: Array[Byte] = encryptedData.getBytes("UTF-8")
        Json.toJson(DataEntry(id, dataAsByteArray))
      } else
        Json.toJson(JsonDataEntry(id, data, DateTime.now(DateTimeZone.UTC)))
    }
    val selector = BSONDocument("id" -> id)
    val modifier = BSONDocument("$set" -> document)
    collection.update(ordered = false).one(selector, modifier, upsert = true)
      .map(_.ok)
  }

  def get(id: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    if (encrypted) {
      collection.find(BSONDocument("id" -> id), projection = Option.empty[JsObject]).one[DataEntry].map {
        _.map {
          dataEntry =>
            val dataAsString = new String(dataEntry.data.byteArray, StandardCharsets.UTF_8)
            val decrypted: PlainText = jsonCrypto.decrypt(Crypted(dataAsString))
            Json.parse(decrypted.value)
        }
      }
    } else {
      collection.find(BSONDocument("id" -> id), projection = Option.empty[JsObject]).one[JsonDataEntry].map {
        _.map {
          dataEntry =>
            dataEntry.data
        }
      }
    }
  }

  def getLastUpdated(id: String)(implicit ec: ExecutionContext): Future[Option[DateTime]] = {
    if (encrypted) {
      collection.find(BSONDocument("id" -> id), projection = Option.empty[JsObject]).one[DataEntry].map {
        _.map {
          dataEntry =>
            dataEntry.lastUpdated
        }
      }
    } else {
      collection.find(BSONDocument("id" -> id), projection = Option.empty[JsObject]).one[JsonDataEntry].map {
        _.map {
          dataEntry =>
            dataEntry.lastUpdated
        }
      }
    }
  }

  def remove(id: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    Logger.warn(s"Removing row from collection ${collection.name} externalId:$id")
    val selector = BSONDocument("id" -> id)
    collection.delete().one(selector).map(_.ok)
  }

}
