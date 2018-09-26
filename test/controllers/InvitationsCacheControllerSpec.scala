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

package controllers

import akka.stream.Materializer
import akka.util.ByteString
import models.Invitation
import org.apache.commons.lang3.RandomUtils
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncFlatSpec, MustMatchers}
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.bson.BSONDocument
import reactivemongo.core.errors.DatabaseException
import repositories.InvitationsCacheRepository
import service.MongoDBFailedException
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{BadRequestException, UnauthorizedException}

import scala.concurrent.Future

class InvitationsCacheControllerSpec extends AsyncFlatSpec with MustMatchers with MockitoSugar {

  implicit lazy val mat: Materializer = new GuiceApplicationBuilder().configure("run.mode" -> "Test").build().materializer

  private def configuration(encrypted: Boolean = true) = Configuration(
    "mongodb.pension-administrator-cache.maxSize" -> 512000,
    "encrypted" -> encrypted
  )

  private val invitation = Invitation("test-pstr", "test-scheme", "test-inviter-psa-id", "inviteePsaId", "inviteeName")

  private val repo = mock[InvitationsCacheRepository]
  private val authConnector: AuthConnector = mock[AuthConnector]

  private class InvitationsCacheControllerImpl(
                                                repo: InvitationsCacheRepository,
                                                authConnector: AuthConnector,
                                                encrypted: Boolean
                                              ) extends InvitationsCacheController(configuration(encrypted), repo, authConnector)

  def controller(repo: InvitationsCacheRepository, authConnector: AuthConnector, encrypted: Boolean): InvitationsCacheController = {
    new InvitationsCacheControllerImpl(repo, authConnector, encrypted)
  }

  // scalastyle:off method.length
  def validCacheController(encrypted: Boolean): Unit = {
    val msg = if (encrypted) "where encrypted" else "where not encrypted"

    s".insert $msg" should "return 201 when the request body can be parsed and passed to the repository successfully" in {

      when(repo.insert(any())(any())).thenReturn(Future.successful(true))
      when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

      val result = call(controller(repo, authConnector, encrypted).add, FakeRequest("POST", "/").withJsonBody(Json.toJson(invitation)))
      status(result) mustEqual CREATED
    }

    it should "return 400 when the request body cannot be parsed" in {

      when(repo.insert(any())(any())).thenReturn(Future.successful(true))
      when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

      recoverToExceptionIf[BadRequestException](
        call(controller(repo, authConnector, encrypted).add, FakeRequest().withRawBody(ByteString(RandomUtils.nextBytes(512001))))).map {
        ex =>
          ex.responseCode mustBe BAD_REQUEST
          ex.message mustBe "Bad Request with no request body returned for PSA Invitation"
      }
    }

    it should "throw an exception when the call is not authorised" in {

      when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.failed {
        new UnauthorizedException("")
      })

      recoverToExceptionIf[UnauthorizedException](
        call(controller(repo, authConnector, encrypted).add, FakeRequest().withRawBody(ByteString("foo")))).map {
        ex =>
          ex.responseCode mustBe UNAUTHORIZED
      }
    }

    it should "throw a MongoDBFailedException when the mongodb insert call is failed with DatabaseException" in {
      val databaseException = new DatabaseException {
        override def originalDocument: Option[BSONDocument] = None
        override def code: Option[Int] = None
        override def message: String = "mongo error"
      }

      when(repo.insert(any())(any())).thenReturn(Future.failed(databaseException))
      when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

      recoverToExceptionIf[MongoDBFailedException](
        call(controller(repo, authConnector, encrypted).add, FakeRequest("POST", "/").withJsonBody(Json.toJson(invitation)))).map {
        ex =>
          ex.responseCode mustBe INTERNAL_SERVER_ERROR
          ex.message must include("mongo error")
      }
    }
  }

  // scalastyle:on method.length

  "InvitationsCacheController" should behave like validCacheController(encrypted = false)
  "InvitationsCacheController" should behave like validCacheController(encrypted = true)
}
