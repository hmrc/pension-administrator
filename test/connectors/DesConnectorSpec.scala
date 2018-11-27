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

import audit.{AuditService, PSADetails, StubSuccessfulAuditService}
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock.{serverError, _}
import config.AppConfig
import connectors.helper.ConnectorBehaviours
import models.{PSTR, PsaToBeRemovedFromScheme, SchemeReferenceNumber}
import org.joda.time.LocalDate
import org.scalatest._
import org.slf4j.event.Level
import play.api.LoggerLike
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers.NOT_FOUND
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.{BadRequestException, _}
import utils.{FakeDesConnector, StubLogger, WireMockHelper}
import utils.testhelpers.PsaSubscriptionBuilder._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

class DesConnectorSpec extends AsyncFlatSpec
  with Matchers
  with WireMockHelper
  with OptionValues
  with RecoverMethods
  with EitherValues
  with ConnectorBehaviours{

  import DesConnectorSpec._

  override def beforeEach(): Unit = {
    auditService.reset()
    super.beforeEach()
  }

  override protected def portConfigKey: String = "microservice.services.des-hod.port"

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(auditService),
      bind[LoggerLike].toInstance(logger)
    )

  lazy val connector: DesConnector = injector.instanceOf[DesConnector]
  lazy val appConfig: AppConfig = injector.instanceOf[AppConfig]

  "DesConnector registerPSA" should "handle OK (200)" in {
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
            .withBody(errorResponse("INVALID_CORRELATION_ID"))
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
            .withBody(errorResponse("INVALID_BUSINESS_PARTNER"))
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
            .withBody(errorResponse("DUPLICATE_SUBMISSION"))
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

  "DesConnector getPSASubscriptionDetails" should "handle OK (200)" in {

       server.stubFor(
         get(urlEqualTo(psaSubscriptionDetailsUrl))
           .withHeader("Content-Type", equalTo("application/json"))
           .willReturn(
             ok(psaSubscriptionData.toString())
               .withHeader("Content-Type", "application/json")
           )
       )
       connector.getPSASubscriptionDetails(psaId.value).map { response =>
         response.right.value shouldBe psaSubscription
         server.findAll(getRequestedFor(urlPathEqualTo(psaSubscriptionDetailsUrl))).size() shouldBe 1
       }

  }

  it should behave like errorHandlerForGetApiFailures(
    connector.getPSASubscriptionDetails(psaId.value),
    psaSubscriptionDetailsUrl
  )

  it should "return a JsResultException if the API response cannot be converted to PsaSubscription" in {
    server.stubFor(
      get(urlEqualTo(psaSubscriptionDetailsUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .willReturn(
          ok(invalidPsaSubscriptionResponse.toString())
            .withHeader("Content-Type", "application/json")
        )
    )

    recoverToExceptionIf[JsResultException](connector.getPSASubscriptionDetails(psaId.value)) map {
      ex =>
        ex.leftSide shouldBe a[JsResultException]
    }
  }

  it should "send a GetPSADetails audit event on success" in {

    server.stubFor(
      get(urlEqualTo(psaSubscriptionDetailsUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .willReturn(
          ok(psaSubscriptionData.toString())
            .withHeader("Content-Type", "application/json")
        )
    )

    connector.getPSASubscriptionDetails(psaId.value).map { _ =>
      auditService.verifySent(
        PSADetails(
          psaId = psaId.value,
          psaName = Some("abcdefghijkl abcdefghijkl abcdefjkl"),
          status = OK,
          response = Some(Json.toJson(psaSubscription))
        )
      ) shouldBe true
    }
  }

  it should "send a GetPSADetails audit event on not found" in {

    server.stubFor(
      get(urlEqualTo(psaSubscriptionDetailsUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .willReturn(
          notFound()
        )
    )

    connector.getPSASubscriptionDetails(psaId.value).map { _ =>
        auditService.verifySent(
          PSADetails(
            psaId = psaId.value,
            psaName = None,
            status = NOT_FOUND,
            response = None
          )
        ) shouldBe true
    }
  }


  it should "not send a GetPSADetails audit event on JsResultException if the API response cannot be converted to PsaSubscription" in {

    server.stubFor(
      get(urlEqualTo(psaSubscriptionDetailsUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .willReturn(
          ok(invalidPsaSubscriptionResponse.toString())
            .withHeader("Content-Type", "application/json")
        )
    )

    recoverToExceptionIf[JsResultException](connector.getPSASubscriptionDetails(psaId.value)) map { _ =>
      auditService.verifyNothingSent shouldBe true
    }
  }

  it should "not send a GetPSADetails audit event on failure" in {

    val failureResponse = Json.obj(
      "code" -> "INVALID_PAYLOAD",
      "reason" -> "Submission has not passed validation. Invalid PAYLOAD"
    )

    server.stubFor(
      get(urlEqualTo(psaSubscriptionDetailsUrl))
        .willReturn(
          serverError
            .withBody(failureResponse.toString)
        )
    )

    recoverToExceptionIf[Upstream5xxResponse](connector.getPSASubscriptionDetails(psaId.value)) map {_ =>
      auditService.verifyNothingSent shouldBe true
    }

  }

  "DesConnector removePSA" should "handle OK (200)" in {
    val successResponse = FakeDesConnector.removePsaResponseJson
    server.stubFor(
      post(urlEqualTo(removePsaUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(equalToJson(Json.stringify(removePsaData)))
        .willReturn(
          ok(Json.stringify(successResponse))
            .withHeader("Content-Type", "application/json")
        )
    )
    connector.removePSA(removePsaDataModel).map { response =>
      response.right.value shouldBe successResponse
    }
  }

  it should "return a BadRequestException for a 400 INVALID_CORRELATION_ID response" in {
    server.stubFor(
      post(urlEqualTo(removePsaUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_CORRELATION_ID"))
        )
    )
    connector.removePSA(removePsaDataModel).map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_CORRELATION_ID")
    }
  }

  it should "return a BadRequestException for a 400 INVALID_PSTR response" in {
    server.stubFor(
      post(urlEqualTo(removePsaUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_PSTR"))
        )
    )
    connector.removePSA(removePsaDataModel).map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_PSTR")
    }
  }

  it should "return a BadRequestException for a 400 INVALID_PSAID response" in {
    server.stubFor(
      post(urlEqualTo(removePsaUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_PSAID"))
        )
    )
    connector.removePSA(removePsaDataModel).map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_PSAID")
    }
  }

  it should "log details of an INVALID_PAYLOAD for a 400 BAD request" in {
    server.stubFor(
      post(urlEqualTo(removePsaUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody("INVALID_PAYLOAD")
        )
    )

    logger.reset()
    connector.removePSA(removePsaDataModel).map {
      _ =>
        logger.getLogEntries.size shouldBe 1
        logger.getLogEntries.head.level shouldBe Level.WARN
    }
  }

  it should "return a ForbiddenException for a 403 NO_RELATIONSHIP_EXISTS response" in {
    server.stubFor(
      post(urlEqualTo(removePsaUrl))
        .willReturn(
          forbidden
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("NO_RELATIONSHIP_EXISTS"))
        )
    )

    connector.removePSA(removePsaDataModel).map {
      response =>
        response.left.value shouldBe a[ForbiddenException]
        response.left.value.message should include("NO_RELATIONSHIP_EXISTS")
    }
  }

  it should "return a ForbiddenException for a 403 NO_OTHER_ASSOCIATED_PSA response" in {
    server.stubFor(
      post(urlEqualTo(removePsaUrl))
        .willReturn(
          forbidden
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("NO_OTHER_ASSOCIATED_PSA"))
        )
    )

    connector.removePSA(removePsaDataModel).map {
      response =>
        response.left.value shouldBe a[ForbiddenException]
        response.left.value.message should include("NO_OTHER_ASSOCIATED_PSA")
    }
  }

  it should "return a ForbiddenException for a 403 FUTURE_CEASE_DATE response" in {
    server.stubFor(
      post(urlEqualTo(removePsaUrl))
        .willReturn(
          forbidden
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("FUTURE_CEASE_DATE"))
        )
    )

    connector.removePSA(removePsaDataModel).map {
      response =>
        response.left.value shouldBe a[ForbiddenException]
        response.left.value.message should include("FUTURE_CEASE_DATE")
    }
  }

  it should "return a ForbiddenException for a 403 PSAID_NOT_ACTIVE response" in {
    server.stubFor(
      post(urlEqualTo(removePsaUrl))
        .willReturn(
          forbidden
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("PSAID_NOT_ACTIVE"))
        )
    )

    connector.removePSA(removePsaDataModel).map {
      response =>
        response.left.value shouldBe a[ForbiddenException]
        response.left.value.message should include("PSAID_NOT_ACTIVE")
    }
  }

  it should "return a ConflictException for a 409 DUPLICATE_SUBMISSION response" in {
    server.stubFor(
      post(urlEqualTo(removePsaUrl))
        .willReturn(
          aResponse()
            .withStatus(CONFLICT)
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("DUPLICATE_SUBMISSION"))
        )
    )
    connector.removePSA(removePsaDataModel).map {
      response =>
        response.left.value shouldBe a[ConflictException]
        response.left.value.message should include("DUPLICATE_SUBMISSION")
    }
  }

  it should "return not found exception and failure response details for a 404 response" in {
    server.stubFor(
      post(urlEqualTo(removePsaUrl))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("NOT_FOUND"))
        )
    )

    connector.removePSA(removePsaDataModel).collect {
      case Left(_: NotFoundException) => succeed
    }
  }

  it should behave like errorHandlerForPostApiFailures(
    connector.removePSA(removePsaDataModel),
    removePsaUrl
  )

}

object DesConnectorSpec extends JsonFileReader {
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")
  private val registerPsaData = readJsonFromFile("/data/validPsaRequest.json")
  private val psaSubscriptionData = readJsonFromFile("/data/validPSASubscriptionDetails.json")
  private val invalidPsaSubscriptionResponse = readJsonFromFile("/data/validPsaRequest.json")

  val srn = SchemeReferenceNumber("S0987654321")
  val psaId = PsaId("A7654321")
  val pstr: String = PSTR("123456789AB")
  val removalDate: LocalDate = LocalDate.now()

  private val removePsaData: JsValue = Json.obj("ceaseDate" -> removalDate.toString)
  private val removePsaDataModel: PsaToBeRemovedFromScheme = PsaToBeRemovedFromScheme(psaId.id, pstr, removalDate)

  val registerPsaUrl = "/pension-online/subscription"
  val psaSubscriptionDetailsUrl = s"/pension-online/psa-subscription-details/$psaId"
  val removePsaUrl = s"/pension-online/cease-psa/psaid/$psaId/pstr/$pstr"

  private def errorResponse(code: String) =
    Json.stringify(
      Json.obj(
        "code" -> code,
        "reason" -> "test-reason"
      )
    )

  val auditService = new StubSuccessfulAuditService()
  val logger = new StubLogger()
}
