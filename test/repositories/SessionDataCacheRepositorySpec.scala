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

import com.github.simplyscala.MongoEmbedDatabase
import com.typesafe.config.Config
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.MockitoSugar
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.Configuration
import play.api.libs.json.Json
import repositories.ManageCacheEntry.{DataEntry, JsonDataEntry}
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class SessionDataCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with MongoEmbedDatabase with BeforeAndAfter with
  BeforeAndAfterEach { // scalastyle:off magic.number

  private val idField: String = "id"

  import SessionDataCacheRepositorySpec._

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockAppConfig.underlying).thenReturn(mockConfig)
    when(mockConfig.getString("mongodb.pension-administrator-cache.session-data.name")).thenReturn("session-data")
    when(mockConfig.getInt("mongodb.pension-administrator-cache.session-data.timeToLiveInSeconds")).thenReturn(3600)
    when(mockConfig.getString("manage.json.encryption.key")).thenReturn("gvBoGdgzqG1AarzF1LY0zQ==")
  }

  withEmbedMongoFixture(port = 24680) { _ =>
    "upsert" must {
      "save a new session data cache as JsonDataEntry in Mongo collection when encrypted false and collection is empty" in {
        when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        Await.result(sessionDataCacheRepository.upsert(record._1, record._2), Duration.Inf)

        val filters = Filters.eq(idField, record._1)
        whenReady(
          sessionDataCacheRepository.collection.find[JsonDataEntry](filters).toFuture()) {
          documentsInDB =>
            documentsInDB.size mustBe 1
        }
      }

      "update an existing session data cache as JsonDataEntry in Mongo collection when encrypted false" in {
        when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
        mongoCollectionDrop()

        val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
        val record2 = ("id-1", Json.parse("""{"data":"2"}"""))

        Await.result(sessionDataCacheRepository.upsert(record1._1, record1._2).map { _ =>
          sessionDataCacheRepository.upsert(record2._1, record2._2)
        }, Duration.Inf)

        val filters = Filters.eq(idField, "id-1")
        whenReady(
          sessionDataCacheRepository.collection.find[JsonDataEntry](filters).toFuture()) {
          documentsInDB =>
            documentsInDB.size mustBe 1
            documentsInDB.head.data mustBe record2._2
            documentsInDB.head.data must not be record1._2
        }
      }

      "insert a new session data cache as JsonDataEntry in Mongo collection when encrypted false and id is not same" in {
        when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
        mongoCollectionDrop()

        val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
        val record2 = ("id-2", Json.parse("""{"data":"2"}"""))

        Await.result(sessionDataCacheRepository.upsert(record1._1, record1._2).map { _ =>
          sessionDataCacheRepository.upsert(record2._1, record2._2)
        }, Duration.Inf)

        whenReady(
          sessionDataCacheRepository.collection.find[JsonDataEntry].toFuture()) {
          documentsInDB =>
            documentsInDB.size mustBe 2
        }
      }

      "insert a new session data cache as DataEntry in Mongo collection when encrypted true and collection is empty" in {
        when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        Await.result(sessionDataCacheRepository.upsert(record._1, record._2), Duration.Inf)

        val filters = Filters.eq(idField, "id-1")
        whenReady(
          sessionDataCacheRepository.collection.find[DataEntry](filters).toFuture()) {
          documentsInDB =>
            documentsInDB.size mustBe 1
        }
      }

      "update an existing session data cache as DataEntry in Mongo collection when encrypted true" in {
        when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
        mongoCollectionDrop()

        val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
        val record2 = ("id-1", Json.parse("""{"data":"2"}"""))

        Await.result(sessionDataCacheRepository.upsert(record1._1, record1._2).map { _ =>
          sessionDataCacheRepository.upsert(record2._1, record2._2)
        }, Duration.Inf)

        val filters = Filters.eq(idField, "id-1")
        whenReady(
          sessionDataCacheRepository.collection.find[DataEntry](filters).toFuture()) {
          documentsInDB =>
            documentsInDB.size mustBe 1
        }
      }

      "insert a new session data cache as DataEntry in Mongo collection when encrypted true and id is not same" in {
        when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
        mongoCollectionDrop()

        val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
        val record2 = ("id-2", Json.parse("""{"data":"2"}"""))

        Await.result(sessionDataCacheRepository.upsert(record1._1, record1._2).map { _ =>
          sessionDataCacheRepository.upsert(record2._1, record2._2)
        }, Duration.Inf)

        whenReady(
          sessionDataCacheRepository.collection.find[DataEntry].toFuture()) {
          documentsInDB =>
            documentsInDB.size mustBe 2
        }
      }
    }

    "get" must {
      "get a session data cache record as JsonDataEntry by id in Mongo collection when encrypted false" in {

        when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        Await.result(sessionDataCacheRepository.upsert(record._1, record._2), Duration.Inf)

        whenReady(sessionDataCacheRepository.get(record._1)) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }
      }

      "get a session data cache record as DataEntry by id in Mongo collection when encrypted true" in {

        when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        Await.result(sessionDataCacheRepository.upsert(record._1, record._2), Duration.Inf)

        whenReady(sessionDataCacheRepository.get(record._1)) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }
      }
    }

    "getLastUpdated" must {
      "get a session cache data's lastUpdated field by id in Mongo collection when encrypted false" in {

        when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        Await.result(sessionDataCacheRepository.upsert(record._1, record._2), Duration.Inf)

        whenReady(sessionDataCacheRepository.getLastUpdated(record._1)) { documentsInDB =>
          documentsInDB.get.compareTo(DateTime.now(DateTimeZone.UTC)) mustBe -1
        }
      }

      "get a session cache data's lastUpdated field by id in Mongo collection when encrypted true" in {

        when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        Await.result(sessionDataCacheRepository.upsert(record._1, record._2), Duration.Inf)

        whenReady(sessionDataCacheRepository.getLastUpdated(record._1)) { documentsInDB =>
          documentsInDB.get.compareTo(DateTime.now(DateTimeZone.UTC)) mustBe -1

        }
      }
    }

    "remove" must {
      "delete an existing JsonDataEntry session data cache record by id in Mongo collection when encrypted false" in {

        when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        Await.result(sessionDataCacheRepository.upsert(record._1, record._2), Duration.Inf)

        whenReady(sessionDataCacheRepository.get(record._1)) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }

        Await.result(sessionDataCacheRepository.remove(record._1), Duration.Inf)

        whenReady(sessionDataCacheRepository.get(record._1)) { documentsInDB =>
          documentsInDB.isDefined mustBe false
        }
      }

      "not delete an existing JsonDataEntry session data cache record by id in Mongo collection when encrypted false and id incorrect" in {

        when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        Await.result(sessionDataCacheRepository.upsert(record._1, record._2), Duration.Inf)

        whenReady(sessionDataCacheRepository.get(record._1)) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }

        Await.result(sessionDataCacheRepository.remove("2"), Duration.Inf)

        whenReady(sessionDataCacheRepository.get(record._1)) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }
      }

      "delete an existing DataEntry session data cache record by id in Mongo collection when encrypted true" in {

        when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        Await.result(sessionDataCacheRepository.upsert(record._1, record._2), Duration.Inf)

        whenReady(sessionDataCacheRepository.get(record._1)) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }

        Await.result(sessionDataCacheRepository.remove(record._1), Duration.Inf)

        whenReady(sessionDataCacheRepository.get(record._1)) { documentsInDB =>
          documentsInDB.isDefined mustBe false
        }
      }

      "not delete an existing DataEntry session data cache record by id in Mongo collection when encrypted true" in {

        when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        Await.result(sessionDataCacheRepository.upsert(record._1, record._2), Duration.Inf)

        whenReady(sessionDataCacheRepository.get(record._1)) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }

        Await.result(sessionDataCacheRepository.remove("2"), Duration.Inf)

        whenReady(sessionDataCacheRepository.get(record._1)) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }
      }
    }
  }
}

object SessionDataCacheRepositorySpec extends AnyWordSpec with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits._

  private val mockAppConfig = mock[Configuration]
  private val mockConfig = mock[Config]
  private val databaseName = "pension-administrator"
  private val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
  private val mongoComponent = MongoComponent(mongoUri)

  private def mongoCollectionDrop(): Void = Await
    .result(sessionDataCacheRepository.collection.drop().toFuture(), Duration.Inf)

  def sessionDataCacheRepository: ManageCacheRepository = new SessionDataCacheRepository(mockAppConfig, mongoComponent)
}