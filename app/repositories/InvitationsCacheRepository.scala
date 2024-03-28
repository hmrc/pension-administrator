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

import com.google.inject.{Inject, Singleton}
import com.mongodb.client.model.FindOneAndUpdateOptions
import models.Invitation
import org.mongodb.scala.bson.BsonBinary
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import play.api.libs.json._
import play.api.{Configuration, Logging}
import repositories.InvitationsCacheEntry.InvitationsCacheEntryFormats.{expireAtKey, inviteePsaIdKey, pstrKey}
import repositories.InvitationsCacheEntry._
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainText, SymmetricCryptoFactory}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoBinaryFormats.{byteArrayReads, byteArrayWrites}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}


object InvitationsCacheEntry {

  sealed trait InvitationsCacheEntry

  case class DataEntry(inviteePsaId: String, pstr: String, data: BsonBinary, lastUpdated: Instant, expireAt: Instant) extends InvitationsCacheEntry

  case class JsonDataEntry(inviteePsaId: String, pstr: String, data: JsValue, lastUpdated: Instant, expireAt: Instant) extends InvitationsCacheEntry

  object DataEntry {
    def apply(inviteePsaId: String,
              pstr: String,
              data: Array[Byte],
              lastUpdated: Instant = Instant.now(),
              expireAt: Instant): DataEntry = {

      DataEntry(inviteePsaId, pstr, BsonBinary(data), lastUpdated, expireAt)
    }

    final val bsonBinaryReads: Reads[BsonBinary] = byteArrayReads.map(BsonBinary(_))
    final val bsonBinaryWrites: Writes[BsonBinary] = byteArrayWrites.contramap(_.getData)
    implicit val bsonBinaryFormat: Format[BsonBinary] = Format(bsonBinaryReads, bsonBinaryWrites)
    implicit val dateFormats: Format[Instant] = MongoJavatimeFormats.instantFormat
    implicit val format: Format[DataEntry] = Json.format[DataEntry]
  }

  object JsonDataEntry {
    def applyJsonDataEntry(inviteePsaId: String,
                           pstr: String,
                           data: JsValue,
                           lastUpdated: Instant = Instant.now(),
                           expireAt: Instant): JsonDataEntry = {

      JsonDataEntry(inviteePsaId, pstr, data, lastUpdated, expireAt)
    }

    implicit val dateFormats: Format[Instant] = MongoJavatimeFormats.instantFormat
    implicit val format: Format[JsonDataEntry] = Json.format[JsonDataEntry]
  }

  object InvitationsCacheEntryFormats {
    implicit val dateFormats: Format[Instant] = MongoJavatimeFormats.instantFormat
    implicit val format: Format[InvitationsCacheEntry] = Json.format[InvitationsCacheEntry]

    val pstrKey = "pstr"
    val inviteePsaIdKey = "inviteePsaId"
    val expireAtKey = "expireAt"
    val lastUpdatedKey = "lastUpdated"
    val dataKey = "data"
  }
}

