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

import com.google.inject.Inject
import com.mongodb.client.model.FindOneAndUpdateOptions
import org.mongodb.scala.bson.BsonBinary
import org.mongodb.scala.model._
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import play.api.{Configuration, Logging}
import repositories.PSADataCacheEntry.{DataEntryWithoutEncryption, EncryptedDataEntry, PSADataCacheEntry, PSADataCacheEntryFormats}
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainText, SymmetricCryptoFactory}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoBinaryFormats.{byteArrayReads, byteArrayWrites}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

object PSADataCacheEntry {

  sealed trait PSADataCacheEntry

  case class EncryptedDataEntry(
                                 id: String,
                                 data: BsonBinary,
                                 lastUpdated: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                                 expireAt: Instant) extends PSADataCacheEntry

  case class DataEntryWithoutEncryption(
                                         id: String,
                                         data: JsValue,
                                         lastUpdated: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                                         expireAt: Instant) extends PSADataCacheEntry

  object EncryptedDataEntry {
    private val dateReads = new Reads[Instant] {
      def reads(json: JsValue): JsResult[Instant] = {
        val result = json.asOpt[String].map { date =>
          LocalDateTime.parse(
              date ,
              DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            )
            .atZone(
              ZoneId.of("UTC")
            )
            .toInstant
        }.getOrElse(json.as[Instant](MongoJavatimeFormats.instantReads))
        JsSuccess(result)
      }
    }

    implicit val invitationReads: Reads[EncryptedDataEntry] = (
      (JsPath \ "id").read[String] and
        (JsPath \ "data").read[Array[Byte]] and
        (JsPath \ "lastUpdated").read[Instant](dateReads) and
        (JsPath \ "expireAt").read[Instant](dateReads)
      )((id, data, lastUpdated , expireAt) =>
      EncryptedDataEntry(id, BsonBinary(data), lastUpdated, expireAt)
    )
//    def apply(id: String,
//              data: Array[Byte],
//              lastUpdated: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS),
//              expireAt: Instant): EncryptedDataEntry =
//      EncryptedDataEntry(id, BsonBinary(data), lastUpdated, expireAt)

    final val bsonBinaryReads: Reads[BsonBinary] = byteArrayReads.map(t => BsonBinary(t))
    final val bsonBinaryWrites: Writes[BsonBinary] = byteArrayWrites.contramap(t => t.getData)
    implicit val bsonBinaryFormat: Format[BsonBinary] = Format(bsonBinaryReads, bsonBinaryWrites)
    implicit val dateFormats: Format[Instant] = MongoJavatimeFormats.instantFormat
    implicit val format: Format[EncryptedDataEntry] = Json.format[EncryptedDataEntry]
  }

  object DataEntryWithoutEncryption {
    private val dateReads = new Reads[Instant] {
      def reads(json: JsValue): JsResult[Instant] = {
        val result = json.asOpt[String].map { date =>
          LocalDateTime.parse(
              date ,
              DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            )
            .atZone(
              ZoneId.of("UTC")
            )
            .toInstant
        }.getOrElse(json.as[Instant](MongoJavatimeFormats.instantReads))
        JsSuccess(result)
      }
    }

    implicit val invitationReads: Reads[DataEntryWithoutEncryption] = (
      (JsPath \ "id").read[String] and
        (JsPath \ "data").read[JsValue] and
        (JsPath \ "lastUpdated").read[Instant](dateReads) and
        (JsPath \ "expireAt").read[Instant](dateReads)
      )((id, data, lastUpdated, expireAt) =>
      DataEntryWithoutEncryption(id, data, lastUpdated, expireAt)
    )
//    def applyDataEntry(id: String,
//                       data: JsValue,
//                       lastUpdated: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS),
//                       expireAt: Instant): DataEntryWithoutEncryption =
//      DataEntryWithoutEncryption(id, data, lastUpdated, expireAt)

    implicit val dateFormats: Format[Instant] = MongoJavatimeFormats.instantFormat
    implicit val format: Format[DataEntryWithoutEncryption] = Json.format[DataEntryWithoutEncryption]
  }

  object PSADataCacheEntryFormats {
    implicit val dateFormats: Format[Instant] = MongoJavatimeFormats.instantFormat
    implicit val format: Format[PSADataCacheEntry] = Json.format[PSADataCacheEntry]
  }
}

