/*
 * Copyright 2022 HM Revenue & Customs
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
import com.google.inject.Inject
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{Json, _}
import play.api.Configuration
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.Subtype.GenericBinarySubtype
import reactivemongo.bson.{BSONBinary, BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.crypto.{Crypted, CryptoWithKeysFromConfig, PlainText}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}

class PSADataCacheRepository @Inject()(
                                        mongoComponent: ReactiveMongoComponent,
                                        configuration: Configuration
                                      )(implicit val ec: ExecutionContext)
  extends ReactiveRepository[JsValue, BSONObjectID](
    collectionName = configuration.get[String](path = "mongodb.pension-administrator-cache.psa-data.name"),
    mongo = mongoComponent.mongoConnector.db,
    domainFormat = implicitly
  ) {

  override val logger: Logger = LoggerFactory.getLogger("PSADataCacheRepository")
  private val encryptionKey = "psa.json.encryption"
  private val jsonCrypto: CryptoWithKeysFromConfig = new CryptoWithKeysFromConfig(baseConfigKey = encryptionKey, configuration.underlying)
  private val encrypted: Boolean = configuration.get[Boolean](path = "encrypted")
  private val expireInDays = configuration.get[Int](path = "mongodb.pension-administrator-cache.psa-data.timeToLiveInDays")

  private def expireAt: DateTime = DateTime.now(DateTimeZone.UTC).toLocalDate.plusDays(expireInDays + 1).toDateTimeAtStartOfDay()

  private val ttl = 0

  (for {
    _ <- ensureIndex(fields = Seq("expireAt"), indexName = "dataExpiry", ttl = Some(ttl))
    _ <- ensureIndex(fields = Seq("id"), indexName = "credId", isUnique = true)
  } yield {
    ()
  }) recoverWith {
    case t: Throwable => Future.successful(logger.error(s"Error ensuring indexes on collection ${collection.name}", t))
  } andThen {
    case _ => CollectionDiagnostics.logCollectionInfo(collection)
  }

  private def ensureIndex(fields: Seq[String], indexName: String, ttl: Option[Int] = None, isUnique: Boolean = false): Future[Boolean] = {
    val fieldIndexes = fields.map((_, IndexType.Ascending))
    val index = {
      val defaultIndex = Index(fieldIndexes, Some(indexName), background = true, unique = isUnique)
      ttl.map(ttl => defaultIndex.copy(options = BSONDocument("expireAfterSeconds" -> ttl))).fold(defaultIndex)(identity)
    }

    val indexCreationDescription = {
      def addExpireAfterSecondsDescription(s: String): String = ttl.fold(s)(ttl => s"$s (expireAfterSeconds = $ttl)")

      addExpireAfterSecondsDescription(s"Attempt to create Mongo index $index (unique = ${index.unique}))")
    }

    collection.indexesManager.ensure(index) map { result =>
      logger.debug(indexCreationDescription + s" was successful and result is: $result")
      result
    } recover {
      case e => logger.error(indexCreationDescription + s" was unsuccessful", e)
        false
    }
  }

  def upsert(credId: String, userData: JsValue)(implicit ec: ExecutionContext): Future[Boolean] = {
    val document: JsValue = {
      if (encrypted) {
        val unencrypted = PlainText(Json.stringify(userData))
        val encryptedData = jsonCrypto.encrypt(unencrypted).value
        val dataAsByteArray: Array[Byte] = encryptedData.getBytes("UTF-8")
        Json.toJson(EncryptedDataEntry(id = credId, data = dataAsByteArray, expireAt = expireAt))
      } else
        Json.toJson(DataEntryWithoutEncryption.applyDataEntry(id = credId, data = userData, expireAt = expireAt))
    }
    val selector = BSONDocument("id" -> credId)
    val modifier = BSONDocument("$set" -> document)
    collection.update(ordered = false).one(selector, modifier, upsert = true).map(_.ok)
  }

  def get(credId: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    if (encrypted) {
      collection.find(BSONDocument("id" -> credId), projection = Option.empty[JsObject]).one[EncryptedDataEntry].map {
        _.map {
          dataEntry =>
            val dataAsString = new String(dataEntry.data.byteArray, StandardCharsets.UTF_8)
            val decrypted: PlainText = jsonCrypto.decrypt(Crypted(dataAsString))
            Json.parse(decrypted.value).as[JsObject] ++ Json.obj("expireAt" -> JsNumber(dataEntry.expireAt.getMillis))
        }
      }
    } else {
      collection.find(BSONDocument("id" -> credId), projection = Option.empty[JsObject]).one[DataEntryWithoutEncryption].map {
        _.map {
          dataEntry =>
            dataEntry.data.as[JsObject] ++ Json.obj("expireAt" -> JsNumber(dataEntry.expireAt.getMillis))
        }
      }
    }
  }

  def remove(credId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    logger.warn(s"Removing row from collection ${collection.name} credId:$credId")
    val selector = BSONDocument("id" -> credId)
    collection.delete().one(selector).map(_.ok)
  }

  private case class EncryptedDataEntry(
                                         id: String,
                                         data: BSONBinary,
                                         lastUpdated: DateTime,
                                         expireAt: DateTime)

  private object EncryptedDataEntry {

    def apply(id: String,
              data: Array[Byte],
              lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC),
              expireAt: DateTime): EncryptedDataEntry = {

      EncryptedDataEntry(id, BSONBinary(data, GenericBinarySubtype), lastUpdated, expireAt)
    }

    implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
    implicit val format: Format[EncryptedDataEntry] = Json.format[EncryptedDataEntry]
  }

  private case class DataEntryWithoutEncryption(
                                                 id: String,
                                                 data: JsValue,
                                                 lastUpdated: DateTime,
                                                 expireAt: DateTime
                                               )

  private object DataEntryWithoutEncryption {

    def applyDataEntry(id: String,
                       data: JsValue,
                       lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC),
                       expireAt: DateTime): DataEntryWithoutEncryption = {

      DataEntryWithoutEncryption(id, data, lastUpdated, expireAt)
    }

    implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
    implicit val format: Format[DataEntryWithoutEncryption] = Json.format[DataEntryWithoutEncryption]
  }

}
