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

import audit._
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.helper.ConnectorBehaviours
import models._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories._
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http._
import utils.WireMockHelper

class AssociationConnectorSpec extends AsyncFlatSpec
  with Matchers
  with WireMockHelper
  with OptionValues
  with EitherValues
  with ConnectorBehaviours with MockitoSugar {

  private val psaRegime = "poda"
  private val psaId = PsaId("A2123456")
  private val inviteePsaId = PsaId("A2123457")
  private val pstr = "pstr"
  private val psaType = "psaid"
  private val psaMinimunIndividualDetailPayload = Json.parse(
    """{
      |	"processingDate": "2001-12-17T09:30:47Z",
      |	"minimalDetails": {
      |		"individualDetails": {
      |			"firstName": "testFirst",
      |			"middleName": "testMiddle",
      |			"lastName": "testLast"
      |		}
      |	},
      |	"email": "test@email.com",
      |	"psaSuspensionFlag": true,
      |	"rlsFlag": true,
      |	"deceasedFlag": true
      |}""".stripMargin)

  val psaMinimalDetailsIndividualUser: MinimalDetails = MinimalDetails(
    "test@email.com",
    isPsaSuspended = true,
    None,
    Some(IndividualDetails(
      "testFirst",
      Some("testMiddle"),
      "testLast"
    )),
    rlsFlag = true,
    deceasedFlag = true
  )

  private implicit val rh: RequestHeader = FakeRequest("", "")
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val psaMinimalDetailsUrl = s"/pension-online/psa-min-details/poda/psaid/$psaId"
  private val createPsaAssociationUrl = s"/pension-online/association/pods/$pstr"

  private val adviserName = "Adviser"
  private val adviserAddress = UkAddress("line1", Some("line2"), Some("line3"), Some("line4"), "GB", "Test")
  private val adviserEmail = "a@a.c"

  override def beforeEach(): Unit = {
    auditService.reset()
    super.beforeEach()
  }

  override protected def portConfigKeys: String = "microservice.services.if-hod.port"

  private val auditService = new StubSuccessfulAuditService()

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(auditService),
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
      bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
      bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
      bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
      bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository])
    )

  private lazy val connector = injector.instanceOf[AssociationConnector]

  "acceptInvitation" should "process correctly a request including pension adviser" in {
    val pensionAdviserDetails = PensionAdviserDetails(name = adviserName, addressDetail = adviserAddress, email = adviserEmail)

    val invitation = AcceptedInvitation(
      pstr = pstr,
      inviteePsaId = inviteePsaId,
      inviterPsaId = psaId,
      declaration = true,
      declarationDuties = false,
      pensionAdviserDetails = Some(pensionAdviserDetails)
    )

    val testJsonBody = Json.obj(fields =
      "psaAssociationIDsDetails" -> Json.obj(fields =
        "inviteeIDType" -> "PSAID",
        "inviteeIDNumber" -> inviteePsaId,
        "inviterPSAID" -> psaId
      ),
      "psaDeclarationDetails" -> Json.obj(fields =
        "box1" -> true,
        "box2" -> true,
        "box3" -> true,
        "box4" -> true,
        "box6" -> true,
        "pensionAdviserDetails" -> Json.obj(fields =
          "name" -> pensionAdviserDetails.name,
          "addressDetails" -> Json.obj(fields =
            "addressLine1" -> adviserAddress.addressLine1,
            "nonUKAddress" -> "false",
            "addressLine4" -> adviserAddress.addressLine4,
            "addressLine3" -> adviserAddress.addressLine3,
            "postalCode" -> adviserAddress.postalCode,
            "countryCode" -> "GB",
            "addressLine2" -> adviserAddress.addressLine2
          ),
          "contactDetails" -> Json.obj(fields =
            "email" -> pensionAdviserDetails.email
          )
        )
      )
    )

    server.stubFor(
      post(urlEqualTo(createPsaAssociationUrl))
        .withRequestBody(equalToJson(Json.stringify(testJsonBody)))
        .willReturn(
          ok()
            .withHeader("Content-Type", "application/json")
        )
    )


    connector.acceptInvitation(invitation).map { response =>
      response.value shouldBe()
    }

  }

  "getMinimalDetails with IF toggle switched ON" should "return OK (200) with a JSON payload" in {

    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          ok(psaMinimunIndividualDetailPayload.toString())
            .withHeader("Content-Type", "application/json")
        )
    )

    connector.getMinimalDetails(psaId.id, psaType, psaRegime).map { response =>
      response.value shouldBe psaMinimalDetailsIndividualUser
    }
  }

  it should "return bad request - 400 if response body is invalid" in {
    val invalidReponse = Json.obj("response" -> "invalid response").toString()
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          ok(invalidReponse)
            .withHeader("Content-Type", "application/json")
        )
    )

    connector.getMinimalDetails(psaId.id, psaType, psaRegime).map { response =>
      response.left.value shouldBe a[BadRequestException]
      response.left.value.message shouldBe "INVALID PAYLOAD"
    }
  }

  it should "return bad request - 400 if body contains INVALID_PAYLOAD" in {

    val errorResponse =
      """{
        |	"code": "INVALID_PAYLOAD",
        |	"reason": "Submission has not passed validation. Invalid parameter idValue."
        |}""".stripMargin
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          badRequest().withBody(Json.parse(errorResponse).toString)
        )
    )

    connector.getMinimalDetails(psaId.id, psaType, psaRegime).map { response =>
      response.left.value shouldBe a[BadRequestException]
      response.left.value.message shouldBe Json.parse(errorResponse).toString()
    }
  }

  it should "return bad request - 400 if body contains INVALID_IDTYPE" in {

    val errorResponse =
      """{
        |	"code": "INVALID_IDTYPE",
        |	"reason": "Submission has not passed validation. Invalid parameter idType."
        |}""".stripMargin
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          badRequest().withBody(Json.parse(errorResponse).toString)
        )
    )

    connector.getMinimalDetails(psaId.id, psaType, psaRegime).map { response =>
      response.left.value shouldBe a[BadRequestException]
      response.left.value.message shouldBe Json.parse(errorResponse).toString()
    }
  }

  it should "return bad request - 400 if body contains INVALID_REGIME" in {

    val errorResponse =
      """{
        |	"code": "INVALID_REGIME",
        |	"reason": "Submission has not passed validation. Invalid parameter regime."
        |}""".stripMargin
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          badRequest().withBody(Json.parse(errorResponse).toString)
        )
    )

    connector.getMinimalDetails(psaId.id, psaType, psaRegime).map { response =>
      response.left.value shouldBe a[BadRequestException]
      response.left.value.message shouldBe Json.parse(errorResponse).toString()
    }
  }

  it should "return bad request - 400 if body contains INVALID_CORRELATIONID" in {

    val errorResponse =
      """{
        |	"code": "INVALID_CORRELATIONID",
        |	"reason": "Submission has not passed validation. Invalid header CorrelationId."
        |}""".stripMargin
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          badRequest().withBody(Json.parse(errorResponse).toString)
        )
    )

    connector.getMinimalDetails(psaId.id, psaType, psaRegime).map { response =>
      response.left.value shouldBe a[BadRequestException]
      response.left.value.message shouldBe Json.parse(errorResponse).toString()
    }

  }

  it should behave like errorHandlerForGetApiFailures(
    connector.getMinimalDetails(psaId.id, psaType, psaRegime),
    psaMinimalDetailsUrl
  )

  it should "throw Upstream5XX for internal server error - 500" in {
    val errorResponse =
      """{
        |	"code": "SERVER_ERROR",
        |	"reason": "DES is currently experiencing problems that require live service intervention."
        |}""".stripMargin
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          serverError().withBody(Json.parse(errorResponse).toString)
        )
    )

    recoverToExceptionIf[UpstreamErrorResponse](connector.getMinimalDetails(psaId.id, psaType, psaRegime)) map {
      ex =>
        ex.statusCode shouldBe INTERNAL_SERVER_ERROR
        ex.getMessage should startWith("Minimal details")
        ex.message should include(Json.parse(errorResponse).toString)
        ex.reportAs shouldBe BAD_GATEWAY
    }
  }

  it should "throw exception for other runtime exception" in {
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          noContent()
        )
    )

    recoverToExceptionIf[Exception](connector.getMinimalDetails(psaId.id, psaType, psaRegime)) map {
      ex =>
        ex.getMessage should startWith("Minimal details")
        ex.getMessage should include("failed with status")
    }
  }

  it should "send a GetMinDetails audit event on success" in {
    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          ok(psaMinimunIndividualDetailPayload.toString())
            .withHeader("Content-Type", "application/json")
        )
    )

    connector.getMinimalDetails(psaId.id, psaType, psaRegime).map { _ =>
      auditService.verifySent(
        MinimalDetailsEvent(
          idType = "psaid",
          idValue = psaId.id,
          name = psaMinimalDetailsIndividualUser.name,
          isSuspended = Some(psaMinimalDetailsIndividualUser.isPsaSuspended),
          rlsFlag = Some(true),
          deceasedFlag = Some(true),
          status = OK,
          response = Some(Json.toJson(psaMinimalDetailsIndividualUser))
        )
      ) shouldBe true
    }
  }

  it should "send a GetMinDetails audit event on not found" in {
    server.stubFor(
      post(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          notFound
        )
    )

    connector.getMinimalDetails(psaId.id, psaType, psaRegime).map { _ =>
      auditService.verifySent(
        MinimalDetailsEvent(
          idType = "psaid",
          idValue = psaId.id,
          name = None,
          isSuspended = None,
          rlsFlag = None,
          deceasedFlag = None,
          status = NOT_FOUND,
          response = None
        )
      ) shouldBe true
    }
  }

  it should "not send a GetMinPSADetails audit event on failure" in {
    val errorResponse =
      """{
        |	"code": "SERVER_ERROR",
        |	"reason": "DES is currently experiencing problems that require live service intervention."
        |}""".stripMargin

    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          serverError().withBody(Json.parse(errorResponse).toString)
        )
    )

    recoverToExceptionIf[UpstreamErrorResponse](connector.getMinimalDetails(psaId.id, psaType, psaRegime)) map {
      _ =>
        auditService.verifyNothingSent() shouldBe true
    }

  }
}

