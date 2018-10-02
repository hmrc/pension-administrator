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

import audit.{AuditService, StubSuccessfulAuditService}
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import config.AppConfig
import connectors.helper.ConnectorBehaviours
import models.SchemeReferenceNumber
import org.joda.time.LocalDate
import org.scalatest._
import org.slf4j.event.Level
import play.api.LoggerLike
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsBoolean, Json}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http._
import utils.{StubLogger, WireMockHelper}

class SchemeConnectorSpec extends AsyncFlatSpec
  with Matchers
  with WireMockHelper
  with OptionValues
  with RecoverMethods
  with EitherValues
  with ConnectorBehaviours{

  import SchemeConnectorSpec._

  override protected def portConfigKey: String = "microservice.services.des-hod.port"

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(auditService),
      bind[LoggerLike].toInstance(logger)
    )

  lazy val connector: SchemeConnector = injector.instanceOf[SchemeConnector]
  lazy val appConfig: AppConfig = injector.instanceOf[AppConfig]

  "SchemeConnector registerPSA" should "handle OK (200)" in {
    val successResponse = Json.obj(
      "processingDate" -> LocalDate.now,
      "formBundle" -> "1121313",
      "psaId" -> "A21999999"
    )
    server.stubFor(
      post(urlEqualTo(registerPsaUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(equalToJson(Json.stringify(registerPsaData)))
        .willReturn(
          ok(Json.stringify(successResponse))
            .withHeader("Content-Type", "application/json")
        )
    )
    connector.registerPSA(registerPsaData).map { response =>
      response.right.value shouldBe successResponse
    }
  }

  it should "return a BadRequestException for a 400 INVALID_CORRELATION_ID response" in {
    server.stubFor(
      post(urlEqualTo(registerPsaUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(invalidCorrelationIdResponse)
        )
    )
    connector.registerPSA(registerPsaData).map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_CORRELATION_ID")
    }
  }

  it should "log details of an INVALID_PAYLOAD for a 400 BAD request" in {
    server.stubFor(
      post(urlEqualTo(registerPsaUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody("INVALID_PAYLOAD")
        )
    )

    logger.reset()
    connector.registerPSA(registerPsaData).map {
      _ =>
        logger.getLogEntries.size shouldBe 1
        logger.getLogEntries.head.level shouldBe Level.WARN
    }
  }

  it should "return a ForbiddenException for a 403 INVALID_BUSINESS_PARTNER response" in {
    server.stubFor(
      post(urlEqualTo(registerPsaUrl))
        .willReturn(
          forbidden
            .withHeader("Content-Type", "application/json")
            .withBody(invalidBusinessPartnerResponse)
        )
    )

    connector.registerPSA(registerPsaData).map {
      response =>
        response.left.value shouldBe a[ForbiddenException]
        response.left.value.message should include("INVALID_BUSINESS_PARTNER")
    }
  }

  it should "return a ConflictException for a 409 DUPLICATE_SUBMISSION response" in {
    server.stubFor(
      post(urlEqualTo(registerPsaUrl))
        .willReturn(
          aResponse()
            .withStatus(CONFLICT)
            .withHeader("Content-Type", "application/json")
            .withBody(duplicateSubmissionResponse)
        )
    )
    connector.registerPSA(registerPsaData).map {
      response =>
        response.left.value shouldBe a[ConflictException]
        response.left.value.message should include("DUPLICATE_SUBMISSION")
    }
  }

  it should behave like errorHandlerForPostApiFailures(
    connector.registerPSA(registerPsaData),
    registerPsaUrl
  )

  "SchemeConnector getPSASubscriptionDetails" should "handle OK (200)" in {

       server.stubFor(
         get(urlEqualTo(psaSubscriptionDetailsUrl))
           .withHeader("Content-Type", equalTo("application/json"))
           .willReturn(
             ok(psaSubscriptionData.toString())
               .withHeader("Content-Type", "application/json")
           )
       )
       connector.getPSASubscriptionDetails(psaId.value).map { response =>
         response.right.value shouldBe psaSubscriptionData
         server.findAll(getRequestedFor(urlPathEqualTo(psaSubscriptionDetailsUrl))).size() shouldBe 1
       }

  }

  it should behave like errorHandlerForGetApiFailures(
    connector.getPSASubscriptionDetails(psaId.value),
    psaSubscriptionDetailsUrl
  )

  "SchemeConnector checkForAssociation" should "handle OK (200)" in {

    server.stubFor(
      get(urlEqualTo(s"${appConfig.pensionsScheme}/psa-associated"))
        .withHeader("Content-Type", equalTo("application/json"))
        .withHeader("psaId", equalTo(psaId.value))
        .withHeader("schemeReferenceNumber", equalTo(srn))
        .willReturn(
          ok(JsBoolean(true).toString())
            .withHeader("Content-Type", "application/json")
        )
    )

    connector.checkForAssociation(psaId, srn) map { response =>
      response.right.value shouldBe psaSubscriptionData
      server.findAll(getRequestedFor(urlPathEqualTo(psaSubscriptionDetailsUrl))).size() shouldBe 1
    }

  }

  }

object SchemeConnectorSpec extends JsonFileReader {
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")
  private val registerPsaData = readJsonFromFile("/data/validPsaRequest.json")
  private val psaSubscriptionData = readJsonFromFile("/data/validPSASubscriptionDetails.json")

  val srn = SchemeReferenceNumber("S0987654321")
  val psaId = PsaId("A7654321")
  val registerPsaUrl = "/pension-online/subscription"
  val psaSubscriptionDetailsUrl = s"/pension-online/psa-subscription-details/$psaId"

  private val invalidBusinessPartnerResponse =
    Json.stringify(
      Json.obj(
        "code" -> "INVALID_BUSINESS_PARTNER",
        "reason" -> "test-reason"
      )
    )

  private val invalidCorrelationIdResponse =
    Json.stringify(
      Json.obj(
        "code" -> "INVALID_CORRELATION_ID",
        "reason" -> "test-reason"
      )
    )

  private val duplicateSubmissionResponse =
    Json.stringify(
      Json.obj(
        "code" -> "DUPLICATE_SUBMISSION",
        "reason" -> "test-reason"
      )
    )

  private val notFoundResponse =
    Json.stringify(
      Json.obj(
        "code" -> "NOT_FOUND",
        "reason" -> "test-reason"
      )
    )
  val auditService = new StubSuccessfulAuditService()
  val logger = new StubLogger()
}
