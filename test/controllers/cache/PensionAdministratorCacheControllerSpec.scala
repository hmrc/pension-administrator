/*
 * Copyright 2021 HM Revenue & Customs
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

import akka.stream.Materializer
import akka.util.ByteString
import org.apache.commons.lang3.RandomUtils
import org.joda.time.DateTime
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.JodaWrites._
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import repositories.ManageCacheRepository
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.UnauthorizedException

import scala.concurrent.Future

class PensionAdministratorCacheControllerSpec
  extends WordSpec
    with MustMatchers
    with MockitoSugar
    with Injecting
    with GuiceOneAppPerSuite {

  implicit lazy val mat: Materializer = app.materializer

  private val repo = mock[ManageCacheRepository]
  private val authConnector: AuthConnector = mock[AuthConnector]

  private class PensionAdministratorCacheControllerImpl(
                                                         repo: ManageCacheRepository,
                                                         authConnector: AuthConnector
                                                       )
    extends PensionAdministratorCacheController(
      repository = repo,
      authConnector = authConnector,
      cc = inject[ControllerComponents]
    )

  def controller(repo: ManageCacheRepository, authConnector: AuthConnector): PensionAdministratorCacheController = {
    new PensionAdministratorCacheControllerImpl(repo, authConnector)
  }

  // scalastyle:off method.length
  "PensionAdministratorCacheController" must {
    s".get" must {
      "return 200 and the relevant data when it exists" in {
        when(repo.get(eqTo("foo"))(any())) thenReturn Future.successful {
          Some(Json.obj())
        }
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(repo, authConnector).get("foo")(FakeRequest())

        status(result) mustEqual OK
        contentAsString(result) mustEqual "{}"
      }

      "return 404 when the data doesn't exist" in {
        when(repo.get(eqTo("foo"))(any())) thenReturn Future.successful {
          None
        }
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(repo, authConnector).get("foo")(FakeRequest())

        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(repo.get(eqTo("foo"))(any())) thenReturn Future.failed {
          new Exception()
        }
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(repo, authConnector).get("foo")(FakeRequest())

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller(repo, authConnector).get("foo")(FakeRequest())

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }

    s".save" must {

      "return 200 when the request body can be parsed and passed to the repository successfully" in {

        when(repo.upsert(any(), any())(any())) thenReturn Future.successful(true)
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = call(controller(repo, authConnector).save("foo"), FakeRequest("POST", "/").withJsonBody(Json.obj("abc" -> "def")))

        status(result) mustEqual OK
      }

      "return 413 when the request body cannot be parsed" in {
        when(repo.upsert(any(), any())(any())) thenReturn Future.successful(true)
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = call(controller(repo, authConnector).save("foo"), FakeRequest().withRawBody(ByteString(RandomUtils.nextBytes(512001))))

        status(result) mustEqual REQUEST_ENTITY_TOO_LARGE
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = call(controller(repo, authConnector).save("foo"), FakeRequest().withRawBody(ByteString("foo")))

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }

    s".remove" must {
      "return 200 when the data is removed successfully" in {
        when(repo.remove(eqTo("foo"))(any())) thenReturn Future.successful(true)
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(repo, authConnector).remove("foo")(FakeRequest())

        status(result) mustEqual OK
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller(repo, authConnector).remove("foo")(FakeRequest())

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }

    s".lastUpdated" must {

      "return 200 and the relevant data when it exists" in {
        val date = DateTime.now
        when(repo.getLastUpdated(eqTo("foo"))(any())) thenReturn Future.successful {
          Some(date)
        }
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(repo, authConnector).lastUpdated("foo")(FakeRequest())

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(date)
      }

      "return 404 when the data doesn't exist" in {
        when(repo.getLastUpdated(eqTo("foo"))(any())) thenReturn Future.successful {
          None
        }
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(repo, authConnector).lastUpdated("foo")(FakeRequest())

        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(repo.getLastUpdated(eqTo("foo"))(any())) thenReturn Future.failed {
          new Exception()
        }
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(repo, authConnector).lastUpdated("foo")(FakeRequest())

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller(repo, authConnector).lastUpdated("foo")(FakeRequest())

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }
  }
}
