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

import audit.{AuditService, StubSuccessfulAuditService}
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.ConnectorBehaviours
import org.scalatest._
import org.slf4j.event.Level
import play.api.LoggerLike
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import utils.{StubLogger, WireMockHelper}
import play.api.test.Helpers._

class AssociationConnectorSpec extends AsyncFlatSpec
  with Matchers
  with WireMockHelper
  with OptionValues
  with RecoverMethods
  with EitherValues
  with ConnectorBehaviours {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val auditService = new StubSuccessfulAuditService()
  val logger = new StubLogger()

  private val psaId = "A2123456"
  private val psaMinimalDetailsUrl = s"/pension-online/psa-min-details/${psaId}"

  override protected def portConfigKey: String = "microservice.services.des-hod.port"

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(auditService),
      bind[LoggerLike].toInstance(logger)
    )


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

  it should "return bad request - 400 if body contains INVALID_PSAID and log the event as warn" in {

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

    logger.reset()

    connector.getPSAMinimalDetails(psaId).map { response =>
      response.left.value shouldBe a[BadRequestException]
      response.left.value.message shouldBe Json.parse(errorResponse).toString()
      logger.getLogEntries.size shouldBe 1
      logger.getLogEntries.head.level shouldBe Level.WARN
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


  it should "throw upstream4xx - if any other 400" in {

    val errorResponse = """{
                         |	"code": "not valid",
                         |	"reason": "any other exception message"
                         |}""".stripMargin
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          badRequest().withBody(Json.parse(errorResponse).toString)
        )
    )

    recoverToExceptionIf[Upstream4xxResponse] (connector.getPSAMinimalDetails(psaId)) map {
      ex =>
        ex.upstreamResponseCode shouldBe BAD_REQUEST
        ex.message shouldBe Json.parse(errorResponse).toString
        ex.reportAs shouldBe BAD_REQUEST
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
          notFound().withBody(Json.parse(errorResponse).toString)
        )
    )

    connector.getPSAMinimalDetails(psaId).map { response =>
      response.left.value shouldBe a[NotFoundException]
      response.left.value.message shouldBe Json.parse(errorResponse).toString()
    }

  }

  it should "throw Upstream4XX for server unavailable - 403" in {

    val errorResponse = """{
                          |	"code": "FORBIDDEN",
                          |	"reason": "Dependent systems are currently not responding."
                          |}""".stripMargin
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          forbidden().withBody(Json.parse(errorResponse).toString)
        )
    )

    recoverToExceptionIf[Upstream4xxResponse] (connector.getPSAMinimalDetails(psaId)) map {
      ex =>
        ex.upstreamResponseCode shouldBe FORBIDDEN
        ex.message shouldBe Json.parse(errorResponse).toString
        ex.reportAs shouldBe BAD_REQUEST
    }

  }

  it should "throw Upstream5XX for internal server error - 500" in {

    val errorResponse = """{
                          |	"code": "SERVER_ERROR",
                          |	"reason": "DES is currently experiencing problems that require live service intervention."
                          |}""".stripMargin
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          serverError().withBody(Json.parse(errorResponse).toString)
        )
    )

    recoverToExceptionIf[Upstream5xxResponse] (connector.getPSAMinimalDetails(psaId)) map {
      ex =>
        ex.upstreamResponseCode shouldBe INTERNAL_SERVER_ERROR
        ex.message shouldBe Json.parse(errorResponse).toString
        ex.reportAs shouldBe BAD_GATEWAY
    }

  }

  it should "throw exception for other runtime exception" in {

    val errorResponse = """{
                          |	"code": "SERVER_ERROR",
                          |	"reason": "DES is currently experiencing problems that require live service intervention."
                          |}""".stripMargin
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          noContent()
        )
    )

    recoverToExceptionIf[Exception] (connector.getPSAMinimalDetails(psaId)) map {
      ex =>
        ex.getMessage shouldBe s"PSA minimal details failed with status ${NO_CONTENT}. Response body: ''"
    }

  }

}