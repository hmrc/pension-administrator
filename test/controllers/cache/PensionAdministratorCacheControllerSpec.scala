/*
 * Copyright 2024 HM Revenue & Customs
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

import base.SpecBase
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import repositories._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.UnauthorizedException
import utils.{AuthUtils, RandomUtils}

import scala.concurrent.Future

class PensionAdministratorCacheControllerSpec
  extends SpecBase
    with Matchers
    with MockitoSugar
    with Injecting {

  implicit lazy val mat: Materializer = app.materializer

  private val repo = mock[ManageCacheRepository]
  private val authConnector: AuthConnector = mock[AuthConnector]

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
      bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
      bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
      bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
      bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository]),
      bind[AdminDataRepository].toInstance(mock[AdminDataRepository]),
      bind[AuthConnector].toInstance(authConnector),
      bind[ManageCacheRepository].toInstance(repo)
    )

  def controller: PensionAdministratorCacheController = app.injector.instanceOf[PensionAdministratorCacheControllerImpl]


  // scalastyle:off method.length
  "PensionAdministratorCacheController" must {
    s".get" must {
      "return 200 and the relevant data when it exists" in {
        when(repo.get(eqTo("foo"))(any())) thenReturn Future.successful {
          Some(Json.obj())
        }
        AuthUtils.noEnrolmentAuthStub(authConnector)

        val result = controller.get("foo")(FakeRequest())

        status(result) mustEqual OK
        contentAsString(result) mustEqual "{}"
      }

      "return 404 when the data doesn't exist" in {
        when(repo.get(eqTo("foo"))(any())) thenReturn Future.successful {
          None
        }
        AuthUtils.noEnrolmentAuthStub(authConnector)

        val result = controller.get("foo")(FakeRequest())

        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(repo.get(eqTo("foo"))(any())) thenReturn Future.failed {
          new Exception()
        }
        AuthUtils.noEnrolmentAuthStub(authConnector)

        val result = controller.get("foo")(FakeRequest())

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller.get("foo")(FakeRequest())

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }

    s".save" must {

      "return 200 when the request body can be parsed and passed to the repository successfully" in {

        when(repo.upsert(any(), any())(any())) thenReturn Future.successful(())
        AuthUtils.noEnrolmentAuthStub(authConnector)

        val result = call(controller.save("foo"), FakeRequest("POST", "/").withJsonBody(Json.obj("abc" -> "def")))

        status(result) mustEqual OK
      }

      "return 413 when the request body cannot be parsed" in {
        when(repo.upsert(any(), any())(any())) thenReturn Future.successful(())
        AuthUtils.noEnrolmentAuthStub(authConnector)

        val result = call(controller.save("foo"), FakeRequest().withRawBody(ByteString(RandomUtils.nextBytes(512001))))

        status(result) mustEqual REQUEST_ENTITY_TOO_LARGE
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = call(controller.save("foo"), FakeRequest().withRawBody(ByteString("foo")))

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }

    s".remove" must {
      "return 200 when the data is removed successfully" in {
        when(repo.remove(eqTo("foo"))(any())) thenReturn Future.successful(true)
        AuthUtils.noEnrolmentAuthStub(authConnector)

        val result = controller.remove("foo")(FakeRequest())

        status(result) mustEqual OK
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller.remove("foo")(FakeRequest())

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }
  }

  "PensionAdministratorCacheController self" must {
    s".getSelf" must {
      "return 200 and the relevant data when it exists" in {
        when(repo.get(eqTo(AuthUtils.externalId))(any())) thenReturn Future.successful {
          Some(Json.obj())
        }
        AuthUtils.noEnrolmentAuthStub(authConnector)

        val result = controller.getSelf(FakeRequest())

        status(result) mustEqual OK
        contentAsString(result) mustEqual "{}"
      }

      "return 404 when the data doesn't exist" in {
        when(repo.get(eqTo(AuthUtils.externalId))(any())) thenReturn Future.successful {
          None
        }
        AuthUtils.noEnrolmentAuthStub(authConnector)

        val result = controller.getSelf(FakeRequest())

        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(repo.get(eqTo(AuthUtils.externalId))(any())) thenReturn Future.failed {
          new Exception()
        }
        AuthUtils.noEnrolmentAuthStub(authConnector)

        val result = controller.getSelf(FakeRequest())

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller.getSelf(FakeRequest())

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }

    s".saveSelf" must {

      "return 200 when the request body can be parsed and passed to the repository successfully" in {

        when(repo.upsert(any(), any())(any())) thenReturn Future.successful(())
        AuthUtils.noEnrolmentAuthStub(authConnector)

        val result = call(controller.saveSelf, FakeRequest("POST", "/").withJsonBody(Json.obj("abc" -> "def")))

        status(result) mustEqual OK
      }

      "return 413 when the request body cannot be parsed" in {
        when(repo.upsert(any(), any())(any())) thenReturn Future.successful(())
        AuthUtils.noEnrolmentAuthStub(authConnector)

        val result = call(controller.saveSelf, FakeRequest().withRawBody(ByteString(RandomUtils.nextBytes(512001))))

        status(result) mustEqual REQUEST_ENTITY_TOO_LARGE
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = call(controller.saveSelf, FakeRequest().withRawBody(ByteString("foo")))

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }

    s".removeSelf" must {
      "return 200 when the data is removed successfully" in {
        when(repo.remove(eqTo(AuthUtils.externalId))(any())) thenReturn Future.successful(true)
        AuthUtils.noEnrolmentAuthStub(authConnector)

        val result = controller.removeSelf(FakeRequest())

        status(result) mustEqual OK
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller.removeSelf(FakeRequest())

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }
  }
}
