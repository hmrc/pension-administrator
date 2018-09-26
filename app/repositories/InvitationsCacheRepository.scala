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

import com.google.inject.Inject
import models.Invitation
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
import utils.DateUtils

import scala.concurrent.{ExecutionContext, Future}

class InvitationsCacheRepositoryImpl @Inject()(
                                               config: Configuration,
                                               component: ReactiveMongoComponent
                                             ) extends InvitationsCacheRepository(
  config.underlying.getString("mongodb.pension-administrator-cache.manage-pensions.name"),
  component,
  config
)

abstract class InvitationsCacheRepository @Inject()(
                                            collectionName: String,
                                            component: ReactiveMongoComponent,
                                            config: Configuration
                                          ) extends ReactiveRepository[JsValue, BSONObjectID](
  collectionName,
  component.mongoConnector.db,
  implicitly
) {
  private val ttl = 30
  private val encryptionKey = "manage.json.encryption"
  private val encrypted: Boolean = config.getBoolean("encrypted").getOrElse(true)
  private val jsonCrypto: CryptoWithKeysFromConfig = CryptoWithKeysFromConfig(baseConfigKey = encryptionKey, config)


  private case class DataEntry(inviteePsaId: String,
                                pstr: String,
                                data: BSONBinary,
                                lastUpdated: DateTime,
                                expireAt: Option[DateTime])

  // scalastyle:off magic.number
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
        Some(DateUtils.dateTimeFromDateToMidnightOnDay(DateTime.now(DateTimeZone.UTC), ttl)))

    }

    implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
    implicit val reads: OFormat[DataEntry] = Json.format[DataEntry]
    implicit val writes: OWrites[DataEntry] = Json.format[DataEntry]
  }

  private case class JsonDataEntry(inviteePsaId: String,
                                   pstr: String,
                                   data: JsValue,
                                   lastUpdated: DateTime
                                  )

  private object JsonDataEntry {
    implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
    implicit val reads: OFormat[JsonDataEntry] = Json.format[JsonDataEntry]
    implicit val writes: OWrites[JsonDataEntry] = Json.format[JsonDataEntry]
  }

  private val fieldName = "expireAt"
  private val createdIndexName = "dataExpiry"
  private val expireAfterSeconds = "expireAfterSeconds"

  ensureIndex(fieldName, createdIndexName, Some(ttl))

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

  def insert(invitation: Invitation)(implicit ec: ExecutionContext): Future[Boolean] = {

      if (encrypted) {
        val encryptedInviteePsaId = jsonCrypto.encrypt(PlainText(invitation.inviteePsaId)).value
        val encryptedPstr = jsonCrypto.encrypt(PlainText(invitation.pstr)).value

        val unencrypted = PlainText(Json.stringify(Json.toJson(invitation)))
        val encryptedData = jsonCrypto.encrypt(unencrypted).value
        val dataAsByteArray: Array[Byte] = encryptedData.getBytes("UTF-8")

        collection.insert(DataEntry(encryptedInviteePsaId, encryptedPstr, dataAsByteArray)).map(_.ok)

      } else {

        val record = JsonDataEntry(invitation.inviteePsaId, invitation.pstr, Json.toJson(invitation), DateTime.now(DateTimeZone.UTC))
        collection.insert(record).map(_.ok)
      }
  }

  def get(inviteePsaId: String, pstr: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    if (encrypted) {
      collection.find(BSONDocument("inviteePsaId" -> inviteePsaId, "pstr" -> pstr)).one[DataEntry].map {
        _.map {
          dataEntry =>
            val dataAsString = new String(dataEntry.data.byteArray, StandardCharsets.UTF_8)
            val decrypted: PlainText = jsonCrypto.decrypt(Crypted(dataAsString))
            Json.parse(decrypted.value)
        }
      }
    } else {
      collection.find(BSONDocument("inviteePsaId" -> inviteePsaId, "pstr" -> pstr)).one[JsonDataEntry].map {
        _.map {
          dataEntry =>
            dataEntry.data
        }
      }
    }
  }

  def getForScheme(pstr: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    if (encrypted) {
      collection.find(BSONDocument("pstr" -> pstr)).one[DataEntry].map {
        _.map {
          dataEntry =>
            val dataAsString = new String(dataEntry.data.byteArray, StandardCharsets.UTF_8)
            val decrypted: PlainText = jsonCrypto.decrypt(Crypted(dataAsString))
            Json.parse(decrypted.value)
        }
      }
    } else {
      collection.find(BSONDocument("pstr" -> pstr)).one[JsonDataEntry].map {
        _.map {
          dataEntry =>
            dataEntry.data
        }
      }
    }
  }

  def getForInvitee(inviteePsaId: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    if (encrypted) {
      collection.find(BSONDocument("inviteePsaId" -> inviteePsaId)).one[DataEntry].map {
        _.map {
          dataEntry =>
            val dataAsString = new String(dataEntry.data.byteArray, StandardCharsets.UTF_8)
            val decrypted: PlainText = jsonCrypto.decrypt(Crypted(dataAsString))
            Json.parse(decrypted.value)
        }
      }
    } else {
      collection.find(BSONDocument("inviteePsaId" -> inviteePsaId)).one[JsonDataEntry].map {
        _.map {
          dataEntry =>
            dataEntry.data
        }
      }
    }
  }

  def getLastUpdated(inviteePsaId: String)(implicit ec: ExecutionContext): Future[Option[DateTime]] = {
    if (encrypted) {
      collection.find(BSONDocument("inviteePsaId" -> inviteePsaId)).one[DataEntry].map {
        _.map {
          dataEntry =>
            dataEntry.lastUpdated
        }
      }
    } else {
      collection.find(BSONDocument("inviteePsaId" -> inviteePsaId)).one[JsonDataEntry].map {
        _.map {
          dataEntry =>
            dataEntry.lastUpdated
        }
      }
    }
  }


  def remove(inviteePsaId: String, pstr: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val selector = BSONDocument("inviteePsaId" -> inviteePsaId, "pstr" -> pstr)
    collection.remove(selector).map(_.ok)
  }
}
