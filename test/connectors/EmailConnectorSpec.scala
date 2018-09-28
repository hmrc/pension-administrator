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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import models.SendEmailRequest
import org.scalatest.{AsyncFlatSpec, MustMatchers}
import play.api.http.Status
import play.api.inject.Injector
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

class EmailConnectorSpec extends AsyncFlatSpec with MustMatchers with WireMockHelper {

  import EmailConnectorSpec._

  override protected def portConfigKey: String = "microservice.services.email.port"

  "EmailConnector" must "return EmailSent when an email request is accepted by the API" in {

    server.stubFor(
      post(urlEqualTo(url))
        .withRequestBody(equalToJson(emailJson))
        .willReturn(
          aResponse()
            .withStatus(Status.ACCEPTED)
        )
    )

    connector(injector).sendEmail(email) map {
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

    connector(injector).sendEmail(email) map {
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

    connector(injector).sendEmail(email) map {
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
