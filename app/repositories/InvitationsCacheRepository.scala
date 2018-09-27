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

import java.nio.charset.StandardCharsets

import com.google.inject.{Inject, Singleton}
import models.Invitation
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._
import play.api.{Configuration, Logger}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.ReadPreference
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.Subtype.GenericBinarySubtype
import reactivemongo.bson.{BSONBinary, BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.crypto.{Crypted, CryptoWithKeysFromConfig, PlainText}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InvitationsCacheRepository @Inject()(
                                            component: ReactiveMongoComponent,
                                            config: Configuration
                                          ) extends ReactiveRepository[JsValue, BSONObjectID](
  config.underlying.getString("mongodb.pension-administrator-cache.invitations.name"),
  component.mongoConnector.db,
  implicitly
) {
  private val encryptionKey: String = "manage.json.encryption"
  // scalastyle:off magic.number
  private val ttl = 0
  private val encrypted: Boolean = config.getBoolean("encrypted").getOrElse(true)
  private val jsonCrypto: CryptoWithKeysFromConfig = CryptoWithKeysFromConfig(baseConfigKey = encryptionKey, config)

  private case class DataEntry(
                                inviteePsaId: String,
                                pstr: String,
                                data: BSONBinary,
                                lastUpdated: DateTime,
                                expireAt: Option[DateTime])

  private object DataEntry {
    def apply(inviteePsaId: String,
              pstr: String,
              data: Array[Byte],
              lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC),
              expireAt: Option[DateTime] = None): DataEntry = {

      DataEntry(inviteePsaId, pstr, BSONBinary(
        data,
        GenericBinarySubtype),
        lastUpdated,
        expireAt
      )

    }

    implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
    implicit val reads: OFormat[DataEntry] = Json.format[DataEntry]
    implicit val writes: OWrites[DataEntry] = Json.format[DataEntry]
  }

  // scalastyle:on magic.number

  private case class JsonDataEntry(inviteePsaId: String,
                                   pstr: String,
                                   data: JsValue,
                                   lastUpdated: DateTime,
                                   expireAt: Option[DateTime]
                                  )

  private object JsonDataEntry {
    def applyJsonDataEntry(inviteePsaId: String,
                           pstr: String,
                           data: JsValue,
                           lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC),
                           expireAt: Option[DateTime] = None): JsonDataEntry = {

      JsonDataEntry(inviteePsaId, pstr, data,
        lastUpdated,
        expireAt)
    }

    implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
    implicit val reads: OFormat[JsonDataEntry] = Json.format[JsonDataEntry]
    implicit val writes: OWrites[JsonDataEntry] = Json.format[JsonDataEntry]
  }

  private val fieldName = "expireAt"
  private val createdIndexName = "dataExpiry"
  private val expireAfterSeconds = "expireAfterSeconds"
  private val inviteePsaIdKey = "inviteePsaId"
  private val pstrKey = "pstr"
  private val compoundIndexName = "inviteePsaId_Pstr"
  private val pstrIndexName = "pstr"

  ensureIndex(Seq(fieldName), createdIndexName, Some(ttl))

  ensureIndex(Seq(inviteePsaIdKey, pstrKey), compoundIndexName, Some(ttl))

  ensureIndex(Seq(pstrKey), pstrIndexName, Some(ttl))

  private def ensureIndex(fields: Seq[String], indexName: String, ttl: Option[Int]): Future[Boolean] = {

    import scala.concurrent.ExecutionContext.Implicits.global

    val fieldIndexes = fields.map((_, IndexType.Ascending))

    val defaultIndex: Index = Index(fieldIndexes, Some(indexName))

    val index: Index = ttl.fold(defaultIndex) { ttl =>
      Index(
        fieldIndexes,
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

  def insert(invitation: Invitation)(implicit ec: ExecutionContext): Future[Boolean] = {

    val (selector, modifier) = if (encrypted) {
      val encryptedInviteePsaId = jsonCrypto.encrypt(PlainText(invitation.inviteePsaId)).value
      val encryptedPstr = jsonCrypto.encrypt(PlainText(invitation.pstr)).value

      val unencrypted = PlainText(Json.stringify(Json.toJson(invitation)))
      val encryptedData = jsonCrypto.encrypt(unencrypted).value
      val dataAsByteArray: Array[Byte] = encryptedData.getBytes("UTF-8")

      (BSONDocument(inviteePsaIdKey -> encryptedInviteePsaId, pstrKey -> encryptedPstr),
        BSONDocument("$set" -> Json.toJson(DataEntry(encryptedInviteePsaId, encryptedPstr, dataAsByteArray, invitation.expireAt))))
    } else {
      val record = JsonDataEntry.applyJsonDataEntry(invitation.inviteePsaId,
                                                    invitation.pstr,
                                                    Json.toJson(invitation), invitation.expireAt)
      (BSONDocument(inviteePsaIdKey -> invitation.inviteePsaId, pstrKey -> invitation.pstr),
        BSONDocument("$set" -> Json.toJson(record)))
    }
    collection.update(selector, modifier, upsert = true)
      .map(_.ok)
  }

  def getByKeys(mapOfKeys: Map[String, String])(implicit ec: ExecutionContext): Future[Option[List[Invitation]]] = {
    if (encrypted) {
      val encryptedMapOfKeys = mapOfKeys.map {
        case key if key._1 == "inviteePsaId" =>
          val encryptedValue = jsonCrypto.encrypt(PlainText(key._2)).value
          (key._1, encryptedValue)
        case key if key._1 == "pstr" =>
          val encryptedValue = jsonCrypto.encrypt(PlainText(key._2)).value
          (key._1, encryptedValue)
        case key => key
      }

      val queryBuilder = collection.find(BSONDocument(encryptedMapOfKeys))
      queryBuilder.cursor[DataEntry](ReadPreference.primary).collect[List]().map { de =>
        val listOfInvitationsJson = de.map {
          dataEntry =>
            val dataAsString = new String(dataEntry.data.byteArray, StandardCharsets.UTF_8)
            val decrypted: PlainText = jsonCrypto.decrypt(Crypted(dataAsString))
            Json.parse(decrypted.value)
        }
        listToOption(listOfInvitationsJson)
      }
    } else {
      val queryBuilder = collection.find(mapOfKeys)
      queryBuilder.cursor[JsonDataEntry](ReadPreference.primary).collect[List]().map { de =>
        val listOfInvitationsJson = de.map {
          dataEntry =>
            dataEntry.data
        }
        listToOption(listOfInvitationsJson)
      }
    }
  }

  private def listToOption(data: List[JsValue]) = {
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
    val selector = mapOfKeys
    collection.remove(selector).map(_.ok)
  }
}
