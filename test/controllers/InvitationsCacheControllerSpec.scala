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
import models.{IndividualDetails, PSAMinimalDetails}
import org.apache.commons.lang3.RandomUtils
import org.joda.time.DateTime
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncFlatSpec, MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.InvitationsCacheRepository
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{BadRequestException, UnauthorizedException}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class InvitationsCacheControllerSpec extends AsyncFlatSpec with MustMatchers with MockitoSugar{

  implicit lazy val mat: Materializer = new GuiceApplicationBuilder().configure("run.mode" -> "Test").build().materializer

  private def configuration(encrypted: Boolean = true) = Configuration(
    "mongodb.pension-administrator-cache.maxSize" -> 512000,
    "encrypted" -> encrypted
  )

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

    s".insert $msg" should "return 200 when the request body can be parsed and passed to the repository successfully" in {

        when(repo.insert(any(), any(), any())(any())).thenReturn(Future.successful(true))
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))
        val johnDoe = Json.parse(
          """
            |{
            |	"processingDate": "2009-12-17T09:30:47Z",
            |	"psaMinimalDetails": {
            |		"organisationOrPartnershipName": "test org name"
            |	},
            |	"email": "test@email.com",
            |	"psaSuspensionFlag": true
            |}
            |
          """.stripMargin
        )

        val result = call(controller(repo, authConnector, encrypted).add, FakeRequest("POST", "/").withHeaders(("inviteePsaId"->""),("pstr","")).withJsonBody(johnDoe))

        status(result) mustEqual OK
      }

      it should "return 400 when the request dont have valid headers" in {

        when(repo.insert(any(), any(), any())(any())).thenReturn(Future.successful(true))
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))
        val johnDoe = PSAMinimalDetails("john.doe@email.com", false, None, Some(IndividualDetails("John", None, "Doe")))

        recoverToExceptionIf[BadRequestException](call(controller(repo, authConnector, encrypted).add, FakeRequest("POST", "/").withJsonBody(Json.toJson(johnDoe)))).map {
          ex =>
            ex.getMessage mustBe "inviteePsaId  & pstr values missing in request header"
        }
      }

    it should "return 413 when the request body cannot be parsed" in {
        when(repo.insert(any(), any(), any())(any())).thenReturn(Future.successful(true))
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = call(controller(repo, authConnector, encrypted).add, FakeRequest().withRawBody(ByteString(RandomUtils.nextBytes(512001))))

        status(result) mustEqual REQUEST_ENTITY_TOO_LARGE
      }

    it should "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.failed {
          new UnauthorizedException("")
        })

        val result = call(controller(repo, authConnector, encrypted).add, FakeRequest().withRawBody(ByteString("foo")))

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }

    s".lastUpdated $msg" should "return 200 and the relevant data when it exists" in {
        val date = DateTime.now
        when(repo.getLastUpdated(eqTo("foo"))(any())) thenReturn Future.successful {
          Some(date)
        }
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(repo, authConnector, encrypted).lastUpdated("foo")(FakeRequest())

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(date.getMillis)
      }

      it should "return 404 when the data doesn't exist" in {
        when(repo.getLastUpdated(eqTo("foo"))(any())) thenReturn Future.successful {
          None
        }
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(repo, authConnector, encrypted).lastUpdated("foo")(FakeRequest())

        status(result) mustEqual NOT_FOUND
      }

      it should "throw an exception when the repository call fails" in {
        when(repo.getLastUpdated(eqTo("foo"))(any())) thenReturn Future.failed {
          new Exception()
        }
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(repo, authConnector, encrypted).lastUpdated("foo")(FakeRequest())

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      it should "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller(repo, authConnector, encrypted).lastUpdated("foo")(FakeRequest())

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
  }
  // scalastyle:on method.length

    "InvitationsCacheController" should behave like validCacheController(encrypted = false)
    "InvitationsCacheController" should behave like validCacheController(encrypted = true)
}
