/*
 * Copyright 2025 HM Revenue & Customs
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

import base.MongoConfig
import com.typesafe.config.Config
import org.mockito.Mockito.*
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.bson.{BsonDocument, BsonString}
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

class MinimalDetailsCacheRepositorySpec extends AnyWordSpec
                                          with MockitoSugar
                                          with Matchers
                                          with BeforeAndAfter
                                          with BeforeAndAfterEach
                                          with BeforeAndAfterAll
                                          with ScalaFutures
                                          with MongoConfig { // scalastyle:off magic.number

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(30, Seconds), Span(1, Millis))

  import MinimalDetailsCacheRepositorySpec.*

  private val idField: String = "id"

  override def beforeAll(): Unit = {
    when(mockAppConfig.underlying).thenReturn(mockConfig)
    when(mockAppConfig.get[String](path = "mongodb.pension-administrator-cache.minimal-detail.name")).thenReturn("minimal-detail")
    when(mockAppConfig.get[Int]("mongodb.pension-administrator-cache.minimal-detail.timeToLiveInSeconds")).thenReturn(3600)
    when(mockConfig.getString("manage.json.encryption.key")).thenReturn("gvBoGdgzqG1AarzF1LY0zQ==")
    super.beforeAll()
  }


  "upsert" must {
    "save a new minimal details cache as JsonDataEntry in Mongo collection when encrypted false and collection is empty" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val minimalDetailsCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val filters = Filters.eq(idField, record._1)

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size.mustBe(1)
      }
    }

    "update an existing minimal details cache as JsonDataEntry in Mongo collection when encrypted false" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val minimalDetailsCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-1", Json.parse("""{"data":"2"}"""))
      val filters = Filters.eq(idField, "id-1")

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(record1._1, record1._2)
        _ <- minimalDetailsCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- minimalDetailsCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size `mustBe` 1
          documentsInDB.head.data.mustBe(record2._2)
          documentsInDB.head.data.must(not) be record1._2
      }
    }

    "save a new minimal details cache as JsonDataEntry in Mongo collection when encrypted false and id is not same" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val minimalDetailsCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-2", Json.parse("""{"data":"2"}"""))

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(record1._1, record1._2)
        _ <- minimalDetailsCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- minimalDetailsCacheRepository.collection.find[JsonDataEntry]().toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size.mustBe(2)
      }
    }

    "save a new minimal details cache as DataEntry in Mongo collection when encrypted true and collection is empty" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val minimalDetailsCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val filters = Filters.eq(idField, record._1)

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.collection.find[DataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size.mustBe(1)
      }
    }

    "update an existing minimal details cache as DataEntry in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val minimalDetailsCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-1", Json.parse("""{"data":"2"}"""))
      val filters = Filters.eq(idField, "id-1")

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(record1._1, record1._2)
        _ <- minimalDetailsCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- minimalDetailsCacheRepository.collection.find[DataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size.mustBe(1)
      }
    }

    "insert a new minimal details cache as DataEntry in Mongo collection when encrypted true and id is not same" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val minimalDetailsCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-2", Json.parse("""{"data":"2"}"""))

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(record1._1, record1._2)
        _ <- minimalDetailsCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- minimalDetailsCacheRepository.collection.find[DataEntry]().toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size.mustBe(2)
      }
    }
    "save lastUpdated value as a date" in {
      when(mockAppConfig.getOptional[Boolean](path= "encrypted")).thenReturn(Some(false))
      val minimalDetailsCacheRepository = buildFormRepository(mongoHost, mongoPort)
      val ftr = minimalDetailsCacheRepository.collection.drop().toFuture().flatMap { _ =>
        minimalDetailsCacheRepository.upsert("id", Json.parse("{}")).flatMap { _ =>
          for {
            stringResults <- minimalDetailsCacheRepository.collection.find[JsonDataEntry](
              BsonDocument("lastUpdated" -> BsonDocument("$type" -> BsonString("string")))
            ).toFuture()
            dateResults <- minimalDetailsCacheRepository.collection.find[JsonDataEntry](
              BsonDocument("lastUpdated" -> BsonDocument("$type" -> BsonString("date")))
            ).toFuture()
          } yield stringResults -> dateResults
        }
      }

      whenReady(ftr) { case (stringResults, dateResults) =>
        stringResults.length `mustBe` 0
        dateResults.length.mustBe(1)
      }
    }
  }

  "get" must {
    "get a minimal details cache record as JsonDataEntry by id in Mongo collection when encrypted false" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val minimalDetailsCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined.mustBe(true)
      }
    }

    "get a minimal details cache record as DataEntry by id in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val minimalDetailsCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined.mustBe(true)
      }
    }
  }

  "getLastUpdated" must {
    "get a minimal details cache data's lastUpdated field by id in Mongo collection when encrypted false" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val minimalDetailsCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.getLastUpdated(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined.mustBe(true)
      }
    }

    "get a minimal details cache data's lastUpdated field by id in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val minimalDetailsCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.getLastUpdated(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined.mustBe(true)

      }
    }
  }

  "remove" must {
    "delete an existing JsonDataEntry minimal details cache record by id in Mongo collection when encrypted false" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val minimalDetailsCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined.mustBe(true)
      }
      val documentsInDB2 = for {
        _ <- minimalDetailsCacheRepository.remove(record._1)
        documentsInDB2 <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2) { documentsInDB2 =>
        documentsInDB2.isDefined.mustBe(false)
      }
    }

    "not delete an existing JsonDataEntry minimal details cache record by id in Mongo collection when encrypted false and id incorrect" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val minimalDetailsCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined.mustBe(true)
      }

      val documentsInDB2 = for {
        _ <- minimalDetailsCacheRepository.remove("2")
        documentsInDB2 <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2) { documentsInDB2 =>
        documentsInDB2.isDefined.mustBe(true)
      }
    }

    "delete an existing DataEntry minimal details cache record by id in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val minimalDetailsCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined.mustBe(true)
      }

      val documentsInDB2 = for {
        _ <- minimalDetailsCacheRepository.remove(record._1)
        documentsInDB2 <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2) { documentsInDB2 =>
        documentsInDB2.isDefined.mustBe(false)
      }
    }

    "not delete an existing DataEntry minimal details cache record by id in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val minimalDetailsCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- minimalDetailsCacheRepository.collection.drop().toFuture()
        _ <- minimalDetailsCacheRepository.upsert(record._1, record._2)
        documentsInDB <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined.mustBe(true)
      }

      val documentsInDB2 = for {
        _ <- minimalDetailsCacheRepository.remove("2")
        documentsInDB2 <- minimalDetailsCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2) { documentsInDB2 =>
        documentsInDB2.isDefined.mustBe(true)
      }
    }
  }
}

object MinimalDetailsCacheRepositorySpec extends AnyWordSpec with MockitoSugar {

  private val mockAppConfig = mock[Configuration]
  private val mockConfig = mock[Config]

  private def buildFormRepository(mongoHost: String, mongoPort: Int) = {
    val databaseName = "pension-administrator"
    val mongoUri = s"mongodb://$mongoHost:$mongoPort/$databaseName?heartbeatFrequencyMS=1000"
    new MinimalDetailsCacheRepository(MongoComponent(mongoUri), mockAppConfig)
  }
}
