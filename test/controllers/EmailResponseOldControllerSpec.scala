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

package controllers

import audit.{AuditService, EmailAuditEvent, StubSuccessfulAuditService}
import base.SpecBase
import models.*
import models.enumeration.JourneyType
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.*
import repositories.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.domain.PsaId
import utils.AuthUtils

import java.time.Instant

class EmailResponseOldControllerSpec extends SpecBase with MockitoSugar {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val psa: PsaId = PsaId("A7654321")
  val fakeAuditService = new StubSuccessfulAuditService()
  val validJson: JsObject = Json.obj("name" -> "value")
  val emailEvents: EmailEvents =
    EmailEvents(Seq(
      EmailEvent(Sent, Instant.now()),
      EmailEvent(Delivered, Instant.now()),
      EmailEvent(Opened, Instant.now())
    ))
  val crypto: ApplicationCrypto = app.injector.instanceOf[ApplicationCrypto]
  val controller: EmailResponseOldController = app.injector.instanceOf[EmailResponseOldController]


  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
      bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
      bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
      bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
      bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository]),
      bind[AuditService].to(fakeAuditService)
    )

  private val authConnector: AuthConnector = mock[AuthConnector]

  "EmailResponseController retrieveStatus" must {

    "respond OK when given EmailEvents" which {

      JourneyType.values.foreach { eventType =>
        s"will send events excluding Opened for ${eventType.toString} to audit service" in {
          AuthUtils.authStub(authConnector)

          val encrypted = crypto.QueryParameterCrypto.encrypt(PlainText(psa.id)).value

          val result = controller.retrieveStatus(eventType, encrypted)(fakeRequest.withBody(Json.toJson(emailEvents)))

          status(result).mustBe(OK)
          fakeAuditService.verifySent(EmailAuditEvent(psa, Sent, eventType)).mustBe(true)
          fakeAuditService.verifySent(EmailAuditEvent(psa, Delivered, eventType)).mustBe(true)
          fakeAuditService.verifySent(EmailAuditEvent(psa, Opened, eventType)).mustBe(false)

        }
      }
    }
  }

  "respond with BAD_REQUEST when not given EmailEvents" in {
    fakeAuditService.reset()
    AuthUtils.authStub(authConnector)

    val encrypted = crypto.QueryParameterCrypto.encrypt(PlainText(psa.id)).value

    val result = controller.retrieveStatus(JourneyType.PSA, encrypted)(fakeRequest.withBody(validJson))

    status(result).mustBe(BAD_REQUEST)
    fakeAuditService.verifyNothingSent().mustBe(true)
  }

  "respond with FORBIDDEN" when {
    "URL contains an id does not match PSAID pattern" in {

      fakeAuditService.reset()
      AuthUtils.authStub(authConnector)

      val psa = crypto.QueryParameterCrypto.encrypt(PlainText("psa")).value

      val result = controller.retrieveStatus(JourneyType.PSA, psa)(fakeRequest.withBody(Json.toJson(emailEvents)))

      status(result).mustBe(FORBIDDEN)
      contentAsString(result).mustBe("Malformed PSAID")
      fakeAuditService.verifyNothingSent().mustBe(true)

    }

    "URL contains an invalid PSAID id - handling SecurityException" in {

      fakeAuditService.reset()

      val psa = "manipulatedPSAID"
      AuthUtils.authStub(authConnector)

      val controller = app.injector.instanceOf[EmailResponseController]

      val result = controller.retrieveStatus(JourneyType.PSA, psa)(fakeRequest.withBody(Json.toJson(emailEvents)))

      status(result).mustBe(FORBIDDEN)
      contentAsString(result).mustBe("Malformed PSAID")
      fakeAuditService.verifyNothingSent().mustBe(true)

    }
  }
}
