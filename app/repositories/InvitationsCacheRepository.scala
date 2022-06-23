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

import com.google.inject.{Inject, Singleton}
import com.mongodb.client.model.FindOneAndUpdateOptions
import models.Invitation
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.bson.BsonBinary
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import play.api.libs.json._
import play.api.{Configuration, Logging}
import uk.gov.hmrc.crypto.{Crypted, CryptoWithKeysFromConfig, PlainText}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.mongo.play.json.formats.MongoBinaryFormats.{byteArrayReads, byteArrayWrites}
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import InvitationsCacheEntry._

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}


object InvitationsCacheEntry {

  sealed trait InvitationsCacheEntry

  case class DataEntry(inviteePsaId: String, pstr: String, data: BsonBinary, lastUpdated: DateTime, expireAt: DateTime) extends InvitationsCacheEntry

  case class JsonDataEntry(inviteePsaId: String, pstr: String, data: JsValue, lastUpdated: DateTime, expireAt: DateTime) extends InvitationsCacheEntry

  object DataEntry {
    def apply(inviteePsaId: String,
              pstr: String,
              data: Array[Byte],
              lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC),
              expireAt: DateTime): DataEntry = {

      DataEntry(inviteePsaId, pstr, BsonBinary(data), lastUpdated, expireAt)
    }

    final val bsonBinaryReads: Reads[BsonBinary] = byteArrayReads.map(BsonBinary(_))
    final val bsonBinaryWrites: Writes[BsonBinary] = byteArrayWrites.contramap(_.getData)
    implicit val bsonBinaryFormat: Format[BsonBinary] = Format(bsonBinaryReads, bsonBinaryWrites)

    implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat
    implicit val format: Format[DataEntry] = Json.format[DataEntry]
  }

  object JsonDataEntry {
    def applyJsonDataEntry(inviteePsaId: String,
                           pstr: String,
                           data: JsValue,
                           lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC),
                           expireAt: DateTime): JsonDataEntry = {

      JsonDataEntry(inviteePsaId, pstr, data, lastUpdated, expireAt)
    }

    implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat
    implicit val format: Format[JsonDataEntry] = Json.format[JsonDataEntry]
  }

  object InvitationsCacheEntryFormats{
    implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat
    implicit val format: Format[InvitationsCacheEntry] = Json.format[InvitationsCacheEntry]
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
    ),
    indexes = Seq(
      IndexModel(
        Indexes.ascending("expireAt"),
        IndexOptions().name("dataExpiry").expireAfter(0, TimeUnit.SECONDS).background(true)
      ),
      IndexModel(
        Indexes.ascending("inviteePsaId", "pstr"),
        IndexOptions().name("inviteePsaId_Pstr").unique(true).background(true)
      ),
      IndexModel(
        Indexes.ascending("pstr"),
        IndexOptions().name("pstr").background(true)
      )
    )
  ) with Logging {

  import InvitationsCacheEntryFormats._

  private val encryptionKey: String = "manage.json.encryption"

  private val encrypted: Boolean = config.getOptional[Boolean]("encrypted").getOrElse(true)
  private val jsonCrypto: CryptoWithKeysFromConfig = new CryptoWithKeysFromConfig(baseConfigKey = encryptionKey, config.underlying)

  private val inviteePsaIdKey = "inviteePsaId"
  private val pstrKey = "pstr"

  def upsert(invitation: Invitation)(implicit ec: ExecutionContext): Future[Boolean] = {
    val (selector, modifier) = if (encrypted) {
      val encryptedInviteePsaId = jsonCrypto.encrypt(PlainText(invitation.inviteePsaId.value)).value
      val encryptedPstr = jsonCrypto.encrypt(PlainText(invitation.pstr)).value

      val unencrypted = PlainText(Json.stringify(Json.toJson(invitation)))
      val encryptedData = jsonCrypto.encrypt(unencrypted).value
      val dataAsByteArray: Array[Byte] = encryptedData.getBytes("UTF-8")
      val dataEntry = DataEntry(encryptedInviteePsaId, encryptedPstr, dataAsByteArray, expireAt = invitation.expireAt)
      (Filters.and(Filters.equal(inviteePsaIdKey, encryptedInviteePsaId), Filters.equal(pstrKey, encryptedPstr)),
        Updates.combine(
          Updates.set("inviteePsaId", dataEntry.inviteePsaId),
          Updates.set("pstr", dataEntry.pstr),
          Updates.set("data", dataEntry.data),
          Updates.set("lastUpdated", Codecs.toBson(dataEntry.lastUpdated)),
          Updates.set("expireAt", Codecs.toBson(dataEntry.expireAt))
        ))
    } else {
      val record = JsonDataEntry.applyJsonDataEntry(invitation.inviteePsaId.value,
        invitation.pstr, Json.toJson(invitation), expireAt = invitation.expireAt)

      (Filters.and(Filters.equal(inviteePsaIdKey, invitation.inviteePsaId.value), Filters.equal(pstrKey, invitation.pstr)),
        Updates.combine(
          Updates.set("inviteePsaId", record.inviteePsaId),
          Updates.set("pstr", record.pstr),
          Updates.set("data", Codecs.toBson(record.data)),
          Updates.set("lastUpdated", Codecs.toBson(record.lastUpdated)),
          Updates.set("expireAt", Codecs.toBson(record.expireAt))
        ))
    }

    collection.findOneAndUpdate(
      filter = selector,
      update = modifier, new FindOneAndUpdateOptions().upsert(true)).toFutureOption().map {
      case Some(_) => true
      case None =>
        logger.error(s"Failed to save or update the row with filters=${selector.toString}")
        false
    }
  }

  private def filterEncryptKeys(mapOfKeys: Map[String, String]): Bson = {
    val filters = mapOfKeys.map {
      case key if key._1 == "inviteePsaId" || key._1 == "pstr" =>
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
