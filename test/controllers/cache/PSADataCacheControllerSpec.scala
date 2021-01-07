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
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{AsyncWordSpec, MustMatchers}
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.PSADataCacheRepository
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.UnauthorizedException

import scala.concurrent.Future

class PSADataCacheControllerSpec extends AsyncWordSpec with MustMatchers with MockitoSugar {
  private val app = new GuiceApplicationBuilder().configure("run.mode" -> "Test").build()
  implicit lazy val mat: Materializer = app.materializer
  private val cc = app.injector.instanceOf[ControllerComponents]
  private val config = app.injector.instanceOf[Configuration]
  private val repo = mock[PSADataCacheRepository]
  private val authConnector: AuthConnector = mock[AuthConnector]

  def controller: PSADataCacheController = new PSADataCacheController(config, repo, authConnector, cc)

  "PSADataCacheController" when {
    s"on .get " must {

      "return 200 and the relevant data when it exists" in {
        when(repo.get(eqTo("testId"))(any())) thenReturn Future.successful(Some(Json.obj("testId" -> "foo")))
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller.get("testId")(FakeRequest())

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.obj("testId" -> "foo")
      }

      "return 404 when the data doesn't exist" in {
        when(repo.get(eqTo("testId"))(any())) thenReturn Future.successful(None)
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller.get("testId")(FakeRequest())

        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(repo.get(eqTo("testId"))(any())) thenReturn Future.failed(new Exception())
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller.get("foo")(FakeRequest())
        an[Exception] must be thrownBy status(result)
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed(new UnauthorizedException(""))

        val result = controller.get("foo")(FakeRequest())
        an[UnauthorizedException] must be thrownBy status(result)
      }
    }

    s"on .save " must {

      "return 200 when the request body can be parsed and passed to the repository successfully" in {
        when(repo.upsert(any(), any())(any())) thenReturn Future.successful(true)
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = call(controller.save("testId"), FakeRequest("POST", "/").withJsonBody(Json.obj("abc" -> "def")))

        status(result) mustEqual OK
      }

      "return REQUEST_ENTITY_TOO_LARGE when the request body cannot be parsed" in {
        when(repo.upsert(any(), any())(any())) thenReturn Future.successful(true)
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = call(controller.save(id = "testId"),
          FakeRequest().withRawBody(ByteString(RandomUtils.nextBytes(512001))))

        status(result) mustEqual REQUEST_ENTITY_TOO_LARGE
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed(new UnauthorizedException(""))

        val result = call(controller.save(id = "testId"), FakeRequest().withRawBody(ByteString("foo")))

        an[UnauthorizedException] must be thrownBy status(result)
      }
    }

    s"on .remove " must {
      "return 200 when the data is removed successfully" in {
        when(repo.remove(eqTo("testId"))(any())) thenReturn Future.successful(true)
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller.remove(id = "testId")(FakeRequest())

        status(result) mustEqual OK
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed(new UnauthorizedException(""))

        val result = controller.remove(id = "testId")(FakeRequest())

        an[UnauthorizedException] must be thrownBy status(result)
      }
    }
  }
}
