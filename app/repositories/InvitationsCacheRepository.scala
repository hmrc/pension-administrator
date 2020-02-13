/*
 * Copyright 2020 HM Revenue & Customs
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
import reactivemongo.api.{Cursor, ReadPreference}
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
                                          )(implicit val ec: ExecutionContext) extends ReactiveRepository[JsValue, BSONObjectID](
  config.underlying.getString("mongodb.pension-administrator-cache.invitations.name"),
  component.mongoConnector.db,
  implicitly
) {
  private val encryptionKey: String = "manage.json.encryption"
  // scalastyle:off magic.number
  private val ttl = 0
  private val encrypted: Boolean = config.getOptional[Boolean]("encrypted").getOrElse(true)
  private val jsonCrypto: CryptoWithKeysFromConfig = new CryptoWithKeysFromConfig(baseConfigKey = encryptionKey, config.underlying)

  private case class DataEntry(
                                inviteePsaId: String,
                                pstr: String,
                                data: BSONBinary,
                                lastUpdated: DateTime,
                                expireAt: DateTime)

  private object DataEntry {
    def apply(inviteePsaId: String,
              pstr: String,
              data: Array[Byte],
              lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC),
              expireAt: DateTime): DataEntry = {

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
                                   expireAt: DateTime
                                  )

  private object JsonDataEntry {
    def applyJsonDataEntry(inviteePsaId: String,
                           pstr: String,
                           data: JsValue,
                           lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC),
                           expireAt: DateTime): JsonDataEntry = {

      JsonDataEntry(inviteePsaId, pstr, data,
        lastUpdated,
        expireAt)
    }

    implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
    implicit val reads: OFormat[JsonDataEntry] = Json.format[JsonDataEntry]
    implicit val writes: OWrites[JsonDataEntry] = Json.format[JsonDataEntry]
  }

  private val expireAt = "expireAt"
  private val dataExpiry = "dataExpiry"
  private val expireAfterSeconds = "expireAfterSeconds"
  private val inviteePsaIdKey = "inviteePsaId"
  private val pstrKey = "pstr"
  private val uniqueInvitation = "inviteePsaId_Pstr"
  private val pstrIndexName = "pstr"

  (for {
    _ <- CollectionDiagnostics.checkIndexTtl(collection, dataExpiry, Some(ttl))
    _ <- ensureIndex(fields = Seq(expireAt), indexName = dataExpiry, ttl = Some(ttl))
    _ <- ensureIndex(fields = Seq(inviteePsaIdKey, pstrKey), indexName = uniqueInvitation, isUnique = true)
    _ <- ensureIndex(fields = Seq(pstrKey), indexName = pstrIndexName)
  } yield {
    ()
  }) recoverWith {
    case t: Throwable => Future.successful(Logger.error(s"Error ensuring indexes on collection ${collection.name}", t))
  } andThen {
    case _ => CollectionDiagnostics.logCollectionInfo(collection)
  }

  private def ensureIndex(fields: Seq[String], indexName: String, ttl: Option[Int] = None, isUnique:Boolean = false): Future[Boolean] = {

    val fieldIndexes = fields.map((_, IndexType.Ascending))

    val index = {
      val defaultIndex = Index(fieldIndexes, Some(indexName), background = true, unique = isUnique)
      ttl.map(ttl => defaultIndex.copy(options = BSONDocument(expireAfterSeconds -> ttl))).fold(defaultIndex)(identity)
    }

    val indexCreationDescription = {
      def addExpireAfterSecondsDescription(s:String) = ttl.fold(s)(ttl => s"$s (expireAfterSeconds = $ttl)")
      addExpireAfterSecondsDescription(s"Attempt to create Mongo index $index (unique = ${index.unique}))")
    }

    collection.indexesManager.ensure(index) map{ result =>
      Logger.debug( indexCreationDescription + s" was successful and result is: $result")
      result
    } recover {
      case e => Logger.error(indexCreationDescription + s" was unsuccessful", e)
        false
    }
  }

  def insert(invitation: Invitation)(implicit ec: ExecutionContext): Future[Boolean] = {

    val (selector, modifier) = if (encrypted) {
      val encryptedInviteePsaId = jsonCrypto.encrypt(PlainText(invitation.inviteePsaId.value)).value
      val encryptedPstr = jsonCrypto.encrypt(PlainText(invitation.pstr)).value

      val unencrypted = PlainText(Json.stringify(Json.toJson(invitation)))
      val encryptedData = jsonCrypto.encrypt(unencrypted).value
      val dataAsByteArray: Array[Byte] = encryptedData.getBytes("UTF-8")

      (BSONDocument(inviteePsaIdKey -> encryptedInviteePsaId, pstrKey -> encryptedPstr),
        BSONDocument("$set" -> Json.toJson(DataEntry(encryptedInviteePsaId, encryptedPstr, dataAsByteArray, expireAt = invitation.expireAt))))
    } else {
      val record = JsonDataEntry.applyJsonDataEntry(invitation.inviteePsaId.value,
                                                    invitation.pstr,
                                                    Json.toJson(invitation), expireAt = invitation.expireAt)
      (BSONDocument(inviteePsaIdKey -> invitation.inviteePsaId.value, pstrKey -> invitation.pstr),
        BSONDocument("$set" -> Json.toJson(record)))
    }
    collection.update(ordered = false).one(selector, modifier, upsert = true)
      .map(_.ok)
  }

  private def encryptKeys(mapOfKeys: Map[String, String]): Map[String, String] = {
    mapOfKeys.map {
      case key if key._1 == "inviteePsaId" =>
        val encryptedValue = jsonCrypto.encrypt(PlainText(key._2)).value
        (key._1, encryptedValue)
      case key if key._1 == "pstr" =>
        val encryptedValue = jsonCrypto.encrypt(PlainText(key._2)).value
        (key._1, encryptedValue)
      case key => key
    }
  }

  def getByKeys(mapOfKeys: Map[String, String])(implicit ec: ExecutionContext): Future[Option[List[Invitation]]] = {
    if (encrypted) {
      val encryptedMapOfKeys = encryptKeys(mapOfKeys)
      val queryBuilder = collection.find(encryptedMapOfKeys, None)(implicitly, BSONDocumentWrites)
      queryBuilder.cursor[DataEntry](ReadPreference.primary).collect[List](-1, Cursor.FailOnError[List[DataEntry]]()).map { de =>
        val listOfInvitationsJson = de.map {
          dataEntry =>
            val dataAsString = new String(dataEntry.data.byteArray, StandardCharsets.UTF_8)
            val decrypted: PlainText = jsonCrypto.decrypt(Crypted(dataAsString))
            Json.parse(decrypted.value)
        }
        listToOption(listOfInvitationsJson)
      }
    } else {
      val queryBuilder = collection.find(mapOfKeys, None)(implicitly, BSONDocumentWrites)
      queryBuilder.cursor[JsonDataEntry](ReadPreference.primary).collect[List](-1, Cursor.FailOnError[List[JsonDataEntry]]()).map { de =>
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
    val selector = if (encrypted) {
      encryptKeys(mapOfKeys)
    } else {
      mapOfKeys
    }
    collection.delete().one(selector).map(_.ok)
  }
}
