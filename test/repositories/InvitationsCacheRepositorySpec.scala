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

import com.typesafe.config.Config
import models.{Invitation, SchemeReferenceNumber}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito._
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Millis, Milliseconds, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.Json
import repositories.InvitationsCacheEntry.{DataEntry, JsonDataEntry}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.ExecutionContext.Implicits.global

class InvitationsCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with EmbeddedMongoDBSupport with BeforeAndAfter with
  BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures { // scalastyle:off magic.number

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(30, Seconds), Span(1, Millis))

  import InvitationsCacheRepositorySpec._

  var invitationsCacheRepository: InvitationsCacheRepository = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockAppConfig.underlying).thenReturn(mockConfig)
    when(mockConfig.getString("mongodb.pension-administrator-cache.invitations.name")).thenReturn("invitations")
    when(mockConfig.getString("manage.json.encryption.key")).thenReturn("gvBoGdgzqG1AarzF1LY0zQ==")
  }

  override def beforeAll(): Unit = {
    initMongoDExecutable()
    startMongoD()
    invitationsCacheRepository = buildFormRepository(mongoHost, mongoPort)
    super.beforeAll()
  }

  "upsert" must {
    "save a new invitation cache as JsonDataEntry in Mongo collection when encrypted false and collection is empty" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))

      val record = Invitation(SchemeReferenceNumber("id"), "pstr", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC))

      val filters = Filters.and(Filters.eq("inviteePsaId", "A2500002"), Filters.eq("pstr", "pstr"))
      val documentsInDB = for {
        _ <- invitationsCacheRepository.collection.drop().toFuture()
        _ <- invitationsCacheRepository.upsert(record)
        documentsInDB <- invitationsCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) {
        documentsInDB =>
          documentsInDB.size mustBe 1
      }
    }

    "update an existing invitation cache as JsonDataEntry in Mongo collection when encrypted false" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))

      val record1 = Invitation(SchemeReferenceNumber("id"), "pstr", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC))
      val record2 = Invitation(SchemeReferenceNumber("id"), "pstr", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC).plusDays(1))
      val filters = Filters.and(Filters.eq("inviteePsaId", "A2500002"), Filters.eq("pstr", "pstr"))

      val documentsInDB = for {
        _ <- invitationsCacheRepository.collection.drop().toFuture()
        _ <- invitationsCacheRepository.upsert(record1)
        _ <- invitationsCacheRepository.upsert(record2)
        documentsInDB <- invitationsCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) {
        documentsInDB =>
          documentsInDB.size mustBe 1
          documentsInDB.head.expireAt mustBe record2.expireAt
          documentsInDB.head.data mustBe Json.toJson(record2)
      }
    }

    "save a new invitation cache as JsonDataEntry in Mongo collection when encrypted false and inviteePsaId is not same" in {

      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))

      val record1 = Invitation(SchemeReferenceNumber("id"), "pstr", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC))
      val record2 = Invitation(SchemeReferenceNumber("id"), "pstr", "schemeName",
        PsaId("A9876543"), PsaId("A9876544"), "inviteeName", DateTime.now(DateTimeZone.UTC).plusDays(1))

      val documentsInDB = for {
        _ <- invitationsCacheRepository.collection.drop().toFuture()
        _ <- invitationsCacheRepository.upsert(record1)
        _ <- invitationsCacheRepository.upsert(record2)
        documentsInDB <- invitationsCacheRepository.collection.find[JsonDataEntry]().toFuture()
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) {
        documentsInDB =>
          documentsInDB.size mustBe 2
      }
    }

    "save a new invitation cache as JsonDataEntry in Mongo collection when encrypted false and pstr is not same" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))

      val record1 = Invitation(SchemeReferenceNumber("id"), "pstr1", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC))
      val record2 = Invitation(SchemeReferenceNumber("id"), "pstr2", "schemeName",
        PsaId("A9876543"), PsaId("A9876544"), "inviteeName", DateTime.now(DateTimeZone.UTC).plusDays(1))

      val documentsInDB = for {
        _ <- invitationsCacheRepository.collection.drop().toFuture()
        _ <- invitationsCacheRepository.upsert(record1)
        _ <- invitationsCacheRepository.upsert(record2)
        documentsInDB <- invitationsCacheRepository.collection.find[JsonDataEntry]().toFuture()
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) {
        documentsInDB =>
          documentsInDB.size mustBe 2
      }
    }

    "save a new invitation cache as DataEntry in Mongo collection when encrypted true and collection is empty" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))

      val record = Invitation(SchemeReferenceNumber("id"), "pstr", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC))
      val filters = Filters.and(Filters.eq("inviteePsaId", "qPiTIC6PennxowJl8O5lqw=="), Filters.eq("pstr", "U87ezLMl9HOlyHOsEGXrNg=="))

      val documentsInDB = for {
        _ <- invitationsCacheRepository.collection.drop().toFuture()
        _ <- invitationsCacheRepository.upsert(record)
        documentsInDB <- invitationsCacheRepository.collection.find[DataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) {
        documentsInDB =>
          documentsInDB.size mustBe 1
      }
    }

    "update an existing invitation cache as DataEntry in Mongo collection when encrypted true" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))

      val record1 = Invitation(SchemeReferenceNumber("id"), "pstr", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC))
      val record2 = Invitation(SchemeReferenceNumber("id"), "pstr", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC).plusDays(1))
      val filters = Filters.and(Filters.eq("inviteePsaId", "qPiTIC6PennxowJl8O5lqw=="), Filters.eq("pstr", "U87ezLMl9HOlyHOsEGXrNg=="))

      val documentsInDB = for {
        _ <- invitationsCacheRepository.collection.drop().toFuture()
        _ <- invitationsCacheRepository.upsert(record1)
        _ <- invitationsCacheRepository.upsert(record2)
        documentsInDB <- invitationsCacheRepository.collection.find[DataEntry](filters).toFuture()
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) {
        documentsInDB =>
          documentsInDB.size mustBe 1
          documentsInDB.head.expireAt mustBe record2.expireAt
      }
    }

    "save a new invitation cache as DataEntry in Mongo collection when encrypted true and inviteePsaId is not same" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))

      val record1 = Invitation(SchemeReferenceNumber("id"), "pstr", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC))
      val record2 = Invitation(SchemeReferenceNumber("id"), "pstr", "schemeName",
        PsaId("A9876543"), PsaId("A9876544"), "inviteeName", DateTime.now(DateTimeZone.UTC).plusDays(1))

      val documentsInDB = for {
        _ <- invitationsCacheRepository.collection.drop().toFuture()
        _ <- invitationsCacheRepository.upsert(record1)
        _ <- invitationsCacheRepository.upsert(record2)
        documentsInDB <- invitationsCacheRepository.collection.find[DataEntry]().toFuture()
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) {
        documentsInDB =>
          documentsInDB.size mustBe 2
      }
    }

    "save a new invitation cache as DataEntry in Mongo collection when encrypted true and pstr is not same" in {
      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))

      val record1 = Invitation(SchemeReferenceNumber("id"), "pstr1", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC))
      val record2 = Invitation(SchemeReferenceNumber("id"), "pstr2", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC).plusDays(1))

      val documentsInDB = for {
        _ <- invitationsCacheRepository.collection.drop().toFuture()
        _ <- invitationsCacheRepository.upsert(record1)
        _ <- invitationsCacheRepository.upsert(record2)
        documentsInDB <- invitationsCacheRepository.collection.find[DataEntry]().toFuture()
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) {
        documentsInDB =>
          documentsInDB.size mustBe 2
      }
    }
  }

  "get" must {
    "get an invitation cache record as JsonDataEntry by ids in Mongo collection when encrypted false" in {

      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))

      val record = Invitation(SchemeReferenceNumber("id"), "pstr", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC))

      val documentsInDB = for {
        _ <- invitationsCacheRepository.collection.drop().toFuture()
        _ <- invitationsCacheRepository.upsert(record)
        documentsInDB <- invitationsCacheRepository.getByKeys(Map("inviteePsaId" -> "A2500002", "pstr" -> "pstr"))
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }
    }

    "get an invitation cache record as DataEntry by ids in Mongo collection when encrypted true" in {

      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))

      val record = Invitation(SchemeReferenceNumber("id"), "pstr", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC))

      val documentsInDB = for {
        _ <- invitationsCacheRepository.collection.drop().toFuture()
        _ <- invitationsCacheRepository.upsert(record)
        documentsInDB <- invitationsCacheRepository.getByKeys(Map("inviteePsaId" -> "A2500002", "pstr" -> "pstr"))
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }
    }
  }

  "remove" must {
    "delete an existing JsonDataEntry invitation cache record by ids in Mongo collection when encrypted false" in {

      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))

      val record = Invitation(SchemeReferenceNumber("id"), "pstr", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC))
      val filters = Map("inviteePsaId" -> "A2500002", "pstr" -> "pstr")

      val documentsInDB = for {
        _ <- invitationsCacheRepository.collection.drop().toFuture()
        _ <- invitationsCacheRepository.upsert(record)
        documentsInDB <- invitationsCacheRepository.getByKeys(filters)
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }

      val documentsInDB2 = for {
        _ <- invitationsCacheRepository.remove(filters)
        documentsInDB2 <- invitationsCacheRepository.getByKeys(filters)
      } yield documentsInDB2

      whenReady(documentsInDB2, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB2 =>
        documentsInDB2.isDefined mustBe false
      }
    }

    " not delete an existing JsonDataEntry invitation cache record by ids in Mongo collection when encrypted false and filters incorrect" in {

      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(false))

      val record = Invitation(SchemeReferenceNumber("id"), "pstr", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC))
      val filters = Map("inviteePsaId" -> "A2500002", "pstr" -> "pstr")

      val documentsInDB = for {
        _ <- invitationsCacheRepository.collection.drop().toFuture()
        _ <- invitationsCacheRepository.upsert(record)
        documentsInDB <- invitationsCacheRepository.getByKeys(filters)
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }

      val documentsInDB2 = for {
        _ <- invitationsCacheRepository.remove(Map("inviteePsaId" -> "A2500002", "pstr" -> "pstr2"))
        documentsInDB2 <- invitationsCacheRepository.getByKeys(filters)
      } yield documentsInDB2

      whenReady(documentsInDB2, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB2 =>
        documentsInDB2.isDefined mustBe true
      }
    }

    "delete an existing DataEntry invitation cache record by ids in Mongo collection when encrypted true" in {

      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))

      val record = Invitation(SchemeReferenceNumber("id"), "pstr", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC))
      val filters = Map("inviteePsaId" -> "A2500002", "pstr" -> "pstr")

      val documentsInDB = for {
        _ <- invitationsCacheRepository.collection.drop().toFuture()
        _ <- invitationsCacheRepository.upsert(record)
        documentsInDB <- invitationsCacheRepository.getByKeys(filters)
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }

      val documentsInDB2 = for {
        _ <- invitationsCacheRepository.remove(filters)
        documentsInDB2 <- invitationsCacheRepository.getByKeys(filters)
      } yield documentsInDB2

      whenReady(documentsInDB2, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB2 =>
        documentsInDB2.isDefined mustBe false
      }
    }

    "not delete an existing DataEntry invitation cache record by ids in Mongo collection when encrypted true" in {

      when(mockAppConfig.getOptional[Boolean](path = "encrypted")).thenReturn(Some(true))

      val record = Invitation(SchemeReferenceNumber("id"), "pstr", "schemeName",
        PsaId("A2500001"), PsaId("A2500002"), "inviteeName", DateTime.now(DateTimeZone.UTC))
      val filters = Map("inviteePsaId" -> "A2500002", "pstr" -> "pstr")

      val documentsInDB = for {
        _ <- invitationsCacheRepository.collection.drop().toFuture()
        _ <- invitationsCacheRepository.upsert(record)
        documentsInDB <- invitationsCacheRepository.getByKeys(filters)
      } yield documentsInDB

      whenReady(documentsInDB, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }
      val documentsInDB2 = for {
        _ <- invitationsCacheRepository.remove(Map("inviteePsaId" -> "A2500002", "pstr" -> "pstr2"))
        documentsInDB2 <- invitationsCacheRepository.getByKeys(filters)
      } yield documentsInDB2

      whenReady(documentsInDB2, timeout = Timeout(Span(500L, Milliseconds))) { documentsInDB2 =>
        documentsInDB2.isDefined mustBe true
      }
    }

  }
}

object InvitationsCacheRepositorySpec extends AnyWordSpec with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits._

  private val mockAppConfig = mock[Configuration]
  private val mockConfig = mock[Config]

  private def buildFormRepository(mongoHost: String, mongoPort: Int) = {
    val databaseName = "pension-administrator"
    val mongoUri = s"mongodb://$mongoHost:$mongoPort/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
    new InvitationsCacheRepository(MongoComponent(mongoUri), mockAppConfig)
  }
}
