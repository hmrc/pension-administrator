/*
 * Copyright 2021 HM Revenue & Customs
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

import audit._
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.DesConnectorSpec.{psaId, pstr, removalDate}
import connectors.helper.ConnectorBehaviours
import models.{PSTR, PsaToBeRemovedFromScheme, SchemeReferenceNumber}
import org.joda.time.LocalDate
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.{EitherValues, OptionValues, RecoverMethods}
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json._
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers.NOT_FOUND
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.{BadRequestException, _}
import utils.{FakeDesConnector, WireMockHelper}

class DesConnectorSpec extends AsyncFlatSpec
  with WireMockHelper
  with OptionValues
  with RecoverMethods
  with EitherValues
  with ConnectorBehaviours with MockitoSugar {

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")

  override def beforeEach(): Unit = {
    auditService.reset()
    super.beforeEach()
  }
  private def errorResponse(code: String): String =
    Json.stringify(
      Json.obj(
        "code" -> code,
        "reason" -> "test-reason"
      )
    )
  private val ceasePsaData: JsValue = Json.obj(
    "ceaseIDType" -> "PSAID",
    "ceaseNumber" -> psaId.id,
    "initiatedIDType" -> "PSAID",
    "initiatedIDNumber" -> psaId.id,
    "ceaseDate" -> removalDate.toString
  )
  private val removePsaDataModel: PsaToBeRemovedFromScheme = PsaToBeRemovedFromScheme(psaId.id, pstr, removalDate)
  val ceasePsaFromSchemeUrl = s"/pension-online/cease-scheme/pods/$pstr"

  val auditService = new StubSuccessfulAuditService()

  override protected def portConfigKey: String = "microservice.services.if-hod.port"

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(auditService)
    )

  lazy val connector: DesConnector = injector.instanceOf[DesConnector]

  "Cease PSA with toggle on" should "handle OK (200)" in {
    val successResponse = FakeDesConnector.removePsaResponseJson
    server.stubFor(
      post(urlEqualTo(ceasePsaFromSchemeUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(equalToJson(Json.stringify(ceasePsaData)))
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
      post(urlEqualTo(ceasePsaFromSchemeUrl))
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
      post(urlEqualTo(ceasePsaFromSchemeUrl))
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
      post(urlEqualTo(ceasePsaFromSchemeUrl))
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
      post(urlEqualTo(ceasePsaFromSchemeUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody("INVALID_PAYLOAD")
        )
    )


    connector.removePSA(removePsaDataModel).map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_PAYLOAD")
    }
  }

  it should "return a ForbiddenException for a 403 NO_RELATIONSHIP_EXISTS response" in {
    server.stubFor(
      post(urlEqualTo(ceasePsaFromSchemeUrl))
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
      post(urlEqualTo(ceasePsaFromSchemeUrl))
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
      post(urlEqualTo(ceasePsaFromSchemeUrl))
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
      post(urlEqualTo(ceasePsaFromSchemeUrl))
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
      post(urlEqualTo(ceasePsaFromSchemeUrl))
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
      post(urlEqualTo(ceasePsaFromSchemeUrl))
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
}

object DesConnectorSpec extends JsonFileReader {
  val srn: SchemeReferenceNumber = SchemeReferenceNumber("S0987654321")
  val psaId: PsaId = PsaId("A7654321")
  val pstr: String = PSTR("123456789AB")
  val removalDate: LocalDate = LocalDate.now()

  val registerPsaUrl = "/pension-online/subscription"
  val psaSubscriptionDetailsUrl = s"/pension-online/psa-subscription-details/$psaId"
  val removePsaUrl = s"/pension-online/cease-psa/psaid/$psaId/pstr/$pstr"
  val ceasePsaFromSchemeUrl = s"/pension-online/cease-scheme/pods/$pstr"
  val deregisterPsaUrl = s"/pension-online/deregistration/psaid/$psaId"
  val variationPsaUrl = s"/pension-online/psa-variation/psaid/$psaId"

  val auditService = new StubSuccessfulAuditService()
}
