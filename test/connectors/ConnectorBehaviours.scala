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
import connectors.RegistrationConnectorSpec.errorResponse
import org.scalatest.{AsyncFlatSpec, EitherValues, Matchers}
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import utils.WireMockHelper

import scala.concurrent.Future

trait ConnectorBehaviours extends AsyncFlatSpec with WireMockHelper with EitherValues with Matchers {
  //scalastyle:off method.length
  def errorHandlerForApiFailures(call: => Future[Either[HttpException, JsValue]], url: String): Unit = {

    it should "handle BAD_REQUEST (400) - INVALID_PAYLOAD" in {
      server.stubFor(
        post(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withHeader("Content-Type", "application/json")
              .withBody(errorResponse("INVALID_PAYLOAD"))
          )
      )
      call.map {
        response =>
          response.left.value shouldBe a[BadRequestException]
          response.left.value.message should include("INVALID_PAYLOAD")
      }
    }

    it should "handle NOT_FOUND (404)" in {
      server.stubFor(
        post(urlEqualTo(url))
          .willReturn(
            notFound
          )
      )
      call.map {
        response =>
          response.left.value shouldBe a[NotFoundException]
      }
    }

    it should "handle SERVER_ERROR (500)" in {
      server.stubFor(
        post(urlEqualTo(url))
          .willReturn(
            serverError
          )
      )
      recoverToExceptionIf[Upstream5xxResponse](call) map {
        ex =>
          ex.upstreamResponseCode shouldBe INTERNAL_SERVER_ERROR
      }
    }

    it should "handle SERVICE_UNAVAILABLE (503)" in {
      server.stubFor(
        post(urlEqualTo(url))
          .willReturn(
            serviceUnavailable
          )
      )
      recoverToExceptionIf[Upstream5xxResponse](call) map {
        ex =>
          ex.upstreamResponseCode shouldBe SERVICE_UNAVAILABLE
      }
    }
  }
}
