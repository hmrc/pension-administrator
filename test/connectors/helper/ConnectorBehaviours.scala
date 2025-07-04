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

package connectors.helper

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.EitherValues
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.test.Helpers.*
import uk.gov.hmrc.http.*
import utils.WireMockHelper

import scala.concurrent.Future

trait ConnectorBehaviours extends AsyncFlatSpec with WireMockHelper with EitherValues with Matchers {

  def errorHandlerForPostApiFailures[A](call: => Future[Either[HttpException, A]], url: String): Unit = {

    errorHandlerForApiFailures(call, post(urlEqualTo(url)))
  }
  def errorHandlerForPutApiFailures[A](call: => Future[Either[HttpException, A]], url: String): Unit = {

    errorHandlerForApiFailures(call, put(urlEqualTo(url)))
  }

  def errorHandlerForGetApiFailures[A](call: => Future[Either[HttpException, A]], url: String): Unit = {

    errorHandlerForApiFailures(call, get(urlEqualTo(url)))

  }

  private def errorHandlerForApiFailures[A](call: => Future[Either[HttpException, A]],  method: MappingBuilder): Unit = {

   it should "handle SERVER_ERROR (500)" in {

      server.stubFor(
        method
          .willReturn(
            serverError
          )
      )

      recoverToExceptionIf[UpstreamErrorResponse](call) map {
        ex =>
          ex.statusCode.shouldBe(INTERNAL_SERVER_ERROR)
      }
    }

    it should "throw exception for not valid http reposponse code" in {

      server.stubFor(
        method
          .willReturn(
            aResponse().withStatus(600)
          )
      )

      recoverToExceptionIf[Exception] (call) map {
        ex =>
          ex.getMessage.should(include("failed with status"))
      }
    }

  }
}
