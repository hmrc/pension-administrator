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

import audit.{AuditService, StubSuccessfulAuditService}
import models.*
import org.scalatest.OptionValues
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.{BodyWritable, WSClient, WSResponse}
import play.api.test.Helpers.*
import repositories.*
import utils.WireMockHelper

import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.duration.*
import scala.language.postfixOps

class JourneyTypeIntegrationSpec
  extends PlaySpec
    with GuiceOneServerPerSuite
    with WireMockHelper
    with MockitoSugar
    with OptionValues {

  server.start()
  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.pensions-scheme.port" -> wireMockPort)
    .build()

  override def portConfigKeys: String = "microservice.services.pensions-scheme.port"

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
      bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
      bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
      bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
      bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository]),
      bind[AuditService].toInstance(new StubSuccessfulAuditService())
    )

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val emailEvents: EmailEvents =
    EmailEvents(Seq(EmailEvent(Sent, Instant.now()), EmailEvent(Delivered, Instant.now()), EmailEvent(Opened, Instant.now())))

  val psaId = "A7654321"

  val validJson: JsValue = Json.toJson(
    emailEvents
  )

  def post[T](url: String, body: T)(using ws: WSClient, wr: BodyWritable[T]): WSResponse =
    Await.result(
      ws.url(url)
        .withFollowRedirects(false)
        .withHttpHeaders("Csrf-Token" -> "nocheck", "Content-Type" -> "application/json")
        .post(body),
      10.seconds
    )

  "any test name" should {

    "return 200 for valid journeyType and ID" in {
      val result = post(s"http://localhost:$port/pension-administrator/email-response/PSA/$psaId", validJson)(using wsClient)
      result.status.mustBe(OK)
    }

    "return 404 for invalid journeyType not handled by binder" in {
      val result = post(s"http://localhost:$port/pension-administrator/email-response/invalidJourneyType/$psaId", validJson)(using wsClient)
      result.status.mustBe(NOT_FOUND)
    }
  }
}