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

  def controller(repo: InvitationsCacheRepository, authConnector: AuthConnector, encrypted: Boolean): InvitationsCacheController = {
    new InvitationsCacheControllerImpl(repo, authConnector, encrypted)
  }

  // scalastyle:off method.length
  def validCacheController(encrypted: Boolean, ): Unit = {
    val msg = if (encrypted) "where encrypted" else "where not encrypted"
    s".get $msg" must {

      "return 200 and the relevant data when it exists" in {
        when(repo.getByKeys(eqTo(mapBothKeys))(any())) thenReturn Future.successful(Some(invitationJson))
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(repo, authConnector, encrypted).get()(FakeRequest().withHeaders(mapBothKeys.toSeq: _*))

        status(result) mustEqual OK
        contentAsString(result) mustEqual invitationJson.toString
      }

      "return 404 when the data doesn't exist" in {
        when(repo.getByKeys(eqTo(mapBothKeys))(any())) thenReturn Future.successful {
          None
        }
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(repo, authConnector, encrypted).get()(FakeRequest().withHeaders(mapBothKeys.toSeq: _*))

        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(repo.getByKeys(eqTo(mapBothKeys))(any())) thenReturn Future.failed {
          new Exception()
        }
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(repo, authConnector, encrypted).get()(FakeRequest().withHeaders(mapBothKeys.toSeq: _*))

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller(repo, authConnector, encrypted).get()(FakeRequest().withHeaders(mapBothKeys.toSeq: _*))

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }
  }



//    s".remove $msg" must {
//      "return 200 when the data is removed successfully" in {
//        when(repo.remove(eqTo("foo"))(any())) thenReturn Future.successful(true)
//        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())
//
//        val result = controller(repo, authConnector, encrypted).remove("foo")(FakeRequest())
//
//        status(result) mustEqual OK
//      }
//
//      "throw an exception when the call is not authorised" in {
//        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
//          new UnauthorizedException("")
//        }
//
//        val result = controller(repo, authConnector, encrypted).remove("foo")(FakeRequest())
//
//        an[UnauthorizedException] must be thrownBy {
//          status(result)
//        }
//      }
//    }


  // scalastyle:on method.length

  "InvitationsCacheController" must {
    behave like validCacheController(encrypted = false)
    behave like validCacheController(encrypted = true)

  }
}

object InvitationsCacheControllerSpec {

  private val pstr = "S12345"
  private val schemeName = "Test scheme name"
  private val inviterPsaId = "I12345"
  private val inviteePsaId = "P12345"
  private val inviteeName = "Test Invitee Name"
  private val mapBothKeys = Map("pstr" -> pstr, "inviteePsaId" -> inviteePsaId)
  private val mapPstr = Map("pstr" -> pstr)
  private val mapInviteePsaId = Map("inviteePsaId" -> inviteePsaId)

  private val invitation = Invitation(pstr = pstr, schemeName = schemeName, inviterPsaId = inviterPsaId, inviteePsaId = inviteePsaId, inviteeName = inviteeName)

  private val invitationJson =  Json.toJson(invitation)

}
