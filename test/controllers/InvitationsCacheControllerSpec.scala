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
import models.Invitation
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.InvitationsCacheRepository
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.UnauthorizedException

import scala.concurrent.Future
//syncflatspec
class InvitationsCacheControllerSpec extends WordSpec with MustMatchers with MockitoSugar with OneAppPerSuite {

  implicit lazy val mat: Materializer = app.materializer

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

  import InvitationsCacheControllerSpec._

  private def msg(encrypted: Boolean) = if (encrypted) "where encrypted" else "where not encrypted"

  def controller(repo: InvitationsCacheRepository, authConnector: AuthConnector, encrypted: Boolean): InvitationsCacheController = {
    new InvitationsCacheControllerImpl(repo, authConnector, encrypted)
  }
  // scalastyle:off method.length
  def validCacheControllerWithGet(encrypted: Boolean, map: Map[String, String], testMethod: () => Action[AnyContent]): Unit = {
    s"work for request with headers: $map ${msg(encrypted)}" must {

      "return 200 and the relevant data when it exists" in {
        when(repo.getByKeys(eqTo(map))(any())) thenReturn Future.successful(Some(invitationJson))
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = testMethod()(FakeRequest().withHeaders(map.toSeq: _*))

        status(result) mustEqual OK
        contentAsString(result) mustEqual invitationJson.toString
      }

      "return 404 when the data doesn't exist" in {
        when(repo.getByKeys(eqTo(map))(any())) thenReturn Future.successful {
          None
        }
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = testMethod()(FakeRequest().withHeaders(map.toSeq: _*))

        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(repo.getByKeys(eqTo(map))(any())) thenReturn Future.failed {
          new Exception()
        }
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = testMethod()(FakeRequest().withHeaders(map.toSeq: _*))

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = testMethod()(FakeRequest().withHeaders(map.toSeq: _*))

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }
  }


def validCacheControllerWithRemove(encrypted: Boolean): Unit = {

  s".remove ${msg(encrypted)}" must {
    "return 200 when the data is removed successfully" in {
      when(repo.remove(eqTo(mapBothKeys))(any())) thenReturn Future.successful(true)
      when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

      val result = controller(repo, authConnector, encrypted).remove()(FakeRequest().withHeaders(mapBothKeys.toSeq: _*))

      status(result) mustEqual OK
    }

    "throw an exception when the call is not authorised" in {
      when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
        new UnauthorizedException("")
      }

      val result = controller(repo, authConnector, encrypted).remove()(FakeRequest().withHeaders(mapBothKeys.toSeq: _*))

      an[UnauthorizedException] must be thrownBy {
        status(result)
      }
    }
  }
}


  // scalastyle:on method.length

  "InvitationsCacheController" must {
    behave like validCacheControllerWithGet(encrypted = false, mapBothKeys, controller(repo, authConnector, encrypted = false).get _ )
    behave like validCacheControllerWithGet(encrypted = true, mapBothKeys, controller(repo, authConnector, encrypted = true).get _ )
    behave like validCacheControllerWithGet(encrypted = false, mapPstr, controller(repo, authConnector, encrypted = false).getForScheme _ )
    behave like validCacheControllerWithGet(encrypted = true, mapPstr, controller(repo, authConnector, encrypted = true).getForScheme _ )
    behave like validCacheControllerWithGet(encrypted = false, mapInviteePsaId, controller(repo, authConnector, encrypted = false).getForInvitee _ )
    behave like validCacheControllerWithGet(encrypted = true, mapInviteePsaId, controller(repo, authConnector, encrypted = true).getForInvitee _ )

    behave like validCacheControllerWithRemove(encrypted = false)
    behave like validCacheControllerWithRemove(encrypted = true)



  }
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

  private val invitationJson =  Json.toJson(List(invitation1, invitation2))

}
