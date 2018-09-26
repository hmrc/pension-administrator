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
import controllers.InvitationsCacheControllerSpec._
import models.Invitation
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
import repositories.InvitationsCacheRepository
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.UnauthorizedException

import scala.concurrent.Future

class InvitationsCacheControllerSpec extends AsyncFlatSpec with MustMatchers with MockitoSugar {
  implicit lazy val mat: Materializer = new GuiceApplicationBuilder().configure("run.mode" -> "Test").build().materializer

  private def configuration = Configuration("mongodb.pension-administrator-cache.maxSize" -> 512000)

  private val repo = mock[InvitationsCacheRepository]
  private val authConnector: AuthConnector = mock[AuthConnector]

  private class InvitationsCacheControllerImpl(
                                                repo: InvitationsCacheRepository,
                                                authConnector: AuthConnector
                                              ) extends InvitationsCacheController(configuration, repo, authConnector)

  private def controller(repo: InvitationsCacheRepository, authConnector: AuthConnector): InvitationsCacheController = {
    new InvitationsCacheControllerImpl(repo, authConnector)
  }

  // scalastyle:off method.length
  private def validCacheControllerWithInsert(): Unit = {
    ".insert " should "return 200 when the request body can be parsed and passed to the repository successfully" in {

      when(repo.insert(any())(any())).thenReturn(Future.successful(true))
      when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))
      val invitation = Invitation("test-pstr", "test-scheme", "test-inviter-psa-id", "inviteePsaId", "inviteeName")

      val result = call(controller(repo, authConnector).add, FakeRequest("POST", "/").withJsonBody(Json.toJson(invitation)))

      status(result) mustEqual OK
    }

    it should "return 413 when the request body cannot be parsed" in {
      when(repo.insert(any())(any())).thenReturn(Future.successful(true))
      when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

      val result = call(controller(repo, authConnector).add, FakeRequest().withRawBody(ByteString(RandomUtils.nextBytes(512001))))

      status(result) mustEqual BAD_REQUEST
    }

    it should "throw an exception when the call is not authorised" in {
      when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.failed {
        new UnauthorizedException("")
      })

      val result = call(controller(repo, authConnector).add, FakeRequest().withRawBody(ByteString("foo")))

      an[UnauthorizedException] must be thrownBy {
        status(result)
      }
    }
  }



  // scalastyle:on method.length

  private def validCacheControllerWithGet(s: String, map: Map[String, String], testMethod: () => Action[AnyContent]): Unit = {
    s"$s should work for request with headers: $map" should "return 200 and the relevant data when it exists" in {
      when(repo.getByKeys(eqTo(map))(any())) thenReturn Future.successful(Some(invitationJson))
      when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())
      val result = testMethod()(FakeRequest().withHeaders(map.toSeq: _*))
      status(result) mustEqual OK
      contentAsString(result) mustEqual invitationJson.toString
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

      val result = controller(repo, authConnector).remove()(FakeRequest().withHeaders(mapBothKeys.toSeq: _*))

      status(result) mustEqual OK
    }

    it should "throw an exception when the call is not authorised" in {
      when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
        new UnauthorizedException("")
      }
      val result = controller(repo, authConnector).remove()(FakeRequest().withHeaders(mapBothKeys.toSeq: _*))
      an[UnauthorizedException] must be thrownBy status(result)
    }
  }

  "InvitationsCacheController" should behave like validCacheControllerWithInsert
  it should behave like validCacheControllerWithGet("get", mapBothKeys, controller(repo, authConnector).get _)
  it should behave like validCacheControllerWithGet("getForScheme", mapPstr, controller(repo, authConnector).getForScheme _)
  it should behave like validCacheControllerWithGet("getForInvitee", mapInviteePsaId, controller(repo, authConnector).getForInvitee _)
  it should behave like validCacheControllerWithRemove("remove")
}

object InvitationsCacheControllerSpec {

  private val pstr1 = "S12345"
  private val schemeName1 = "Test scheme1 name"
  private val inviterPsaId1 = "I12345"
  private val inviteePsaId1 = "P12345"
  private val inviteeName1 = "Test Invitee1 Name"

  private val pstr2 = "D1234"
  private val schemeName2 = "Test scheme2 name"
  private val inviterPsaId2 = "Q12345"
  private val inviteePsaId2 = "T12345"
  private val inviteeName2 = "Test Invitee2 Name"

  private val mapBothKeys = Map("pstr" -> pstr1, "inviteePsaId" -> inviteePsaId1)
  private val mapPstr = Map("pstr" -> pstr1)
  private val mapInviteePsaId = Map("inviteePsaId" -> inviteePsaId1)

  private val invitation1 =
    Invitation(pstr = pstr1, schemeName = schemeName1, inviterPsaId = inviterPsaId1, inviteePsaId = inviteePsaId1, inviteeName = inviteeName1)
  private val invitation2 =
    Invitation(pstr = pstr2, schemeName = schemeName2, inviterPsaId = inviterPsaId2, inviteePsaId = inviteePsaId2, inviteeName = inviteeName2)

  private val invitationJson = Json.toJson(List(invitation1, invitation2))
}
