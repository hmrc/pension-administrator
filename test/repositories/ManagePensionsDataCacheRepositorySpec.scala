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
import org.mockito.Mockito.when
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

class ManagePensionsDataCacheRepositorySpec extends AnyWordSpec
                                              with MockitoSugar
                                              with Matchers
                                              with BeforeAndAfter
                                              with BeforeAndAfterEach
                                              with BeforeAndAfterAll
                                              with ScalaFutures
                                              with MongoConfig { // scalastyle:off magic.number

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(30, Seconds), Span(1, Millis))

  import ManagePensionsDataCacheRepositorySpec.*

  private val idField: String = "id"

  override def beforeAll(): Unit = {
    when(mockAppConfig.underlying).thenReturn(mockConfig)
    when(mockConfig.getString("mongodb.pension-administrator-cache.manage-pensions.name")).thenReturn("manage-pensions")
    when(mockConfig.getInt("mongodb.pension-administrator-cache.manage-pensions.timeToLiveInSeconds")).thenReturn(3600)
    when(mockConfig.getString("manage.json.encryption.key")).thenReturn("gvBoGdgzqG1AarzF1LY0zQ==")
    super.beforeAll()
  }


  "upsert" must {
    "save a new manage pensions data cache as JsonDataEntry in Mongo collection when encrypted false and collection is empty" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val managePensionsDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val filters = Filters.eq(idField, record._1)

      val documentsInDB = for {
        _ <- managePensionsDataCacheRepository.collection.drop().toFuture()
        _ <- managePensionsDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- managePensionsDataCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size `mustBe` 1
          documentsInDB.map(_.id.mustBe("id-1"))
      }
    }

    "update an existing manage pensions data cache as JsonDataEntry in Mongo collection when encrypted false" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val managePensionsDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-1", Json.parse("""{"data":"2"}"""))
      val filters = Filters.eq(idField, "id-1")

      val documentsInDB = for {
        _ <- managePensionsDataCacheRepository.collection.drop().toFuture()
        _ <- managePensionsDataCacheRepository.upsert(record1._1, record1._2)
        _ <- managePensionsDataCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- managePensionsDataCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size `mustBe` 1
          documentsInDB.head.data.mustBe(record2._2)
          documentsInDB.head.data.must(not) be record1._2
      }
    }

    "insert a new manage pensions data cache as JsonDataEntry in Mongo collection when encrypted false and id is not same" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val managePensionsDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-2", Json.parse("""{"data":"2"}"""))

      val documentsInDB = for {
        _ <- managePensionsDataCacheRepository.collection.drop().toFuture()
        _ <- managePensionsDataCacheRepository.upsert(record1._1, record1._2)
        _ <- managePensionsDataCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- managePensionsDataCacheRepository.collection.find[JsonDataEntry]().toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size.mustBe(2)
      }
    }

    "insert a new manage pensions data cache as DataEntry in Mongo collection when encrypted true and collection is empty" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val managePensionsDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val filters = Filters.eq(idField, record._1)

      val documentsInDB = for {
        _ <- managePensionsDataCacheRepository.collection.drop().toFuture()
        _ <- managePensionsDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- managePensionsDataCacheRepository.collection.find[DataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size.mustBe(1)
      }
    }

    "update an existing manage pensions data cache as DataEntry in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val managePensionsDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-1", Json.parse("""{"data":"2"}"""))
      val filters = Filters.eq(idField, "id-1")

      val documentsInDB = for {
        _ <- managePensionsDataCacheRepository.collection.drop().toFuture()
        _ <- managePensionsDataCacheRepository.upsert(record1._1, record1._2)
        _ <- managePensionsDataCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- managePensionsDataCacheRepository.collection.find[DataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size.mustBe(1)
      }
    }

    "insert a new manage pensions data cache as DataEntry in Mongo collection when encrypted true and id is not same" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val managePensionsDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
      val record2 = ("id-2", Json.parse("""{"data":"2"}"""))

      val documentsInDB = for {
        _ <- managePensionsDataCacheRepository.collection.drop().toFuture()
        _ <- managePensionsDataCacheRepository.upsert(record1._1, record1._2)
        _ <- managePensionsDataCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- managePensionsDataCacheRepository.collection.find[DataEntry]().toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size.mustBe(2)
      }
    }
    "save lastUpdated value as a date" in {
      when(mockAppConfig.getOptional[Boolean](path= "encrypted")).thenReturn(Some(false))
      val managePensionsDataCacheRepository = buildFormRepository(mongoHost, mongoPort)
      val ftr = managePensionsDataCacheRepository.collection.drop().toFuture().flatMap { _ =>
        managePensionsDataCacheRepository.upsert("id", Json.parse("{}")).flatMap { _ =>
          for {
            stringResults <- managePensionsDataCacheRepository.collection.find[JsonDataEntry](
              BsonDocument("lastUpdated" -> BsonDocument("$type" -> BsonString("string")))
            ).toFuture()
            dateResults <- managePensionsDataCacheRepository.collection.find[JsonDataEntry](
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
    "get a manage pensions data cache record as JsonDataEntry by id in Mongo collection when encrypted false" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val managePensionsDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- managePensionsDataCacheRepository.collection.drop().toFuture()
        _ <- managePensionsDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- managePensionsDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined.mustBe(true)
      }
    }

    "get a manage pensions data cache record as DataEntry by id in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val managePensionsDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- managePensionsDataCacheRepository.collection.drop().toFuture()
        _ <- managePensionsDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- managePensionsDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined.mustBe(true)
      }
    }
  }

  "getLastUpdated" must {
    "get a manage pensions cache data's lastUpdated field by id in Mongo collection when encrypted false" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val managePensionsDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- managePensionsDataCacheRepository.collection.drop().toFuture()
        _ <- managePensionsDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- managePensionsDataCacheRepository.getLastUpdated(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined.mustBe(true)
      }
    }

    "get a manage pensions cache data's lastUpdated field by id in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val managePensionsDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- managePensionsDataCacheRepository.collection.drop().toFuture()
        _ <- managePensionsDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- managePensionsDataCacheRepository.getLastUpdated(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined.mustBe(true)
      }
    }
  }

  "remove" must {
    "delete an existing JsonDataEntry manage pensions data cache record by id in Mongo collection when encrypted false" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val managePensionsDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))
      val documentsInDB = for {
        _ <- managePensionsDataCacheRepository.collection.drop().toFuture()
        _ <- managePensionsDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- managePensionsDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined.mustBe(true)
      }

      val documentsInDB2 = for {
        _ <- managePensionsDataCacheRepository.remove(record._1)
        documentsInDB2 <- managePensionsDataCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2) { documentsInDB2 =>
        documentsInDB2.isDefined.mustBe(false)
      }
    }

    "not delete an existing JsonDataEntry manage pensions data cache record by id in Mongo collection when encrypted false and id incorrect" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))
      val managePensionsDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- managePensionsDataCacheRepository.collection.drop().toFuture()
        _ <- managePensionsDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- managePensionsDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined.mustBe(true)
      }

      val documentsInDB2 = for {
        _ <- managePensionsDataCacheRepository.remove("2")
        documentsInDB2 <- managePensionsDataCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2) { documentsInDB2 =>
        documentsInDB2.isDefined.mustBe(true)
      }
    }

    "delete an existing DataEntry manage pensions data cache record by id in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val managePensionsDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- managePensionsDataCacheRepository.collection.drop().toFuture()
        _ <- managePensionsDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- managePensionsDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined.mustBe(true)
      }

      val documentsInDB2 = for {
        _ <- managePensionsDataCacheRepository.remove(record._1)
        documentsInDB2 <- managePensionsDataCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2) { documentsInDB2 =>
        documentsInDB2.isDefined.mustBe(false)
      }
    }

    "not delete an existing DataEntry manage pensions data cache record by id in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))
      val managePensionsDataCacheRepository = buildFormRepository(mongoHost, mongoPort)

      val record = ("id-1", Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- managePensionsDataCacheRepository.collection.drop().toFuture()
        _ <- managePensionsDataCacheRepository.upsert(record._1, record._2)
        documentsInDB <- managePensionsDataCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined.mustBe(true)
      }

      val documentsInDB2 = for {
        _ <- managePensionsDataCacheRepository.remove("2")
        documentsInDB2 <- managePensionsDataCacheRepository.get(record._1)
      } yield documentsInDB2

      whenReady(documentsInDB2) { documentsInDB2 =>
        documentsInDB2.isDefined.mustBe(true)
      }
    }
  }
}

object ManagePensionsDataCacheRepositorySpec extends AnyWordSpec with MockitoSugar {

  private val mockAppConfig = mock[Configuration]
  private val mockConfig = mock[Config]

  private def buildFormRepository(mongoHost: String, mongoPort: Int) = {
    val databaseName = "pension-administrator"
    val mongoUri = s"mongodb://$mongoHost:$mongoPort/$databaseName?heartbeatFrequencyMS=1000"
    new ManagePensionsDataCacheRepository(mockAppConfig, MongoComponent(mongoUri))
  }
}