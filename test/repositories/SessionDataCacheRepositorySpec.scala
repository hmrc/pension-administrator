/*
 * Copyright 2023 HM Revenue & Customs
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
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito._
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.Json
import repositories.ManageCacheEntry.{DataEntry, JsonDataEntry}
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.ExecutionContext.Implicits.global

class SessionDataCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with EmbeddedMongoDBSupport with BeforeAndAfter with
  BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures { // scalastyle:off magic.number

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(30, Seconds), Span(1, Millis))

  import SessionDataCacheRepositorySpec._

  private val idField: String = "id"

  override def beforeAll(): Unit = {
    when(mockAppConfig.underlying).thenReturn(mockConfig)
    when(mockConfig.getString("mongodb.pension-administrator-cache.session-data.name")).thenReturn("session-data")
    when(mockConfig.getInt("mongodb.pension-administrator-cache.session-data.timeToLiveInSeconds")).thenReturn(3600)
    when(mockConfig.getString("manage.json.encryption.key")).thenReturn("gvBoGdgzqG1AarzF1LY0zQ==")
    initMongoDExecutable()
    startMongoD()
    super.beforeAll()
  }

  override def afterAll(): Unit =
    stopMongoD()

  "upsert" must {
    "save a new session data cache as JsonDataEntry in Mongo collection when encrypted false and collection is empty" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val sessionDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val filters = Filters.eq(idField, record._1)

      val documentsInDB = for {
        _ <- sessionDataCacheRepository.collection.drop().toFuture()
        _ <- sessionDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- sessionDataCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size mustBe 1
      }
    }

    "update an existing session data cache as JsonDataEntry in Mongo collection when encrypted false" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val sessionDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-1", Json.parse("""{"data":"2"}"""))
      val filters = Filters.eq(idField, "id-1")

      val documentsInDB = for {
        _ <- sessionDataCacheRepository.collection.drop().toFuture()
        _ <- sessionDataCacheRepository.upsert(record1._1, record1._2)
        _ <- sessionDataCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- sessionDataCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size mustBe 1
          documentsInDB.head.data mustBe record2._2
          documentsInDB.head.data must not be record1._2
      }
    }

    "save a new session data cache as JsonDataEntry in Mongo collection when encrypted false and id is not same" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val sessionDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-2", Json.parse("""{"data":"2"}"""))

      val documentsInDB = for {
        _ <- sessionDataCacheRepository.collection.drop().toFuture()
        _ <- sessionDataCacheRepository.upsert(record1._1, record1._2)
        _ <- sessionDataCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- sessionDataCacheRepository.collection.find[JsonDataEntry]().toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size mustBe 2
      }
    }

    "save a new session data cache as DataEntry in Mongo collection when encrypted true and collection is empty" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val sessionDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val filters = Filters.eq(idField, "id-1")

      val documentsInDB = for {
        _ <- sessionDataCacheRepository.collection.drop().toFuture()
        _ <- sessionDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- sessionDataCacheRepository.collection.find[DataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size mustBe 1
      }
    }

    "update an existing session data cache as DataEntry in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val sessionDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-1", Json.parse("""{"data":"2"}"""))
      val filters = Filters.eq(idField, "id-1")

      val documentsInDB = for {
        _ <- sessionDataCacheRepository.collection.drop().toFuture()
        _ <- sessionDataCacheRepository.upsert(record1._1, record1._2)
        _ <- sessionDataCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- sessionDataCacheRepository.collection.find[DataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size mustBe 1
      }
    }

    "save a new session data cache as DataEntry in Mongo collection when encrypted true and id is not same" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val sessionDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-2", Json.parse("""{"data":"2"}"""))

      val documentsInDB = for {
        _ <- sessionDataCacheRepository.collection.drop().toFuture()
        _ <- sessionDataCacheRepository.upsert(record1._1, record1._2)
        _ <- sessionDataCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- sessionDataCacheRepository.collection.find[DataEntry]().toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size mustBe 2
      }
    }
  }

  "get" must {
    "get a session data cache record as JsonDataEntry by id in Mongo collection when encrypted false" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val sessionDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- sessionDataCacheRepository.collection.drop().toFuture()
        _ <- sessionDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- sessionDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }
    }

    "get a session data cache record as DataEntry by id in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val sessionDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- sessionDataCacheRepository.collection.drop().toFuture()
        _ <- sessionDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- sessionDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }
    }
  }

  "getLastUpdated" must {
    "get a session cache data's lastUpdated field by id in Mongo collection when encrypted false" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val sessionDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- sessionDataCacheRepository.collection.drop().toFuture()
        _ <- sessionDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- sessionDataCacheRepository.getLastUpdated(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.get.compareTo(DateTime.now(DateTimeZone.UTC)) mustBe -1
      }
    }

    "get a session cache data's lastUpdated field by id in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val sessionDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- sessionDataCacheRepository.collection.drop().toFuture()
        _ <- sessionDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- sessionDataCacheRepository.getLastUpdated(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.get.compareTo(DateTime.now(DateTimeZone.UTC)) mustBe -1
      }
    }
  }

  "remove" must {
    "delete an existing JsonDataEntry session data cache record by id in Mongo collection when encrypted false" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val sessionDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- sessionDataCacheRepository.collection.drop().toFuture()
        _ <- sessionDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- sessionDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }

      val documentsInDB2 = for {
        _ <- sessionDataCacheRepository.remove(record._1)
        documentsInDB2 <- sessionDataCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2) { documentsInDB2 =>
        documentsInDB2.isDefined mustBe false
      }
    }

    "not delete an existing JsonDataEntry session data cache record by id in Mongo collection when encrypted false and id incorrect" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val sessionDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- sessionDataCacheRepository.collection.drop().toFuture()
        _ <- sessionDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- sessionDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }

      val documentsInDB2 = for {
        _ <- sessionDataCacheRepository.remove("2")
        documentsInDB2 <- sessionDataCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2) { documentsInDB2 =>
        documentsInDB2.isDefined mustBe true
      }
    }

    "delete an existing DataEntry session data cache record by id in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val sessionDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- sessionDataCacheRepository.collection.drop().toFuture()
        _ <- sessionDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- sessionDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }

      val documentsInDB2 = for {
        _ <- sessionDataCacheRepository.remove(record._1)
        documentsInDB2 <- sessionDataCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2) { documentsInDB2 =>
        documentsInDB2.isDefined mustBe false
      }
    }

    "not delete an existing DataEntry session data cache record by id in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val sessionDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- sessionDataCacheRepository.collection.drop().toFuture()
        _ <- sessionDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- sessionDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }

      val documentsInDB2 = for {
        _ <- sessionDataCacheRepository.remove("2")
        documentsInDB2 <- sessionDataCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2) { documentsInDB2 =>
        documentsInDB2.isDefined mustBe true
      }
    }
  }
}

object SessionDataCacheRepositorySpec extends AnyWordSpec with MockitoSugar {

  private val mockAppConfig = mock[Configuration]
  private val mockConfig = mock[Config]

  private def buildFormRepository(mongoHost: String, mongoPort: Int) = {
    val databaseName = "pension-administrator"
    val mongoUri = s"mongodb://$mongoHost:$mongoPort/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
    new SessionDataCacheRepository(mockAppConfig, MongoComponent(mongoUri))
  }
}