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
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Milliseconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.Configuration
import play.api.libs.json.Json
import repositories.ManageCacheEntry.{DataEntry, JsonDataEntry}
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class MinimalDetailsCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with MongoEmbedDatabase with BeforeAndAfter with
  BeforeAndAfterEach with BeforeAndAfterAll { // scalastyle:off magic.number

  private val idField: String = "id"

  import MinimalDetailsCacheRepositorySpec._

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockAppConfig.underlying).thenReturn(mockConfig)
    when(mockAppConfig.get[String](path = "mongodb.pension-administrator-cache.minimal-detail.name")).thenReturn("minimal-detail")
    when(mockAppConfig.get[Int]("mongodb.pension-administrator-cache.minimal-detail.timeToLiveInSeconds")).thenReturn(3600)
    when(mockConfig.getString("manage.json.encryption.key")).thenReturn("gvBoGdgzqG1AarzF1LY0zQ==")
  }

  override def beforeAll(): Unit = {
    mongoStart(port = databasePort)
    super.beforeAll()
  }

  "upsert" must {
    "save a new minimal details cache as JsonDataEntry in Mongo collection when encrypted false and collection is empty" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      mongoCollectionDrop()

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val filters = Filters.eq(idField, record._1)

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) {
        documentsInDB =>
          documentsInDB.size mustBe 1
      }
    }

    "update an existing minimal details cache as JsonDataEntry in Mongo collection when encrypted false" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      mongoCollectionDrop()

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-1", Json.parse("""{"data":"2"}"""))
      val filters = Filters.eq(idField, "id-1")

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.upsert(record1._1, record1._2)
        _ <- minimalDetailsCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- minimalDetailsCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) {
        documentsInDB =>
          documentsInDB.size mustBe 1
          documentsInDB.head.data mustBe record2._2
          documentsInDB.head.data must not be record1._2
      }
    }

    "save a new minimal details cache as JsonDataEntry in Mongo collection when encrypted false and id is not same" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      mongoCollectionDrop()

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-2", Json.parse("""{"data":"2"}"""))

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.upsert(record1._1, record1._2)
        _ <- minimalDetailsCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- minimalDetailsCacheRepository.collection.find[JsonDataEntry].toFuture()
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) {
        documentsInDB =>
          documentsInDB.size mustBe 2
      }
    }

    "save a new minimal details cache as DataEntry in Mongo collection when encrypted true and collection is empty" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      mongoCollectionDrop()

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val filters = Filters.eq(idField, record._1)

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.collection.find[DataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) {
        documentsInDB =>
          documentsInDB.size mustBe 1
      }
    }

    "update an existing minimal details cache as DataEntry in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      mongoCollectionDrop()

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-1", Json.parse("""{"data":"2"}"""))
      val filters = Filters.eq(idField, "id-1")

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.upsert(record1._1, record1._2)
        _ <- minimalDetailsCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- minimalDetailsCacheRepository.collection.find[DataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) {
        documentsInDB =>
          documentsInDB.size mustBe 1
      }
    }

    "insert a new minimal details cache as DataEntry in Mongo collection when encrypted true and id is not same" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      mongoCollectionDrop()

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-2", Json.parse("""{"data":"2"}"""))

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.upsert(record1._1, record1._2)
        _ <- minimalDetailsCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- minimalDetailsCacheRepository.collection.find[DataEntry].toFuture()
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) {
        documentsInDB =>
          documentsInDB.size mustBe 2
      }
    }
  }

  "get" must {
    "get a minimal details cache record as JsonDataEntry by id in Mongo collection when encrypted false" in {

      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      mongoCollectionDrop()

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }
    }

    "get a minimal details cache record as DataEntry by id in Mongo collection when encrypted true" in {

      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      mongoCollectionDrop()

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }
    }
  }

  "getLastUpdated" must {
    "get a minimal details cache data's lastUpdated field by id in Mongo collection when encrypted false" in {

      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      mongoCollectionDrop()

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.getLastUpdated(record._1)
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB =>
        documentsInDB.get.compareTo(DateTime.now(DateTimeZone.UTC)) mustBe -1
      }
    }

    "get a minimal details cache data's lastUpdated field by id in Mongo collection when encrypted true" in {

      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      mongoCollectionDrop()

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.getLastUpdated(record._1)
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB =>
        documentsInDB.get.compareTo(DateTime.now(DateTimeZone.UTC)) mustBe -1

      }
    }
  }

  "remove" must {
    "delete an existing JsonDataEntry minimal details cache record by id in Mongo collection when encrypted false" in {

      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      mongoCollectionDrop()

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }
      val documentsInDB2 = for {
        _ <- minimalDetailsCacheRepository.remove(record._1)
        documentsInDB2 <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB2 =>
        documentsInDB2.isDefined mustBe false
      }
    }

    "not delete an existing JsonDataEntry minimal details cache record by id in Mongo collection when encrypted false and id incorrect" in {

      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      mongoCollectionDrop()

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }

      val documentsInDB2 = for {
        _ <- minimalDetailsCacheRepository.remove("2")
        documentsInDB2 <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB2 =>
        documentsInDB2.isDefined mustBe true
      }
    }

    "delete an existing DataEntry minimal details cache record by id in Mongo collection when encrypted true" in {

      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      mongoCollectionDrop()

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }

      val documentsInDB2 = for {
        _ <- minimalDetailsCacheRepository.remove(record._1)
        documentsInDB2 <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB2 =>
        documentsInDB2.isDefined mustBe false
      }
    }

    "not delete an existing DataEntry minimal details cache record by id in Mongo collection when encrypted true" in {

      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      mongoCollectionDrop()

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }

      val documentsInDB2 = for {
        _ <- minimalDetailsCacheRepository.remove("2")
        documentsInDB2 <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB2 =>
        documentsInDB2.isDefined mustBe true
      }
    }
  }
}


object MinimalDetailsCacheRepositorySpec extends AnyWordSpec with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits._

  private val mockAppConfig = mock[Configuration]
  private val mockConfig = mock[Config]
  private val databaseName = "pension-administrator"
  private val databasePort = 12349
  private val mongoUri: String = s"mongodb://127.0.0.1:$databasePort/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
  private val mongoComponent = MongoComponent(mongoUri)

  private def mongoCollectionDrop(): Void = Await
    .result(minimalDetailsCacheRepository.collection.drop().toFuture(), Duration.Inf)

  def minimalDetailsCacheRepository: ManageCacheRepository = new MinimalDetailsCacheRepository(mongoComponent, mockAppConfig)
}