@Singleton
class PSADataCacheRepository @Inject()(
                                        mongoComponent: MongoComponent,
                                        configuration: Configuration
                                      )(implicit val ec: ExecutionContext)
  extends PlayMongoRepository[PSADataCacheEntry](
    collectionName = configuration.get[String](path = "mongodb.pension-administrator-cache.psa-data.name"),
    mongoComponent = mongoComponent,
    domainFormat = PSADataCacheEntryFormats.format,
    extraCodecs = Seq(
      Codecs.playFormatCodec(DataEntryWithoutEncryption.format),
      Codecs.playFormatCodec(EncryptedDataEntry.format),
      Codecs.playFormatCodec(MongoJavatimeFormats.instantFormat)
    ),
    indexes = Seq(
      IndexModel(
        Indexes.ascending("id"),
        IndexOptions().name("credId").unique(true).background(true)
      ),
      IndexModel(
        Indexes.ascending("expireAt"),
        IndexOptions().name("dataExpiry").expireAfter(0, TimeUnit.SECONDS).background(true)
      )
    )
  ) with Logging {

  private val encryptionKey = "psa.json.encryption"
  private val jsonCrypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesCryptoFromConfig(baseConfigKey = encryptionKey, configuration.underlying)
  private val encrypted: Boolean = configuration.get[Boolean](path = "encrypted")
  private val expireInDays = configuration.get[Int](path = "mongodb.pension-administrator-cache.psa-data.timeToLiveInDays")
  private val idField: String = "id"

  private def evaluatedExpireAt: Instant = LocalDateTime.now(ZoneId.of("UTC")).toLocalDate.plusDays(expireInDays + 1).atStartOfDay(ZoneId.of("UTC")).toInstant

  def upsert(credId: String, userData: JsValue)(implicit ec: ExecutionContext): Future[Unit] = {
    if (encrypted) {
      val unencrypted = PlainText(Json.stringify(userData))
      val encryptedData = jsonCrypto.encrypt(unencrypted).value
      val dataAsByteArray: Array[Byte] = encryptedData.getBytes("UTF-8")
      val encryptedDataEntry = EncryptedDataEntry(id = credId, data = BsonBinary(dataAsByteArray), expireAt = evaluatedExpireAt)
      val setOperation = Updates.combine(
        Updates.set(idField, encryptedDataEntry.id),
        Updates.set("data", encryptedDataEntry.data),
        Updates.set("lastUpdated", Codecs.toBson(encryptedDataEntry.lastUpdated)),
        Updates.set("expireAt", Codecs.toBson(encryptedDataEntry.expireAt))
      )
      collection.withDocumentClass[EncryptedDataEntry]().findOneAndUpdate(
        filter = Filters.eq(idField, credId),
        update = setOperation, new FindOneAndUpdateOptions().upsert(true))
        .toFuture().map(_ => ())
    } else {
      val dataEntryWithoutEncryption = DataEntryWithoutEncryption(id = credId, data = userData, expireAt = evaluatedExpireAt)
      val setOperation = Updates.combine(
        Updates.set(idField, dataEntryWithoutEncryption.id),
        Updates.set("data", Codecs.toBson(dataEntryWithoutEncryption.data)),
        Updates.set("lastUpdated", Codecs.toBson(dataEntryWithoutEncryption.lastUpdated)),
        Updates.set("expireAt", Codecs.toBson(dataEntryWithoutEncryption.expireAt))
      )
      collection.withDocumentClass[DataEntryWithoutEncryption]().findOneAndUpdate(
        filter = Filters.eq(idField, credId),
        update = setOperation, new FindOneAndUpdateOptions().upsert(true))
        .toFuture().map(_ => ())
    }
  }

  def get(credId: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    if (encrypted) {
      collection.find[EncryptedDataEntry](Filters.equal(idField, credId)).headOption().map {
        _.map {
          dataEntry =>
            val dataAsString = new String(dataEntry.data.getData, StandardCharsets.UTF_8)
            val decrypted: PlainText = jsonCrypto.decrypt(Crypted(dataAsString))
            Json.parse(decrypted.value).as[JsObject] ++ Json.obj("expireAt" -> JsNumber(dataEntry.expireAt.toEpochMilli))
        }
      }
    } else {
      collection.find[DataEntryWithoutEncryption](Filters.equal(idField, credId)).headOption().map {
        _.map {
          dataEntry =>
            val jsObject = dataEntry.data.as[JsObject]
            jsObject ++ Json.obj("expireAt" -> JsNumber(dataEntry.expireAt.getEpochSecond))
        }
      }
    }
  }

  def remove(credId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    collection.deleteOne(Filters.equal(idField, credId)).toFuture().map { result =>
      logger.info(s"Removing row from collection $collectionName credId:$credId")
      result.wasAcknowledged
    }
  }
}
