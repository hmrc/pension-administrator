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

import com.mongodb.client.model.FindOneAndUpdateOptions
import models.ToggleDetails
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.mongo.play.json.Codecs
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.mongo.MongoComponent
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{Filters, Updates}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import scala.concurrent.ExecutionContext.Implicits.global


class ToggleDataRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with EmbeddedMongoDBSupport with BeforeAndAfter with
  BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures { // scalastyle:off magic.number

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(30, Seconds), Span(1, Millis))

  import ToggleDataRepositorySpec._

  var toggleDataRepository: ToggleDataRepository = _

  private val toggleDetails1 = ToggleDetails("Toggle-first", Some("Toggle Description 1 "), isEnabled = true)
  private val toggleDetails2 = ToggleDetails("Toggle-second", Some("Toggle Description 2 "), isEnabled = false)


  /*
  override def beforeAll(): Unit = {
    when(mockAppConfig.mongoDBAFTBatchesMaxTTL).thenReturn(43200)
    when(mockAppConfig.mongoDBAFTBatchesTTL).thenReturn(999999)
    when(mockAppConfig.mongoDBAFTBatchesCollectionName).thenReturn(collectionName)
    initMongoDExecutable()
    startMongoD()
    aftBatchedDataCacheRepository = buildRepository(mongoHost, mongoPort)
    super.beforeAll()
  }
   */

  override def beforeAll(): Unit = {
    when(mockAppConfig.get[String](path = "mongodb.pension-administrator-cache.toggle-data.name")).thenReturn("toggle-data")
    initMongoDExecutable()
    startMongoD()
    toggleDataRepository = buildFormRepository(mongoHost, mongoPort)
    super.beforeAll()
  }


  //  private def mongoCollectionInsert(aftBatchedDataCacheRepository2: AftBatchedDataCacheRepository, id: String,
  //                                           sessionId: String, seqBatchInfo: Seq[BatchInfo]): Future[Unit] = {
  //
  //    def selector(batchType: BatchType, batchNo: Int): Bson = {
  //      Filters.and(
  //        Filters.eq(uniqueAftIdKey, id + sessionId),
  //        Filters.eq(batchTypeKey, batchType.toString),
  //        Filters.eq(batchNoKey, batchNo)
  //      )
  //    }
  //
  //    val seqFutureUpdateWriteResult = seqBatchInfo.map { bi =>
  //      val modifier = Updates.combine(
  //        set(idKey, id),
  //        set("id", sessionId),
  //        set("data", Codecs.toBson(bi.jsValue))
  //      )
  //
  //      val upsertOptions = new FindOneAndUpdateOptions().upsert(true)
  //      aftBatchedDataCacheRepository2.collection.findOneAndUpdate(
  //        filter = selector(bi.batchType, bi.batchNo),
  //        update = modifier,
  //        upsertOptions
  //      ).toFuture().map(_ => (): Unit)
  //
  //    }
  //    Future.sequence(seqFutureUpdateWriteResult).map(_ => (): Unit)
  //  }


  override def afterAll(): Unit =
    stopMongoD()

  private def upsertJsObject(jsonObjectToInsert: JsObject): Future[Unit] = {
    toggleDataRepository.collection.findOneAndUpdate(
      filter = Filters.eq("toggleName", "Test-toggle"),
      update = Updates.combine(
        set("toggleName", "Test-toggle"),
        set("toggleDescription", "Test description"),
        set("data", Codecs.toBson(jsonObjectToInsert))
      ), new FindOneAndUpdateOptions().upsert(true)
    ).toFuture().map(_ => ())
  }

  "getAllFeatureToggles" must {
    "get FeatureToggles from Mongo collection" in {
      val documentsInDB = for {
        _ <- toggleDataRepository.collection.drop().toFuture()
        _ <- upsertJsObject(Json.obj(
          "toggleName" -> "Test-toggle",
          "toggleDescription" -> "Test description",
          "isEnabled" -> true
        ))
        documentsInDB <- toggleDataRepository.getAllFeatureToggles
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.size mustBe 1
      }
    }
  }

  //  "upsertFeatureToggle" must {
  //    "set new FeatureToggles in Mongo collection" in {
  //
  //      val documentsInDB = for {
  //        _ <- toggleDataRepository.collection.drop().toFuture()
  //        _ <- toggleDataRepository.upsertFeatureToggle(toggleDetails1)
  //        _ <- toggleDataRepository.upsertFeatureToggle(toggleDetails2)
  //        documentsInDB <- toggleDataRepository.collection.find[ToggleDetails](Filters.eq("toggleName", "data")).toFuture()
  //      } yield documentsInDB
  //
  //      whenReady(documentsInDB) { documentsInDB =>
  //        documentsInDB.size mustBe 2
  //      }
  //    }
  //  }
  //
  //  "deleteFeatureToggle" must {
  //    "delete a feature toggle in the Mongo collection" in {
  //
  //      val documentsInDB = for {
  //        _ <- toggleDataRepository.collection.drop().toFuture()
  //        _ <- toggleDataRepository.upsertFeatureToggle(toggleDetails1)
  //        _ <- toggleDataRepository.upsertFeatureToggle(toggleDetails2)
  //        _ <- toggleDataRepository.deleteFeatureToggle(toggleDetails2.toggleName)
  //        documentsInDB <- toggleDataRepository.collection.find[ToggleDetails](Filters.eq("toggleName", "data")).toFuture()
  //      } yield documentsInDB
  //
  //      whenReady(documentsInDB) { documentsInDB =>
  //        documentsInDB.size mustBe 1
  //      }
  //    }
  //  }
}

object ToggleDataRepositorySpec extends AnyWordSpec with MockitoSugar {

  private val mockAppConfig = mock[Configuration]

  private def buildFormRepository(mongoHost: String, mongoPort: Int) = {
    val databaseName = "pension-administrator"
    val mongoUri = s"mongodb://$mongoHost:$mongoPort/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
    new ToggleDataRepository(MongoComponent(mongoUri), mockAppConfig)
  }
}
