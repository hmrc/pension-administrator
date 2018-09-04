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
import connectors.helper.ConnectorBehaviours
import org.scalatest._
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import utils.WireMockHelper

class AssociationConnectorSpec extends AsyncFlatSpec
  with Matchers
  with WireMockHelper
  with OptionValues
  with RecoverMethods
  with EitherValues
  with ConnectorBehaviours {

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val psaId = "A2123456"
  private val psaMinimalDetailsUrl = s"/pension-online/psa-min-details/$psaId"

  override protected def portConfigKey: String = "microservice.services.des-hod.port"

  lazy val connector = injector.instanceOf[AssociationConnector]

  "AssociationConnector" should "return OK (200) with a JSON payload" in {

    val individualDetails = """{"processingDate": "2001-12-17T09:30:47Z"}""".stripMargin

    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          ok(Json.parse(individualDetails).toString())
            .withHeader("Content-Type", "application/json")
        )
    )

    connector.getPSAMinimalDetails(psaId).map { response =>
      response.right.value shouldBe Json.parse(individualDetails)
    }
  }

  it should "return bad request - 400 if body contains INVALID_PSAID" in {

    val errorResponse = """{
                         |	"code": "INVALID_PSAID",
                         |	"reason": "Submission has not passed validation. Invalid parameter PSAID."
                         |}""".stripMargin
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          badRequest().withBody(Json.parse(errorResponse).toString)
        )
    )

    connector.getPSAMinimalDetails(psaId).map { response =>
      response.left.value shouldBe a[BadRequestException]
      response.left.value.message shouldBe Json.parse(errorResponse).toString()
    }
  }


  it should "return bad request - 400 if body contains INVALID_CORRELATIONID" in {

    val errorResponse = """{
                         |	"code": "INVALID_CORRELATIONID",
                         |	"reason": "Submission has not passed validation. Invalid header CorrelationId."
                         |}""".stripMargin
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          badRequest().withBody(Json.parse(errorResponse).toString)
        )
    )

    connector.getPSAMinimalDetails(psaId).map { response =>
      response.left.value shouldBe a[BadRequestException]
      response.left.value.message shouldBe Json.parse(errorResponse).toString()
    }

  }

  it should behave like errorHandlerForGetApiFailures(
    connector.getPSAMinimalDetails(psaId),
    psaMinimalDetailsUrl
  )

}