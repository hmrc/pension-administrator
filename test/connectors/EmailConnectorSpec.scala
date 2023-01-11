/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import models.SendEmailRequest
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject.guice.GuiceableModule
import play.api.inject.{Injector, bind}
import play.api.libs.json.Json
import repositories._
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

class EmailConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with MockitoSugar {

  import EmailConnectorSpec._

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
      bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
      bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
      bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
      bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository]),
      bind[AdminDataRepository].toInstance(mock[AdminDataRepository])
    )

  override protected def portConfigKeys: String = "microservice.services.email.port"

  "EmailConnector" must "return EmailSent when an email request is accepted by the API" in {

    server.stubFor(
      post(urlEqualTo(url))
        .withRequestBody(equalToJson(emailJson))
        .willReturn(
          aResponse()
            .withStatus(Status.ACCEPTED)
        )
    )

    connector(app.injector).sendEmail(email) map {
      response =>
        response mustBe EmailSent
    }

  }

  it must "return EmailNotSent when any other non-error response is returned bu the API" in {

    server.stubFor(
      post(urlEqualTo(url))
        .withRequestBody(equalToJson(emailJson))
        .willReturn(
          aResponse()
            .withStatus(Status.NO_CONTENT)
        )
    )

    connector(app.injector).sendEmail(email) map {
      response =>
        response mustBe EmailNotSent
    }

  }

  it must "return EmailNotSent when an error response is returned by the API" in {

    server.stubFor(
      post(urlEqualTo(url))
        .withRequestBody(equalToJson(emailJson))
        .willReturn(
          aResponse()
            .withStatus(Status.INTERNAL_SERVER_ERROR)
        )
    )

    connector(app.injector).sendEmail(email) map {
      response =>
        response mustBe EmailNotSent
    }

  }

}

object EmailConnectorSpec {

  val url: String = "/hmrc/email"

  val email: SendEmailRequest =
    SendEmailRequest(
      List("test@test.com"),
      "test-template-id",
      Map(
        "test-param1" -> "test-value1",
        "test-param2" -> "test-value2"
      ),
      Some("test-response-url")
    )

  val emailJson: String = Json.stringify(Json.toJson(email))

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def connector(injector: Injector): EmailConnector = injector.instanceOf[EmailConnectorImpl]

}
