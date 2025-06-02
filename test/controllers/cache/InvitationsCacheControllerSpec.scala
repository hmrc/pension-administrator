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

package controllers.cache

import com.mongodb.MongoException
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.{eq as eqTo, *}
import org.mockito.Mockito.*
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.*
import service.MongoDBFailedException
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.BadRequestException
import utils.{AuthUtils, RandomUtils}
import utils.testhelpers.InvitationBuilder.*

import scala.concurrent.Future

class InvitationsCacheControllerSpec extends AsyncFlatSpec with Matchers with MockitoSugar {
  private val repo = mock[InvitationsCacheRepository]
  private val authConnector: AuthConnector = mock[AuthConnector]

  val app: Application = new GuiceApplicationBuilder()
    .configure("run.mode" -> "Test")
    .overrides(Seq(
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
      bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
      bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
      bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
      bind[InvitationsCacheRepository].toInstance(repo),
      bind[AuthConnector].toInstance(authConnector)
    ))
    .build()

  implicit lazy val mat: Materializer = app.materializer

  def controller: InvitationsCacheController = app.injector.instanceOf[InvitationsCacheController]

  // scalastyle:off method.length
  def validCacheControllerWithInsert(): Unit = {

    ".insert" should "return 201 when the request body can be parsed and passed to the repository successfully" in {

      when(repo.upsert(any())(using any())).thenReturn(Future.successful(()))
      AuthUtils.authStub(authConnector)
      val result = call(controller.add, FakeRequest("POST", "/").withJsonBody(Json.toJson(invitation1)))
      status(result) mustEqual CREATED
    }

    it should "return 400 when the request body cannot be parsed" in {

      when(repo.upsert(any())(using any())).thenReturn(Future.successful(()))
      AuthUtils.authStub(authConnector)
      recoverToExceptionIf[BadRequestException](
        call(controller.add, FakeRequest().withRawBody(ByteString(RandomUtils.nextBytes(512001))))).map {
        ex =>
          ex.responseCode mustBe BAD_REQUEST
          ex.message mustBe "Bad Request with no request body returned for PSA Invitation"
      }
    }

    it should "return 400 when the request body cannot be parsed as valid json" in {

      when(repo.upsert(any())(using any())).thenReturn(Future.successful(()))
      AuthUtils.authStub(authConnector)
      val badJson = Json.obj(
        "abc" -> "def"
      )

      recoverToExceptionIf[BadRequestException](
        call(controller.add, FakeRequest().withJsonBody(badJson))).map {
        ex =>
          ex.responseCode mustBe BAD_REQUEST
          ex.message mustBe "not valid value for PSA Invitation"
      }
    }


    it should "throw a MongoDBFailedException when the mongodb insert call is failed with DatabaseException" in {
      when(repo.upsert(any())(using any())).thenReturn(Future.failed(new MongoException(0, "mongo error")))
      AuthUtils.authStub(authConnector)

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
      when(repo.getByKeys(eqTo(map))(using any())).thenReturn(Future.successful(Some(invitationList)))
      AuthUtils.authStub(authConnector)
      val result = testMethod()(FakeRequest().withHeaders(map.toSeq *))
      status(result).mustBe(OK)
      contentAsString(result) mustEqual Json.toJson(invitationList).toString
    }

    it should "return 404 when the data doesn't exist" in {
      when(repo.getByKeys(eqTo(map))(using any())).thenReturn(Future.successful {
        None
      })
      AuthUtils.authStub(authConnector)
      val result = testMethod()(FakeRequest().withHeaders(map.toSeq *))
      status(result) mustEqual NOT_FOUND
    }

    it should "throw an exception when the repository call fails" in {
      when(repo.getByKeys(eqTo(map))(using any())).thenReturn(Future.failed {
        new Exception()
      })
      AuthUtils.authStub(authConnector)
      val result = testMethod()(FakeRequest().withHeaders(map.toSeq *))
      an[Exception] must be thrownBy status(result)
    }
  }

  private def validCacheControllerWithRemove(s: String): Unit = {
    s"$s" should "return 200 when the data is removed successfully" in {
      when(repo.remove(eqTo(mapBothKeys))(using any())).thenReturn(Future.successful(true))
      AuthUtils.authStub(authConnector)

      val result = controller.remove()(FakeRequest().withHeaders(mapBothKeys.toSeq *))

      status(result) mustEqual OK
    }
  }

  "InvitationsCacheController" should behave like validCacheControllerWithInsert()
  it should behave like validCacheControllerWithGet("get", mapBothKeys, () => controller.get)
  it should behave like validCacheControllerWithGet("getForScheme", mapPstr, () => controller.getForScheme)
  it should behave like validCacheControllerWithGet("getForInvitee", mapInviteePsaId, () => controller.getForInvitee)
  it should behave like validCacheControllerWithRemove("remove")
}
