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

import com.typesafe.config.Config
import org.mockito.Mockito._
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.{Format, Json, OFormat}
import repositories.PSADataCacheEntry.{DataEntryWithoutEncryption, EncryptedDataEntry}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class PSADataCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with EmbeddedMongoDBSupport with BeforeAndAfter with
  BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures { // scalastyle:off magic.number

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(30, Seconds), Span(1, Millis))

  import PSADataCacheRepositorySpec._

  private val idField: String = "id"

  override def beforeAll(): Unit = {
    when(mockAppConfig.underlying).thenReturn(mockConfig)
    when(mockAppConfig.get[String]("mongodb.pension-administrator-cache.psa-data.name")).thenReturn("pension-administrator-psa-data")
    when(mockAppConfig.get[Int]("mongodb.pension-administrator-cache.psa-data.timeToLiveInDays")).thenReturn(3600)
    when(mockConfig.getString("psa.json.encryption.key")).thenReturn("gvBoGdgzqG1AarzF1LY0zQ==")
    initMongoDExecutable()
    startMongoD()
    super.beforeAll()
  }

  override def afterAll(): Unit =
    stopMongoD()

  "upsert" must {
    "save a new session data cache as DataEntryWithoutEncryption in Mongo collection when encrypted false and collection is empty" in {
      when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(false)
      val psaDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("Ext-b9443dbb-3d88-465d-9696-47d6ef94f356", Json.parse("""{"registerAsBusiness":true,"expireAt":1658530800000,"areYouInUK":true}"""))
      val filters = Filters.eq(idField, record._1)

      val documentsInDB = for {
        _ <- psaDataCacheRepository.collection.drop().toFuture()
        _ <- psaDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- psaDataCacheRepository.collection.find[DataEntryWithoutEncryption](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.size mustBe 1
      }
    }

    "update an existing session data cache as DataEntryWithoutEncryption in Mongo collection when encrypted false" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val psaDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record1 = ("Ext-b9443dbb-3d88-465d-9696-47d6ef94f356", Json.parse("""{"registerAsBusiness":true}"""))
      val record2 = ("Ext-b9443dbb-3d88-465d-9696-47d6ef94f356", Json.parse("""{"registerAsBusiness":true,"expireAt":1658530800000,"areYouInUK":true}"""))

      val filters = Filters.eq(idField, "Ext-b9443dbb-3d88-465d-9696-47d6ef94f356")

      val documentsInDB = for {
        _ <- psaDataCacheRepository.collection.drop().toFuture()
        _ <- psaDataCacheRepository.upsert(record1._1, record1._2)
        _ <- psaDataCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- psaDataCacheRepository.collection.find[DataEntryWithoutEncryption](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.size mustBe 1
        documentsInDB.head.data mustBe record2._2
        documentsInDB.head.data must not be record1._2
      }
    }

    "save a new session data cache as DataEntryWithoutEncryption in Mongo collection when encrypted false and id is not same" in {
      when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(false)
      val psaDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-2", Json.parse("""{"data":"2"}"""))

      val documentsInDB = for {
        _ <- psaDataCacheRepository.collection.drop().toFuture()
        _ <- psaDataCacheRepository.upsert(record1._1, record1._2)
        _ <- psaDataCacheRepository.upsert(record2._1, record1._2)
        documentsInDB <- psaDataCacheRepository.collection.find[DataEntryWithoutEncryption]().toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size mustBe 2
      }
    }

    "save a new session data cache as EncryptedDataEntry in Mongo collection when encrypted true and collection is empty" in {
      when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(true)
      val psaDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val filters = Filters.eq(idField, "id-1")

      val documentsInDB = for {
        _ <- psaDataCacheRepository.collection.drop().toFuture()
        _ <- psaDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- psaDataCacheRepository.collection.find[EncryptedDataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size mustBe 1
      }
    }

    "update an existing session data cache as EncryptedDataEntry in Mongo collection when encrypted true" in {
      when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(true)
      val psaDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-1", Json.parse("""{"data":"2"}"""))
      val filters = Filters.eq(idField, "id-1")

      val documentsInDB = for {
        _ <- psaDataCacheRepository.collection.drop().toFuture()
        _ <- psaDataCacheRepository.upsert(record1._1, record1._2)
        _ <- psaDataCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- psaDataCacheRepository.collection.find[EncryptedDataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size mustBe 1
      }
    }

    "save a new session data cache as EncryptedDataEntry in Mongo collection when encrypted true and id is not same" in {
      when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(true)
      val psaDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-2", Json.parse("""{"data":"2"}"""))

      val documentsInDB = for {
        _ <- psaDataCacheRepository.collection.drop().toFuture()
        _ <- psaDataCacheRepository.upsert(record1._1, record1._2)
        _ <- psaDataCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- psaDataCacheRepository.collection.find[EncryptedDataEntry]().toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size mustBe 2
      }
    }
  }

  "get" must {
    "get a session data cache record as DataEntryWithoutEncryption by id in Mongo collection when encrypted false" in {

      when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(false)
      val psaDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("Ext-b9443dbb-3d88-465d-9696-47d6ef94f356", Json.toJson(TestCacheData(true, Instant.now(), true, Instant.now())))
      val documentsInDB = for {
        _ <- psaDataCacheRepository.collection.drop().toFuture()
        _ <- psaDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- psaDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        println(s"\n\n\n\n\n documents in db: ${documentsInDB}")
        documentsInDB.isDefined mustBe true
      }
    }

    "get a session data cache record as EncryptedDataEntry by id in Mongo collection when encrypted true" in {
      when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(true)
      val psaDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val documentsInDB = for {
        _ <- psaDataCacheRepository.collection.drop().toFuture()
        _ <- psaDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- psaDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }
    }
  }

  "remove" must {
    "delete an existing DataEntryWithoutEncryption session data cache record by id in Mongo collection when encrypted false" in {
      when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(false)
      val psaDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- psaDataCacheRepository.collection.drop().toFuture()
        _ <- psaDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- psaDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }

      val documentsInDB2 = for {
        _ <- psaDataCacheRepository.remove(record._1)
        documentsInDB2 <- psaDataCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2) { documentsInDB2 =>
        documentsInDB2.isDefined mustBe false
      }
    }

    "not delete an existing DataEntryWithoutEncryption session data cache record by id in Mongo collection when encrypted false and id incorrect" in {
      when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(false)
      val psaDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- psaDataCacheRepository.collection.drop().toFuture()
        _ <- psaDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- psaDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }

      val documentsInDB2 = for {
        _ <- psaDataCacheRepository.remove("2")
        documentsInDB2 <- psaDataCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2) { documentsInDB2 =>
        documentsInDB2.isDefined mustBe true
      }
    }

    "delete an existing EncryptedDataEntry session data cache record by id in Mongo collection when encrypted true" in {
      when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(true)
      val psaDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- psaDataCacheRepository.collection.drop().toFuture()
        _ <- psaDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- psaDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }

      val documentsInDB2 = for {
        _ <- psaDataCacheRepository.remove(record._1)
        documentsInDB2 <- psaDataCacheRepository.get(record._1)
      } yield documentsInDB2


      whenReady(documentsInDB2) { documentsInDB2 =>
        documentsInDB2.isDefined mustBe false
      }
    }

    "not delete an existing EncryptedDataEntry session data cache record by id in Mongo collection when encrypted true" in {
      when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(true)
      val psaDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- psaDataCacheRepository.collection.drop().toFuture()
        _ <- psaDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- psaDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }

      val documentsInDB2 = for {
        _ <- psaDataCacheRepository.remove("2")
        documentsInDB2 <- psaDataCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2) { documentsInDB2 =>
        documentsInDB2.isDefined mustBe true
      }
    }
  }
}

object PSADataCacheRepositorySpec extends AnyWordSpec with MockitoSugar {

  private val mockAppConfig = mock[Configuration]
  private val mockConfig = mock[Config]

  private def buildFormRepository(mongoHost: String, mongoPort: Int) = {
    val databaseName = "pension-administrator"
    val mongoUri = s"mongodb://$mongoHost:$mongoPort/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
    new PSADataCacheRepository(MongoComponent(mongoUri), mockAppConfig)
  }
}

case class TestCacheData(registerAsBusiness: Boolean, expireAt: Instant, areYouInUK: Boolean, lastUpdated: Instant)

object TestCacheData {
  implicit val dateTimeFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val format: OFormat[TestCacheData] = Json.format[TestCacheData]
}
