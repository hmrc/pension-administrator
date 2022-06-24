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
import models.FeatureToggle
import models.FeatureToggleName.{PsaFromIvToPdv, PsaRegistration}
import org.mockito.MockitoSugar
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.Configuration
import repositories.FeatureToggleMongoFormatter.{FeatureToggles, featureToggles, id}
import uk.gov.hmrc.mongo.MongoComponent
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class AdminDataRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with MongoEmbedDatabase with BeforeAndAfter with
  BeforeAndAfterEach { // scalastyle:off magic.number

  import AdminDataRepositorySpec._

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockAppConfig.get[String](path = "mongodb.pension-administrator-cache.admin-data.name")).thenReturn("admin-data")
  }

  withEmbedMongoFixture(port = 24680) { _ =>

    "getFeatureToggle" must {
      "get FeatureToggles from Mongo collection" in {
        mongoCollectionDrop()

        val documentsInDB = for {
          _ <- adminDataRepository.collection.insertOne(
            FeatureToggles("toggles", Seq(FeatureToggle(PsaRegistration, enabled = true), FeatureToggle(PsaFromIvToPdv, enabled = false)))).headOption()
          documentsInDB <- adminDataRepository.getFeatureToggles
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB.size mustBe 2
        }
      }
    }

    "setFeatureToggle" must {
      "set new FeatureToggles in Mongo collection" in {
        mongoCollectionDrop()
        val documentsInDB = for {
          _ <- adminDataRepository.setFeatureToggles(Seq(FeatureToggle(PsaRegistration, enabled = true),
            FeatureToggle(PsaFromIvToPdv, enabled = false)))
          documentsInDB <- adminDataRepository.collection.find[FeatureToggles](Filters.eq(id, featureToggles)).headOption()
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB.map(_.toggles.size mustBe 2)
        }
      }
    }
  }
}

object AdminDataRepositorySpec extends AnyWordSpec with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits._

  private val mockAppConfig = mock[Configuration]
  private val databaseName = "pension-administrator"
  private val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
  private val mongoComponent = MongoComponent(mongoUri)

  private def mongoCollectionDrop(): Void = Await
    .result(adminDataRepository.collection.drop().toFuture(), Duration.Inf)

  def adminDataRepository = new AdminDataRepository(mongoComponent, mockAppConfig)
}
