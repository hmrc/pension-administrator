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

import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.{eq as eqTo, *}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.UnauthorizedException
import utils.{AuthUtils, RandomUtils}

import scala.concurrent.Future

class PSADataCacheControllerSpec extends AsyncWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  private val repo = mock[PSADataCacheRepository]
  private val authConnector: AuthConnector = mock[AuthConnector]

  private val app = new GuiceApplicationBuilder().configure("run.mode" -> "Test").overrides(Seq(
    bind[AuthConnector].toInstance(authConnector),
    bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
    bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
    bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
    bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository]),
    bind[PSADataCacheRepository].toInstance(repo)
  )).build()

  implicit lazy val mat: Materializer = app.materializer

  def controller: PSADataCacheController = app.injector.instanceOf[PSADataCacheController]
  private val pstr = "pstr"
  private val psaId = AuthUtils.psaId
  private val fakeRequest = FakeRequest().withHeaders("pstr" -> pstr, "psaId" -> psaId)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(authConnector)
    AuthUtils.noEnrolmentAuthStub(authConnector)
  }

  "PSADataCacheController" when {
    s"on .get " must {

      "return 200 and the relevant data when it exists" in {
        when(repo.get(eqTo("testId"))(using any())).thenReturn(Future.successful(Some(Json.obj("testId" -> "foo"))))

        val result = controller.get("testId")(fakeRequest)

        status(result).mustBe(OK)
        contentAsJson(result).mustBe(Json.obj("testId" -> "foo"))
      }

      "return 404 when the data doesn't exist" in {
        when(repo.get(eqTo("testId"))(using any())).thenReturn(Future.successful(None))

        val result = controller.get("testId")(fakeRequest)

        status(result).mustBe(NOT_FOUND)
      }

      "throw an exception when the repository call fails" in {
        when(repo.get(eqTo("testId"))(using any())).thenReturn(Future.failed(new Exception()))

        val result = controller.get("foo")(FakeRequest())
        an[Exception].mustBe(thrownBy {
          status(result)
        })
      }

      "throw an exception when the call is not authorised" in {
        reset(authConnector)
        when(authConnector.authorise[Unit](any(), any())(using any(), any())).thenReturn(Future.failed(new UnauthorizedException("")))

        val result = controller.get("foo")(FakeRequest())
        an[Exception].mustBe(thrownBy {
          status(result)
        })
      }
    }

    s"on .save " must {

      "return 200 when the request body can be parsed and passed to the repository successfully" in {
        when(repo.upsert(any(), any())(using any())).thenReturn(Future.successful(()))

        val result = call(controller.save("testId"), FakeRequest("POST", "/").withJsonBody(Json.obj("abc" -> "def")))

        status(result).mustBe(OK)
      }

      "return REQUEST_ENTITY_TOO_LARGE when the request body cannot be parsed" in {

        val result = call(controller.save(id = "testId"),
          FakeRequest().withRawBody(ByteString(RandomUtils.nextBytes(512001))))

        status(result).mustBe(REQUEST_ENTITY_TOO_LARGE)
      }

    }

    s"on .remove " must {
      "return 200 when the data is removed successfully" in {
        when(repo.remove(eqTo("testId"))(using any())).thenReturn(Future.successful(true))

        val result = controller.remove(id = "testId")(FakeRequest())

        status(result).mustBe(OK)
      }


    }

    s"on .getSelf " must {

      "return 200 and the relevant data when it exists" in {
        when(repo.get(eqTo(AuthUtils.externalId))(using any())).thenReturn(Future.successful(Some(Json.obj("testId" -> "foo"))))

        val result = controller.getSelf(fakeRequest)

        status(result).mustBe(OK)
        contentAsJson(result).mustEqual(Json.obj("testId" -> "foo"))
      }

      "return 404 when the data doesn't exist" in {
        when(repo.get(eqTo(AuthUtils.externalId))(using any())).thenReturn(Future.successful(None))

        val result = controller.getSelf(fakeRequest)

        status(result).mustBe(NOT_FOUND)
      }

      "throw an exception when the repository call fails" in {
        when(repo.get(eqTo(AuthUtils.externalId))(using any())).thenReturn(Future.failed(new Exception()))

        val result = controller.getSelf(FakeRequest())
        an[Exception].mustBe(thrownBy {
          status(result)
        })
      }

      "throw an exception when the call is not authorised" in {
        reset(authConnector)
        when(authConnector.authorise[Unit](any(), any())(using any(), any())).thenReturn(Future.failed(new UnauthorizedException("")))

        val result = controller.getSelf(FakeRequest())
        an[Exception].mustBe(thrownBy {
          status(result)
        })
      }
    }

    s"on .saveSelf " must {

      "return 200 when the request body can be parsed and passed to the repository successfully" in {
        when(repo.upsert(any(), any())(using any())).thenReturn(Future.successful(()))

        val result = call(controller.saveSelf, FakeRequest("POST", "/").withJsonBody(Json.obj("abc" -> "def")))

        status(result).mustBe(OK)
      }

      "return REQUEST_ENTITY_TOO_LARGE when the request body cannot be parsed" in {

        val result = call(controller.saveSelf,
          FakeRequest().withRawBody(ByteString(RandomUtils.nextBytes(512001))))

        status(result).mustBe(REQUEST_ENTITY_TOO_LARGE)
      }

    }

    s"on .removeSelf " must {
      "return 200 when the data is removed successfully" in {
        when(repo.remove(eqTo(AuthUtils.externalId))(using any())).thenReturn(Future.successful(true))

        val result = controller.removeSelf(FakeRequest())

        status(result).mustBe(OK)
      }
    }
  }
}
