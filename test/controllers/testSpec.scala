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

import audit.{AuditService, StubSuccessfulAuditService}
import models.{Delivered, EmailEvent, EmailEvents, Opened, Sent}
import org.scalatest.{EitherValues, OptionValues, RecoverMethods}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceableModule
import repositories.{InvitationsCacheRepository, ManagePensionsDataCacheRepository, MinimalDetailsCacheRepository, PSADataCacheRepository, SessionDataCacheRepository}
import utils.WireMockHelper
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.{BodyWritable, WSClient, WSResponse}
import uk.gov.hmrc.domain.PsaId
import play.api.libs.ws.JsonBodyWritables.*

import java.time.Instant
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable}
import scala.language.postfixOps

class testSpec extends PlaySpec
  with WireMockHelper
  with MockitoSugar
  with OptionValues
  with RecoverMethods
  with EitherValues {

  protected def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  val psa: PsaId = PsaId("A7654321")

  val emailEvents: EmailEvents = EmailEvents(Seq(EmailEvent(Sent, Instant.now()), EmailEvent(Delivered, Instant.now()), EmailEvent(Opened, Instant.now())))

  val fakeAuditService = new StubSuccessfulAuditService()

  val validJson: JsObject = Json.obj("name" -> "value")

  override protected def portConfigKeys: String = "microservice.services.pensions-scheme.port"

  protected def post[T](url: String,
                        body: T,
                        headers: Seq[(String, String)] = Seq())
                       (using wsClient: WSClient, bodyWritable: BodyWritable[T]): WSResponse = {

    val headersWithNoCheck = headers ++ Seq("Csrf-Token" -> "nocheck")
    await(wsClient.url(url).withFollowRedirects(false).withHttpHeaders(headersWithNoCheck *).post(body))
  }

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
      bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
      bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
      bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
      bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository]),
      bind[EmailResponseController].toSelf.eagerly(),
      bind[AuditService].to(fakeAuditService)
    )


//  private val authConnector: AuthConnector = mock[AuthConnector]

  "EmailResponseController POST to /email-response/PSA/A7654321" should {
    "do some shit" in {
      val result = await(buildClient("/pension-administrator/email-response/PSA/A7654321").post(validJson))
      result.status mustBe 200
    }
  }
}
