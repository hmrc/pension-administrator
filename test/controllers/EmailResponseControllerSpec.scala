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

package controllers

import audit.{AuditService, EmailAuditEvent, StubSuccessfulAuditService}
import base.SpecBase
import models._
import models.enumeration.JourneyType
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories._
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.domain.PsaId

import java.time.Instant

class EmailResponseControllerSpec extends SpecBase with MockitoSugar {

  import EmailResponseControllerSpec._

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
      bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
      bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
      bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
      bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository]),
      bind[AdminDataRepository].toInstance(mock[AdminDataRepository]),
      bind[AuditService].to(fakeAuditService)
    )

  "EmailResponseController" must {

    "respond OK when given EmailEvents" which {

      JourneyType.values.foreach { eventType =>
        s"will send events excluding Opened for ${eventType.toString} to audit service" in {

          val encrypted = app.injector.instanceOf[ApplicationCrypto].QueryParameterCrypto.encrypt(PlainText(psa.id)).value

          val controller = app.injector.instanceOf[EmailResponseController]

          val result = controller.retrieveStatus(eventType, encrypted)(fakeRequest.withBody(Json.toJson(emailEvents)))

          status(result) mustBe OK
          fakeAuditService.verifySent(EmailAuditEvent(psa, Sent, eventType)) mustBe true
          fakeAuditService.verifySent(EmailAuditEvent(psa, Delivered, eventType)) mustBe true
          fakeAuditService.verifySent(EmailAuditEvent(psa, Opened, eventType)) mustBe false

        }
      }
    }
  }

  "respond with BAD_REQUEST when not given EmailEvents" in {
    fakeAuditService.reset()

    val encrypted = app.injector.instanceOf[ApplicationCrypto].QueryParameterCrypto.encrypt(PlainText(psa.id)).value

    val controller = app.injector.instanceOf[EmailResponseController]

    val result = controller.retrieveStatus(JourneyType.PSA, encrypted)(fakeRequest.withBody(validJson))

    status(result) mustBe BAD_REQUEST
    fakeAuditService.verifyNothingSent() mustBe true
  }

  "respond with FORBIDDEN" when {
    "URL contains an id does not match PSAID pattern" in {

      fakeAuditService.reset()

      val psa = app.injector.instanceOf[ApplicationCrypto].QueryParameterCrypto.encrypt(PlainText("psa")).value

      val controller = app.injector.instanceOf[EmailResponseController]

      val result = controller.retrieveStatus(JourneyType.PSA, psa)(fakeRequest.withBody(Json.toJson(emailEvents)))

      status(result) mustBe FORBIDDEN
      contentAsString(result) mustBe "Malformed PSAID"
      fakeAuditService.verifyNothingSent() mustBe true

    }

    "URL contains an invalid PSAID id - handling SecurityException" in {

      fakeAuditService.reset()

      val psa = "manipulatedPSAID"

      val controller = app.injector.instanceOf[EmailResponseController]

      val result = controller.retrieveStatus(JourneyType.PSA, psa)(fakeRequest.withBody(Json.toJson(emailEvents)))

      status(result) mustBe FORBIDDEN
      contentAsString(result) mustBe "Malformed PSAID"
      fakeAuditService.verifyNothingSent() mustBe true

    }
  }
}

object EmailResponseControllerSpec {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val psa = PsaId("A7654321")

  val emailEvents = EmailEvents(Seq(EmailEvent(Sent, Instant.now()), EmailEvent(Delivered, Instant.now()), EmailEvent(Opened, Instant.now())))

  val fakeAuditService = new StubSuccessfulAuditService()

  val validJson = Json.obj("name" -> "value")

}
