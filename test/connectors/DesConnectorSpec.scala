/*
 * Copyright 2019 HM Revenue & Customs
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

import audit.{AuditService, PSARemovalFromSchemeAuditEvent, StubSuccessfulAuditService}
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import config.FeatureSwitchManagementService
import connectors.helper.ConnectorBehaviours
import models.{PSTR, PsaToBeRemovedFromScheme, SchemeReferenceNumber}
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import org.slf4j.event.Level
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.JodaWrites._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers.NOT_FOUND
import play.api.{Application, LoggerLike}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.{BadRequestException, _}
import utils.JsonTransformations.PSASubscriptionDetailsTransformer
import utils.testhelpers.PsaSubscriptionBuilder._
import utils.{FakeDesConnector, FakeFeatureSwitchManagementService, StubLogger, WireMockHelper}

import scala.concurrent.Future

class DesConnectorSpec extends AsyncFlatSpec
  with WireMockHelper
  with OptionValues
  with RecoverMethods
  with EitherValues
  with ConnectorBehaviours with MockitoSugar{

  import DesConnectorSpec._

  override protected def portConfigKey: String = "microservice.services.des-hod.port"

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(auditService),
      bind[LoggerLike].toInstance(logger)
    )

  lazy val connector: DesConnector = injector.instanceOf[DesConnector]

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

    lazy val appWithFeatureEnabled: Application = new GuiceApplicationBuilder().configure(portConfigKey -> server.port().toString,
      "auditing.enabled" -> false,
      "metrics.enabled" -> false
    ).overrides(bind[FeatureSwitchManagementService].toInstance(FakeFeatureSwitchManagementService(false))).build()

    val connector: DesConnector = appWithFeatureEnabled.injector.instanceOf[DesConnector]


    server.stubFor(
         get(urlEqualTo(psaSubscriptionDetailsUrl))
           .withHeader("Content-Type", equalTo("application/json"))
           .willReturn(
             ok(psaSubscriptionData.toString())
               .withHeader("Content-Type", "application/json")
           )
       )
       connector.getPSASubscriptionDetails(psaId.value).map { response =>
         response.right.value shouldBe Json.toJson(psaSubscription)
         server.findAll(getRequestedFor(urlPathEqualTo(psaSubscriptionDetailsUrl))).size() shouldBe 1
       }

  }

  it should "handle OK (200) if variations is enabled" in {

    lazy val appWithFeatureEnabled: Application = new GuiceApplicationBuilder().configure(portConfigKey -> server.port().toString,
      "auditing.enabled" -> false,
      "metrics.enabled" -> false
    ).overrides(bind[FeatureSwitchManagementService].toInstance(FakeFeatureSwitchManagementService(true))).build()

    val connector: DesConnector = appWithFeatureEnabled.injector.instanceOf[DesConnector]

     server.stubFor(
       get(urlEqualTo(psaSubscriptionDetailsUrl))
         .withHeader("Content-Type", equalTo("application/json"))
         .willReturn(
           ok(psaSubscriptionData.toString())
             .withHeader("Content-Type", "application/json")
         )
     )
    connector.getPSASubscriptionDetails(psaId.value).map { response =>
       response.right.value shouldBe Json.parse(psaSubscriptionUserAnswers)
       server.findAll(getRequestedFor(urlPathEqualTo(psaSubscriptionDetailsUrl))).size() shouldBe 1
     }

  }

  it should behave like errorHandlerForGetApiFailures(
    connector.getPSASubscriptionDetails(psaId.value),
    psaSubscriptionDetailsUrl
  )

  it should "return a PSAFailedMapToUserAnswersException if the API response cannot be transformed to UserAnswers" in {

    val pSASubscriptionDetailsTransformer = mock[PSASubscriptionDetailsTransformer]

    when(pSASubscriptionDetailsTransformer.transformToUserAnswers).thenReturn(__.json.copyFrom((__ \ 'hello).json.pick))

    lazy val appWithFeatureEnabled: Application = new GuiceApplicationBuilder().configure(portConfigKey -> server.port().toString,
      "auditing.enabled" -> false,
      "metrics.enabled" -> false
    ).overrides(
      Seq(bind[FeatureSwitchManagementService].toInstance(FakeFeatureSwitchManagementService(true)),
      bind[PSASubscriptionDetailsTransformer].toInstance(pSASubscriptionDetailsTransformer))
      ).build()

    val connector: DesConnector = appWithFeatureEnabled.injector.instanceOf[DesConnector]

    server.stubFor(
      get(urlEqualTo(psaSubscriptionDetailsUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .willReturn(
          ok(psaSubscriptionData.toString())
            .withHeader("Content-Type", "application/json")
        )
    )

    recoverToExceptionIf[PSAFailedMapToUserAnswersException](connector.getPSASubscriptionDetails(psaId.value)) map {
      ex =>
        ex.leftSide shouldBe a[PSAFailedMapToUserAnswersException]
    }
  }

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

      val expectedAuditEvent = PSARemovalFromSchemeAuditEvent(PsaToBeRemovedFromScheme(
        removePsaDataModel.psaId, removePsaDataModel.pstr, removePsaDataModel.removalDate))
      auditService.verifySent(expectedAuditEvent) shouldBe true

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

  "DesConnector deregisterPSA" should "handle OK (200)" in {
    val successResponse = FakeDesConnector.deregisterPsaResponseJson
    server.stubFor(
      post(urlEqualTo(deregisterPsaUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(equalToJson(Json.stringify(deregisterPsaData)))
        .willReturn(
          ok(Json.stringify(successResponse))
            .withHeader("Content-Type", "application/json")
        )
    )

    connector.deregisterPSA(psaId.id).map { response =>
      response.right.value shouldBe successResponse
    }
  }

  it should "return a BadRequestException for a 400 INVALID_CORRELATION_ID response" in {
    server.stubFor(
      post(urlEqualTo(deregisterPsaUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_CORRELATION_ID"))
        )
    )
    connector.deregisterPSA(psaId.id).map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_CORRELATION_ID")
    }
  }

  it should "return a BadRequestException for a 400 INVALID_IDTYPE response" in {
    server.stubFor(
      post(urlEqualTo(deregisterPsaUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_IDTYPE"))
        )
    )
    connector.deregisterPSA(psaId.id).map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_IDTYPE")
    }
  }

  it should "return a BadRequestException for a 400 INVALID_IDVALUE response" in {
    server.stubFor(
      post(urlEqualTo(deregisterPsaUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_IDVALUE"))
        )
    )
    connector.deregisterPSA(psaId.id).map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_IDVALUE")
    }
  }

  it should "log details of an INVALID_PAYLOAD for a 400 BAD request" in {
    server.stubFor(
      post(urlEqualTo(deregisterPsaUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody("INVALID_PAYLOAD")
        )
    )

    logger.reset()
    connector.deregisterPSA(psaId.id).map {
      _ =>
        logger.getLogEntries.size shouldBe 1
        logger.getLogEntries.head.level shouldBe Level.WARN
    }
  }

  it should "return a ForbiddenException for a 403 ACTIVE_RELATIONSHIP_EXISTS response" in {
    server.stubFor(
      post(urlEqualTo(deregisterPsaUrl))
        .willReturn(
          forbidden
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("ACTIVE_RELATIONSHIP_EXISTS"))
        )
    )

    connector.deregisterPSA(psaId.id).map {
      response =>
        response.left.value shouldBe a[ForbiddenException]
        response.left.value.message should include("ACTIVE_RELATIONSHIP_EXISTS")
    }
  }

  it should "return a ForbiddenException for a 403 ALREADY_DEREGISTERED response" in {
    server.stubFor(
      post(urlEqualTo(deregisterPsaUrl))
        .willReturn(
          forbidden
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("ALREADY_DEREGISTERED"))
        )
    )

    connector.deregisterPSA(psaId.id).map {
      response =>
        response.left.value shouldBe a[ForbiddenException]
        response.left.value.message should include("ALREADY_DEREGISTERED")
    }
  }

  it should "return a ForbiddenException for a 403 INVALID_DEREGISTRATION_DATE response" in {
    server.stubFor(
      post(urlEqualTo(deregisterPsaUrl))
        .willReturn(
          forbidden
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_DEREGISTRATION_DATE"))
        )
    )

    connector.deregisterPSA(psaId.id).map {
      response =>
        response.left.value shouldBe a[ForbiddenException]
        response.left.value.message should include("INVALID_DEREGISTRATION_DATE")
    }
  }

  it should "return a ConflictException for a 409 DUPLICATE_SUBMISSION response" in {
    server.stubFor(
      post(urlEqualTo(deregisterPsaUrl))
        .willReturn(
          aResponse()
            .withStatus(CONFLICT)
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("DUPLICATE_SUBMISSION"))
        )
    )
    connector.deregisterPSA(psaId.id).map {
      response =>
        response.left.value shouldBe a[ConflictException]
        response.left.value.message should include("DUPLICATE_SUBMISSION")
    }
  }

  it should "return not found exception and failure response details for a 404 response" in {
    server.stubFor(
      post(urlEqualTo(deregisterPsaUrl))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("NOT_FOUND"))
        )
    )

    connector.deregisterPSA(psaId.id).collect {
      case Left(_: NotFoundException) => succeed
    }
  }

  "DesConnector updatePSA" should "handle OK (200)" in {
    val successResponse = Json.obj(
      "processingDate"-> "2001-12-17T09:30:47Z",
      "formBundleNumber"-> "12345678912"
    )
    server.stubFor(
      post(urlEqualTo(variationPsaUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(equalToJson(Json.stringify(psaVariationData)))
        .willReturn(
          ok(Json.stringify(successResponse))
            .withHeader("Content-Type", "application/json")
        )
    )
    connector.updatePSA(psaId.id, psaVariationData).map { response =>
      response.right.value shouldBe successResponse
    }
  }

  it should "return a BadRequestException for a 400 INVALID_CORRELATION_ID response" in {
    server.stubFor(
      post(urlEqualTo(variationPsaUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_CORRELATION_ID"))
        )
    )
    connector.updatePSA(psaId.id, psaVariationData).map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_CORRELATION_ID")
    }
  }

  it should "log details of an INVALID_PAYLOAD for a 400 BAD request" in {
    server.stubFor(
      post(urlEqualTo(variationPsaUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody("INVALID_PAYLOAD")
        )
    )

    logger.reset()
    connector.updatePSA(psaId.id, psaVariationData).map {
      _ =>
        logger.getLogEntries.size shouldBe 1
        logger.getLogEntries.head.level shouldBe Level.WARN
    }
  }

  it should "return a ConflictException for a 409 DUPLICATE_SUBMISSION response" in {
    server.stubFor(
      post(urlEqualTo(variationPsaUrl))
        .willReturn(
          aResponse()
            .withStatus(CONFLICT)
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("DUPLICATE_SUBMISSION"))
        )
    )
    connector.updatePSA(psaId.id, psaVariationData).map {
      response =>
        response.left.value shouldBe a[ConflictException]
        response.left.value.message should include("DUPLICATE_SUBMISSION")
    }
  }

  it should behave like errorHandlerForPostApiFailures(
    connector.updatePSA(psaId.id, psaVariationData),
    variationPsaUrl
  )

}

object DesConnectorSpec extends JsonFileReader {
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")
  private val registerPsaData = readJsonFromFile("/data/validPsaRequest.json")
  private val psaVariationData = readJsonFromFile("/data/validPsaVariationRequest.json")
  private val psaSubscriptionData = readJsonFromFile("/data/validPSASubscriptionDetails.json")
  private val invalidPsaSubscriptionResponse = readJsonFromFile("/data/validPsaRequest.json")

  val srn = SchemeReferenceNumber("S0987654321")
  val psaId = PsaId("A7654321")
  val pstr: String = PSTR("123456789AB")
  val removalDate: LocalDate = LocalDate.now()

  private val removePsaData: JsValue = Json.obj("ceaseDate" -> removalDate.toString)
  private val removePsaDataModel: PsaToBeRemovedFromScheme = PsaToBeRemovedFromScheme(psaId.id, pstr, removalDate)
  private val deregisterPsaData: JsValue = Json.obj("deregistrationDate" -> LocalDate.now(), "reason" -> "1")

  val registerPsaUrl = "/pension-online/subscription"
  val psaSubscriptionDetailsUrl = s"/pension-online/psa-subscription-details/$psaId"
  val removePsaUrl = s"/pension-online/cease-psa/psaid/$psaId/pstr/$pstr"
  val deregisterPsaUrl = s"/pension-online/deregistration/psaid/$psaId"
  val variationPsaUrl = s"/pension-online/psa-variation/psaid/$psaId"

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