@Singleton
class InvitationsCacheRepository @Inject()(
                                            mongoComponent: MongoComponent,
                                            config: Configuration
                                          )(implicit val ec: ExecutionContext)
  extends PlayMongoRepository[InvitationsCacheEntry](
    collectionName = config.underlying.getString("mongodb.pension-administrator-cache.invitations.name"),
    mongoComponent = mongoComponent,
    domainFormat = InvitationsCacheEntryFormats.format,
    extraCodecs = Seq(
      Codecs.playFormatCodec(JsonDataEntry.format),
      Codecs.playFormatCodec(DataEntry.format),
      Codecs.playFormatCodec(MongoJavatimeFormats.instantFormat)
    ),
    indexes = Seq(
      IndexModel(
        Indexes.ascending(expireAtKey),
        IndexOptions().name("dataExpiry").expireAfter(0, TimeUnit.SECONDS).background(true)
      ),
      IndexModel(
        Indexes.ascending(inviteePsaIdKey, pstrKey),
        IndexOptions().name("inviteePsaId_Pstr").unique(true).background(true)
      ),
      IndexModel(
        Indexes.ascending(pstrKey),
        IndexOptions().name(pstrKey).background(true)
      )
    )
  ) with Logging {

  import InvitationsCacheEntryFormats._

  private val encryptionKey: String = "manage.json.encryption"
  private val encrypted: Boolean = config.getOptional[Boolean]("encrypted").getOrElse(true)
  private val jsonCrypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesCryptoFromConfig(baseConfigKey = encryptionKey, config.underlying)

  def upsert(invitation: Invitation)(implicit ec: ExecutionContext): Future[Unit] = {
    if (encrypted) {
      val encryptedInviteePsaId = jsonCrypto.encrypt(PlainText(invitation.inviteePsaId.value)).value
      val encryptedPstr = jsonCrypto.encrypt(PlainText(invitation.pstr)).value

      val unencrypted = PlainText(Json.stringify(Json.toJson(invitation)))
      val encryptedData = jsonCrypto.encrypt(unencrypted).value
      val dataAsByteArray: Array[Byte] = encryptedData.getBytes("UTF-8")
      val dataEntry = DataEntry(encryptedInviteePsaId, encryptedPstr, dataAsByteArray, expireAt = invitation.expireAt)
      val modifier = Updates.combine(
        Updates.set(inviteePsaIdKey, dataEntry.inviteePsaId),
        Updates.set(pstrKey, dataEntry.pstr),
        Updates.set(dataKey, dataEntry.data),
        Updates.set(lastUpdatedKey, Codecs.toBson(dataEntry.lastUpdated)(MongoJavatimeFormats.instantFormat)),
        Updates.set(expireAtKey, Codecs.toBson(dataEntry.expireAt)(MongoJavatimeFormats.instantFormat))
      )
      val selector = Filters.and(Filters.equal(inviteePsaIdKey, encryptedInviteePsaId), Filters.equal(pstrKey, encryptedPstr))
      collection.withDocumentClass[DataEntry]().findOneAndUpdate(
        filter = selector,
        update = modifier, new FindOneAndUpdateOptions().upsert(true)).toFuture().map(_ => ())
    } else {
      val record = JsonDataEntry.applyJsonDataEntry(invitation.inviteePsaId.value,
        invitation.pstr, Json.toJson(invitation), expireAt = invitation.expireAt)

      val modifier = Updates.combine(
        Updates.set(inviteePsaIdKey, record.inviteePsaId),
        Updates.set(pstrKey, record.pstr),
        Updates.set(dataKey, Codecs.toBson(record.data)),
        Updates.set(lastUpdatedKey, Codecs.toBson(record.lastUpdated)(MongoJavatimeFormats.instantFormat)),
        Updates.set(expireAtKey, Codecs.toBson(record.expireAt)(MongoJavatimeFormats.instantFormat))
      )
      val selector = Filters.and(Filters.equal(inviteePsaIdKey, invitation.inviteePsaId.value), Filters.equal(pstrKey, invitation.pstr))

      collection.withDocumentClass[JsonDataEntry]().findOneAndUpdate(
        filter = selector,
        update = modifier, new FindOneAndUpdateOptions().upsert(true)).toFuture().map(_ => ())
    }
  }

  private def filterEncryptKeys(mapOfKeys: Map[String, String]): Bson = {
    val filters = mapOfKeys.map {
      case key if key._1 == inviteePsaIdKey || key._1 == pstrKey =>
        val encryptedValue = jsonCrypto.encrypt(PlainText(key._2)).value
        Filters.equal(key._1, encryptedValue)
      case key => Filters.equal(key._1, key._2)
    }.toList
    Filters.and(filters: _*)
  }

  def getByKeys(mapOfKeys: Map[String, String])(implicit ec: ExecutionContext): Future[Option[List[Invitation]]] = {
    if (encrypted) {
      collection.find[DataEntry](filterEncryptKeys(mapOfKeys)).toFuture().map { res =>
        val listOfInvitationsJson = res.map {
          dataEntry =>
            val dataAsString = new String(dataEntry.data.getData, StandardCharsets.UTF_8)
            val decrypted: PlainText = jsonCrypto.decrypt(Crypted(dataAsString))
            Json.parse(decrypted.value)
        }.toList
        listToOption(listOfInvitationsJson)
      }
    } else {
      val filters = mapOfKeys.map(t => Filters.equal(t._1, t._2)).toList
      collection.find[JsonDataEntry](Filters.and(filters: _*)).toFuture().map { res =>
        val listOfInvitationsJson = res.map {
          dataEntry =>
            dataEntry.data
        }.toList
        listToOption(listOfInvitationsJson)
      }
    }
  }

  private def listToOption(data: List[JsValue]): Option[List[Invitation]] = {
    if (data.isEmpty) {
      None
    } else {
      val invitationsList = data.map {
        _.validate[Invitation] match {
          case JsSuccess(value, _) => value
          case JsError(errors) => throw JsResultException(errors)
        }
      }
      Some(invitationsList)
    }
  }

  def remove(mapOfKeys: Map[String, String])(implicit ec: ExecutionContext): Future[Boolean] = {
    val selector = if (encrypted) {
      filterEncryptKeys(mapOfKeys)
    } else {
      val filters = mapOfKeys.map(t => Filters.equal(t._1, t._2)).toList
      Filters.and(filters: _*)
    }
    collection.deleteOne(selector).toFuture().map { result =>
      logger.info(s"Removing row from collection $collectionName")
      result.wasAcknowledged
    }
  }
}
