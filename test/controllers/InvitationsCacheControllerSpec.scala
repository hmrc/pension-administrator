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
import org.apache.commons.lang3.RandomUtils
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncFlatSpec, MustMatchers}
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.bson.BSONDocument
import reactivemongo.core.errors.DatabaseException
import repositories.InvitationsCacheRepository
import service.MongoDBFailedException
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{BadRequestException, UnauthorizedException}
import utils.testhelpers.InvitationBuilder._

import scala.concurrent.Future

class InvitationsCacheControllerSpec extends AsyncFlatSpec with MustMatchers with MockitoSugar {
  implicit lazy val mat: Materializer = new GuiceApplicationBuilder().configure("run.mode" -> "Test").build().materializer

  private def configuration = Configuration("mongodb.pension-administrator-cache.maxSize" -> 512000)

  private val repo = mock[InvitationsCacheRepository]
  private val authConnector: AuthConnector = mock[AuthConnector]

  def controller: InvitationsCacheController = new InvitationsCacheController(configuration, repo, authConnector, stubControllerComponents())

  // scalastyle:off method.length
  def validCacheControllerWithInsert(): Unit = {

    ".insert" should "return 201 when the request body can be parsed and passed to the repository successfully" in {

      when(repo.insert(any())(any())).thenReturn(Future.successful(true))
      when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

      val result = call(controller.add, FakeRequest("POST", "/").withJsonBody(Json.toJson(invitation1)))
      status(result) mustEqual CREATED
    }

    it should "return 400 when the request body cannot be parsed" in {

      when(repo.insert(any())(any())).thenReturn(Future.successful(true))
      when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

      recoverToExceptionIf[BadRequestException](
        call(controller.add, FakeRequest().withRawBody(ByteString(RandomUtils.nextBytes(512001))))).map {
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
        call(controller.add, FakeRequest().withRawBody(ByteString("foo")))).map {
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
        call(controller.add, FakeRequest("POST", "/").withJsonBody(Json.toJson(invitation1)))).map {
        ex =>
          ex.responseCode mustBe INTERNAL_SERVER_ERROR
          ex.message must include("mongo error")
      }
    }
  }

  // scalastyle:on method.length

  private def validCacheControllerWithGet(s: String, map: Map[String, String], testMethod: () => Action[AnyContent]): Unit = {
    s"$s should work for request with headers: $map" should "return 200 and the relevant data when it exists" in {
      when(repo.getByKeys(eqTo(map))(any())) thenReturn Future.successful(Some(invitationList))
      when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())
      val result = testMethod()(FakeRequest().withHeaders(map.toSeq: _*))
      status(result) mustEqual OK
      contentAsString(result) mustEqual Json.toJson(invitationList).toString
    }

    it should "return 404 when the data doesn't exist" in {
      when(repo.getByKeys(eqTo(map))(any())) thenReturn Future.successful {
        None
      }
      when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())
      val result = testMethod()(FakeRequest().withHeaders(map.toSeq: _*))
      status(result) mustEqual NOT_FOUND
    }

    it should "throw an exception when the repository call fails" in {
      when(repo.getByKeys(eqTo(map))(any())) thenReturn Future.failed {
        new Exception()
      }
      when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())
      val result = testMethod()(FakeRequest().withHeaders(map.toSeq: _*))
      an[Exception] must be thrownBy status(result)
    }

    it should "throw an exception when the call is not authorised" in {
      when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
        new UnauthorizedException("")
      }
      val result = testMethod()(FakeRequest().withHeaders(map.toSeq: _*))
      an[UnauthorizedException] must be thrownBy status(result)
    }
  }


  private def validCacheControllerWithRemove(s: String): Unit = {
    s"$s" should "return 200 when the data is removed successfully" in {
      when(repo.remove(eqTo(mapBothKeys))(any())) thenReturn Future.successful(true)
      when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

      val result = controller.remove()(FakeRequest().withHeaders(mapBothKeys.toSeq: _*))

      status(result) mustEqual OK
    }

    it should "throw an exception when the call is not authorised" in {
      when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
        new UnauthorizedException("")
      }
      val result = controller.remove()(FakeRequest().withHeaders(mapBothKeys.toSeq: _*))
      an[UnauthorizedException] must be thrownBy status(result)
    }
  }

  "InvitationsCacheController" should behave like validCacheControllerWithInsert
  it should behave like validCacheControllerWithGet("get", mapBothKeys, controller.get _)
  it should behave like validCacheControllerWithGet("getForScheme", mapPstr, controller.getForScheme _)
  it should behave like validCacheControllerWithGet("getForInvitee", mapInviteePsaId, controller.getForInvitee _)
  it should behave like validCacheControllerWithRemove("remove")
}

