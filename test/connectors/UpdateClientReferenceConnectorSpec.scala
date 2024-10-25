/*
 * Copyright 2024 HM Revenue & Customs
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

import audit.{AuditService, StubSuccessfulAuditService, UpdateClientReferenceAuditEvent}
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.helper.{ConnectorBehaviours, HeaderUtils}
import models.{UpdateClientReferenceRequest, User}
import org.mockito.Mockito._
import org.scalatest.EitherValues
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http._
import utils.WireMockHelper

import java.time.LocalDate

class UpdateClientReferenceConnectorSpec extends AsyncFlatSpec
  with JsonFileReader
  with Matchers
  with WireMockHelper
  with EitherValues
  with MockitoSugar
  with ConnectorBehaviours {

  import UpdateClientReferenceConnectorSpec._

  private val mockHeaderUtils = mock[HeaderUtils]

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
      bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
      bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
      bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
      bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository]),
      bind[AdminDataRepository].toInstance(mock[AdminDataRepository]),
      bind(classOf[HeaderUtils]).toInstance(mockHeaderUtils),
      bind[AuditService].toInstance(auditService)
    )

  override def beforeEach(): Unit = {
    when(mockHeaderUtils.integrationFrameworkHeader).thenReturn(Nil)
    when(mockHeaderUtils.desHeader).thenReturn(Nil)
    when(mockHeaderUtils.getCorrelationId).thenReturn(testCorrelationId)
    when(mockHeaderUtils.getCorrelationIdIF).thenReturn(testCorrelationId)
    super.beforeEach()
  }

  override protected def portConfigKeys: String = "microservice.services.if-hod.port"

  def connector: UpdateClientReferenceConnector = app.injector.instanceOf[UpdateClientReferenceConnector]

  "updateClientReference" should "handle OK (200)" in {

    server.stubFor(
      put(urlEqualTo(updateClientReferenceUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(successResponse.toString())
        )
    )

    connector.updateClientReference(testUpdateClientReference, None).map {
      response =>
        response.value shouldBe successResponse
        val expectedAuditEvent = UpdateClientReferenceAuditEvent(testUpdateClientReference, Some(successResponse), OK, None)
        auditService.verifySent(expectedAuditEvent) shouldBe true
    }

  }

  it should "handle validation failure" in {

    server.stubFor(
      put(urlEqualTo(updateClientReferenceUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(successResponse.toString())
        )
    )

    val x = intercept[UpdateClientRefValidationFailureException] {
      connector.updateClientReference(testUpdateClientReferenceInvalid, None)
    }
    assert(x.getMessage === "Invalid payload when updateClientReference :-\nValidationFailure(pattern,$.identifierDetails.pstr: " +
      "does not match the regex pattern ^[0-9]{8}[A-Z]{2}$,None)ValidationFailure(pattern,$.identifierDetails.psaId: " +
      "does not match the regex pattern ^A[0-9]{7}$,None)ValidationFailure(pattern,$.identifierDetails.pspId: " +
      "does not match the regex pattern ^[0-2]{1}[0-9]{7}$,None)")
  }

  it should "handle BAD_REQUEST (400)" in {

    server.stubFor(
      put(urlEqualTo(updateClientReferenceUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_PSTR"))
        )
    )


    recoverToExceptionIf[UpstreamErrorResponse](connector.updateClientReference(testUpdateClientReference, None)) map {
      ex =>
        ex.statusCode shouldBe BAD_REQUEST
        ex.message should include("INVALID_PSTR")
    }
  }


  it should behave like errorHandlerForPutApiFailures(
    connector.updateClientReference(testUpdateClientReference, None),
    updateClientReferenceUrl
  )

}

object UpdateClientReferenceConnectorSpec {

  val updateClientReferenceUrl = "/pension-online/update-client-reference/pods"
  val auditService = new StubSuccessfulAuditService()

  val testOrganisation: User = User("test-external-id", AffinityGroup.Organisation)
  val testIndividual: User = User("test-external-id", AffinityGroup.Individual)
  val testCorrelationId = "testCorrelationId"
  val testUpdateClientReference: UpdateClientReferenceRequest = UpdateClientReferenceRequest(
    pstr = "45554528AV", psaId = "A3869826", pspId = "11542640", clientReference = Some("as1234aasda"))

  val testUpdateClientReferenceInvalid: UpdateClientReferenceRequest = UpdateClientReferenceRequest(
    pstr = "45554528A", psaId = "A38698", pspId = "115426", clientReference = Some("as1234aasdaaaaaaaaaaaa"))


  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  val successResponse: JsValue = Json.obj(
    "status" -> "OK",
    "statusText" -> "Hello there!",
    "processingDate" -> LocalDate.now().toString()
  )


  def errorResponse(code: String): String = {
    Json.obj(
      "code" -> code,
      "reason" -> s"Reason for $code"
    ).toString()
  }
}



