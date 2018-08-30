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

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.ConnectorBehaviours
import org.scalatest._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpException, HeaderCarrier}
import utils.WireMockHelper
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global

class AssociationConnectorSpec extends AsyncFlatSpec
  with Matchers
  with WireMockHelper
  with OptionValues
  with RecoverMethods
  with EitherValues
  with ConnectorBehaviours {

  override protected def portConfigKey: String = "microservice.services.des-hod.port"

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val psaId = "A2123456"
  val psaMinimalDetailsUrl = s"/pension-online/psa-min-details/${psaId}"

  lazy val connector = injector.instanceOf[AssociationConnector]

  "AssociationConnector" should "return OK (200) - all fields with individualDetails" in {

    val individualDetails = """{
                     |	"processingDate": "2001-12-17T09:30:47Z",
                     |	"psaMinimalDetails": {
                     |		"individualDetails": {
                     |			"firstName": "abcdefghjffgfg",
                     |			"middleName": "dfgfdgdfgfdgd",
                     |			"lastName": "sfdsfsdgdfgdfg"
                     |		}
                     |	},
                     |	"email": "aaa@email.com",
                     |	"psaSuspensionFlag": true
                     |}""".stripMargin

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

  it should "return OK (200) - All fields with organisationOrPartnershipName" in {

    val organisationOrPartnershipName = """{
                              |	"processingDate": "2009-12-17T09:30:47Z",
                              |	"psaMinimalDetails": {
                              |		"organisationOrPartnershipName": "a"
                              |	},
                              |	"email": "bbb@email.com",
                              |	"psaSuspensionFlag": true
                              |}
                              |""".stripMargin

    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          ok(Json.parse(organisationOrPartnershipName).toString())
            .withHeader("Content-Type", "application/json")
        )
    )

    connector.getPSAMinimalDetails(psaId).map { response =>
      response.right.value shouldBe Json.parse(organisationOrPartnershipName)
    }
  }

  it should "return bad request - 400" in {

    val errorReponse = """{
                         |	"code": "INVALID_PSAID",
                         |	"reason": "Submission has not passed validation. Invalid parameter PSAID."
                         |}""".stripMargin
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          aResponse().withStatus(BAD_REQUEST).withBody(Json.parse(errorReponse).toString)
        )
    )

    connector.getPSAMinimalDetails(psaId).map { response =>
      response.left.value shouldBe a[HttpException]
      //response.body shouldBe Json.parse(errorReponse).toString()
    }

  }

  it should "return Not Found - 404" in {

    val errorResponse = """{
                         |	"code": "NOT_FOUND",
                         |	"reason": "The back end has indicated that there is no match found."
                         |}""".stripMargin
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          aResponse().withStatus(NOT_FOUND).withBody(Json.parse(errorResponse).toString)
        )
    )

    connector.getPSAMinimalDetails(psaId).map { response =>
      response.left.value shouldBe a[HttpException]
    }

  }

  it should "return internal server error - 500" in {

    val errorResponse = """{
                          |	"code": "SERVER_ERROR",
                          |	"reason": "DES is currently experiencing problems that require live service intervention."
                          |}""".stripMargin
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody(Json.parse(errorResponse).toString)
        )
    )

    connector.getPSAMinimalDetails(psaId).map { response =>
      response.left.value shouldBe a[HttpException]
    }
   
  }

  it should "return server unavailable - 503" in {

    val errorResponse = """{
                          |	"code": "SERVICE_UNAVAILABLE",
                          |	"reason": "Dependent systems are currently not responding."
                          |}""".stripMargin
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          aResponse().withStatus(SERVICE_UNAVAILABLE).withBody(Json.parse(errorResponse).toString)
        )
    )

    connector.getPSAMinimalDetails(psaId).map { response =>
      response.left.value shouldBe a[HttpException]
    }
  }



}