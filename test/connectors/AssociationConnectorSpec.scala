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

import audit.testdoubles.StubSuccessfulAuditService
import audit.{AuditService, InvitationAcceptanceAuditEvent, MinimalPSADetails}
import com.github.tomakehurst.wiremock.client.WireMock._
import config.AppConfig
import connectors.RegistrationConnectorSpec.auditService
import connectors.helper.ConnectorBehaviours
import models._
import org.scalatest._
import org.slf4j.event.Level
import play.api.LoggerLike
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http._
import utils.{StubLogger, WireMockHelper}

class AssociationConnectorSpec extends AsyncFlatSpec
  with Matchers
  with WireMockHelper
  with OptionValues
  with EitherValues
  with ConnectorBehaviours {

  import AssociationConnectorSpec._

  private implicit val rh: RequestHeader = FakeRequest("", "")
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val logger = new StubLogger()
  private val psaMinimalDetailsUrl = s"/pension-online/psa-min-details/$psaId"
  private val acceptInvitationUrl = s"/pension-online/psa-association/pstr/$pstr"

  override def beforeEach(): Unit = {
    auditService.reset()
    super.beforeEach()
  }

  override protected def portConfigKey: String = "microservice.services.des-hod.port"

  private val auditService = new StubSuccessfulAuditService()

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(auditService),
      bind[LoggerLike].toInstance(logger)
    )

  private lazy val connector = injector.instanceOf[AssociationConnector]
  private lazy val config = injector.instanceOf[AppConfig]

  private def stubServer(responseJson: String, status: Int) = {
    server.stubFor(
      post(urlEqualTo(acceptInvitationUrl))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/json")
            .withBody(responseJson)
        )
    )
  }

  "AssociationConnector" should "return OK (200) with a JSON payload" in {

    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          ok(psaMinimunIndividualDetailPayload.toString())
            .withHeader("Content-Type", "application/json")
        )
    )

    connector.getPSAMinimalDetails(psaId).map { response =>
      response.right.value shouldBe psaMinimalDetailsIndividualUser
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

    connector.getPSAMinimalDetails(psaId).map { response =>
      response.left.value shouldBe a[BadRequestException]
      response.left.value.message shouldBe "INVALID PAYLOAD"
    }
  }

  it should "return bad request - 400 if body contains INVALID_PSAID" in {

    val errorResponse =
      """{
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

    connector.getPSAMinimalDetails(psaId).map { response =>
      response.left.value shouldBe a[BadRequestException]
      response.left.value.message shouldBe Json.parse(errorResponse).toString()
    }

  }

  it should behave like errorHandlerForGetApiFailures(
    connector.getPSAMinimalDetails(psaId),
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

    recoverToExceptionIf[Upstream5xxResponse](connector.getPSAMinimalDetails(psaId)) map {
      ex =>
        ex.upstreamResponseCode shouldBe INTERNAL_SERVER_ERROR
        ex.getMessage should startWith("PSA minimal details")
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

    recoverToExceptionIf[Exception](connector.getPSAMinimalDetails(psaId)) map {
      ex =>
        ex.getMessage should startWith("PSA minimal details")
        ex.getMessage should include("failed with status")
    }
  }

  it should "send a GetMinPSADetails audit event on success" in {

    server.stubFor(
      get(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          ok(psaMinimunIndividualDetailPayload.toString())
            .withHeader("Content-Type", "application/json")
        )
    )

    connector.getPSAMinimalDetails(psaId).map { response =>
      auditService.verifySent(
        MinimalPSADetails(
          psaId = psaId.id,
          status = OK,
          response = Some(Json.toJson(psaMinimalDetailsIndividualUser))
        )
      ) shouldBe true

    }
  }

  it should "send a GetMinPSADetails audit event on not found" in {

    server.stubFor(
      post(urlEqualTo(psaMinimalDetailsUrl))
        .willReturn(
          notFound
        )
    )

    connector.getPSAMinimalDetails(psaId).map { response =>
      auditService.verifySent(
        MinimalPSADetails(
          psaId = psaId.id,
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

    recoverToExceptionIf[Upstream5xxResponse](connector.getPSAMinimalDetails(psaId)) map {
      _ =>
        auditService.verifyNothingSent shouldBe true
    }

  }

  "acceptInvitation" should "check headers" in {
    val correlationId = hc.requestId.map(_.value).getOrElse("test-correlation-id")
    server.stubFor(
      post(urlEqualTo(acceptInvitationUrl))
        .withHeader("Environment", equalTo(config.desEnvironment))
        .withHeader("Content-Type", equalTo("application/json"))
        .withHeader("CorrelationId", matching("^.+$"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withHeader("CorrelationId", correlationId)
            .withBody(successResponseJson)
        )
    )
    val acceptedInvitation = AcceptedInvitation(pstr = pstr, inviteePsaId = psaIdInvitee, declaration = true, declarationDuties = false, inviterPsaId = psaId,
      pensionAdviserDetails = Some(pensionAdviserDetailUK))

    connector.acceptInvitation(acceptedInvitation).map(_.isRight shouldBe true)
  }

  it should "correctly submit and handle a valid request for a UK address" in {
    server.stubFor(
      post(urlEqualTo(acceptInvitationUrl))
        .withRequestBody(equalToJson(psaAssociationDetailsjsonUKAddress))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(successResponseJson)
        )
    )

    val acceptedInvitation = AcceptedInvitation(pstr = pstr, inviteePsaId = psaIdInvitee, declaration = true, declarationDuties = false, inviterPsaId = psaId,
      pensionAdviserDetails = Some(pensionAdviserDetailUK))

    connector.acceptInvitation(acceptedInvitation).map(_.isRight shouldBe true)
  }

  it should "correctly send an audit event for a valid request" in {
    server.stubFor(
      post(urlEqualTo(acceptInvitationUrl))
        .withRequestBody(equalToJson(psaAssociationDetailsjsonUKAddress))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(successResponseJson)
        )
    )

    val acceptedInvitation = AcceptedInvitation(pstr = pstr, inviteePsaId = psaIdInvitee, declaration = true, declarationDuties = false, inviterPsaId = psaId,
      pensionAdviserDetails = Some(pensionAdviserDetailUK))

    connector.acceptInvitation(acceptedInvitation).map{ response =>

      response.isRight shouldBe true
      auditService.verifySent(InvitationAcceptanceAuditEvent(acceptedInvitation, OK, Some(Json.parse(successResponseJson)))) should equal(true)

    }
  }

  it should "correctly submit and handle a valid request for a non-UK address" in {
    server.stubFor(
      post(urlEqualTo(acceptInvitationUrl))
        .withRequestBody(equalToJson(psaAssociationDetailsjsonInternationalAddress))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(successResponseJson)
        )
    )

    val acceptedInvitation = AcceptedInvitation(pstr = pstr, inviteePsaId = psaIdInvitee, declaration = true, declarationDuties = false, inviterPsaId = psaId,
      pensionAdviserDetails = Some(pensionAdviserDetailInternational))

    connector.acceptInvitation(acceptedInvitation).map(_.isRight shouldBe true)
  }

  it should "correctly submit and handle a valid request where there is no advisor" in {
    server.stubFor(
      post(urlEqualTo(acceptInvitationUrl))
        .withRequestBody(equalToJson(psaAssociationDetailsjsonNoAdvisor))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(successResponseJson)
        )
    )

    val acceptedInvitation = AcceptedInvitation(pstr = pstr, inviteePsaId = psaIdInvitee, declaration = true, declarationDuties = true, inviterPsaId = psaId,
      pensionAdviserDetails = None)

    connector.acceptInvitation(acceptedInvitation).map(_.isRight shouldBe true)
  }

  it should "return ConflictException for a 403 active relationship exists response" in {
    stubServer("""{ "code":"ACTIVE_RELATIONSHIP_EXISTS"}""", FORBIDDEN)
    val acceptedInvitation = AcceptedInvitation(pstr = pstr, inviteePsaId = psaIdInvitee, declaration = true, declarationDuties = true, inviterPsaId = psaId,
      pensionAdviserDetails = None)

    connector.acceptInvitation(acceptedInvitation).collect {
      case Left(_: ConflictException) => succeed
    }
  }

  it should "return not found exception and failure response details for a 404 response" in {
    server.stubFor(
      post(urlEqualTo(acceptInvitationUrl))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withHeader("Content-Type", "application/json")
            .withBody(failureResponseJson.toString)
        )
    )
    val acceptedInvitation = AcceptedInvitation(pstr = pstr, inviteePsaId = psaIdInvitee, declaration = true, declarationDuties = true, inviterPsaId = psaId,
      pensionAdviserDetails = None)

    connector.acceptInvitation(acceptedInvitation).collect {
      case Left(_: NotFoundException) => succeed
    }
  }

  it should "return not found exception for a 404 response where no response details" in {
    server.stubFor(
      post(urlEqualTo(acceptInvitationUrl))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withHeader("Content-Type", "application/json")
        )
    )
    val acceptedInvitation = AcceptedInvitation(pstr = pstr, inviteePsaId = psaIdInvitee, declaration = true, declarationDuties = true, inviterPsaId = psaId,
      pensionAdviserDetails = None)

    connector.acceptInvitation(acceptedInvitation).collect {
      case Left(_: NotFoundException) => succeed
    }
  }

  it should "send an audit event with no response for a 404 response" in {
    server.stubFor(
      post(urlEqualTo(acceptInvitationUrl))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withHeader("Content-Type", "application/json")
        )
    )
    val acceptedInvitation = AcceptedInvitation(pstr = pstr, inviteePsaId = psaIdInvitee, declaration = true, declarationDuties = true, inviterPsaId = psaId,
      pensionAdviserDetails = None)

    connector.acceptInvitation(acceptedInvitation).map { _ =>
      auditService.verifySent(InvitationAcceptanceAuditEvent(acceptedInvitation, NOT_FOUND, None)) should equal(true)
    }
  }

  it should "return bad request exception for a 404 invalid payload response" in {
    stubServer("""{ "code":"INVALID_PAYLOAD"}""", BAD_REQUEST)
    val acceptedInvitation = AcceptedInvitation(pstr = pstr, inviteePsaId = psaIdInvitee, declaration = true, declarationDuties = true, inviterPsaId = psaId,
      pensionAdviserDetails = None)

    connector.acceptInvitation(acceptedInvitation).collect {
      case Left(_: BadRequestException) => succeed
    }
  }

  it should "log validation failures for a 404 invalid payload response" in {
    stubServer("""{ "code":"INVALID_PAYLOAD"}""", BAD_REQUEST)
    val acceptedInvitation = AcceptedInvitation(pstr = pstr, inviteePsaId = psaIdInvitee, declaration = true, declarationDuties = true, inviterPsaId = psaId,
      pensionAdviserDetails = None)

    logger.reset()
    connector.acceptInvitation(acceptedInvitation).map { _ =>
      logger.getLogEntries.size shouldBe 1
      logger.getLogEntries.head.level shouldBe Level.WARN
    }
  }

  it should "return BadRequestException for 403 INVALID_INVITER_PSAID responses" in {
    stubServer("""{ "code":"INVALID_INVITER_PSAID"}""", FORBIDDEN)
    val acceptedInvitation = AcceptedInvitation(pstr = pstr, inviteePsaId = psaIdInvitee, declaration = true, declarationDuties = true, inviterPsaId = psaId,
      pensionAdviserDetails = None)

    connector.acceptInvitation(acceptedInvitation).collect {
      case Left(e: BadRequestException) if e.message.contains("INVALID_INVITER_PSAID") => succeed
    }
  }

  it should "return BadRequestException for 403 INVALID_INVITEE_PSAID responses" in {
    stubServer("""{ "code":"INVALID_INVITEE_PSAID"}""", FORBIDDEN)
    val acceptedInvitation = AcceptedInvitation(pstr = pstr, inviteePsaId = psaIdInvitee, declaration = true, declarationDuties = true, inviterPsaId = psaId,
      pensionAdviserDetails = None)

    connector.acceptInvitation(acceptedInvitation).collect {
      case Left(e: BadRequestException) if e.message.contains("INVALID_INVITEE_PSAID") => succeed
    }
  }
}

object AssociationConnectorSpec extends OptionValues {

  private val psaIdInvitee = PsaId("A2123456")
  private val psaId = PsaId("A2123456")
  private val email = "aaa@aaa.com"
  private val ukAddress = UkAddress(
    addressLine1 = "address line 1",
    addressLine2 = Some("address line 2"),
    addressLine3 = Some("address line 3"),
    addressLine4 = Some("address line 4"), countryCode = "GB", postalCode = "ZZ11ZZ"
  )
  private val internationalAddress = InternationalAddress(
    addressLine1 = "address line 1",
    addressLine2 = Some("address line 2"),
    addressLine3 = Some("address line 3"),
    addressLine4 = Some("address line 4"), countryCode = "FR", postalCode = Some("ZZ11ZZ")
  )
  private val pensionAdvisorName = "pension advisor 1"
  private val pensionAdviserDetailUK = PensionAdviserDetails(name = pensionAdvisorName, addressDetail = ukAddress, email = email)
  private val pensionAdviserDetailInternational = PensionAdviserDetails(
    name = pensionAdvisorName, addressDetail = internationalAddress, email = email)
  private val pstr = "scheme"

  private val psaAssociationDetailsjsonUKAddress =
    s"""{
       |   "psaAssociationDetails":{
       |      "psaAssociationIDsDetails":{
       |         "inviteePSAID":"$psaIdInvitee",
       |         "inviterPSAID":"$psaId"
       |      },
       |      "declarationDetails":{
       |         "box1":true,
       |         "box2":true,
       |         "box3":true,
       |         "box4":true,
       |         "box6":true,
       |         "pensionAdviserDetails":{
       |            "name":"$pensionAdvisorName",
       |            "addressDetails":{
       |               "nonUKAddress":false,
       |               "line1":"${ukAddress.addressLine1}",
       |               "line2":"${ukAddress.addressLine2.value}",
       |               "line3":"${ukAddress.addressLine3.value}",
       |               "line4":"${ukAddress.addressLine4.value}",
       |               "postalCode":"${ukAddress.postalCode}",
       |               "countryCode":"${ukAddress.countryCode}"
       |            },
       |            "contactDetails":{
       |               "email":"${email}"
       |            }
       |         }
       |      }
       |   }
       |}""".stripMargin

  private val psaAssociationDetailsjsonInternationalAddress =
    s"""{
       |   "psaAssociationDetails":{
       |      "psaAssociationIDsDetails":{
       |         "inviteePSAID":"$psaIdInvitee",
       |         "inviterPSAID":"$psaId"
       |      },
       |      "declarationDetails":{
       |         "box1":true,
       |         "box2":true,
       |         "box3":true,
       |         "box4":true,
       |         "box6":true,
       |         "pensionAdviserDetails":{
       |            "name":"$pensionAdvisorName",
       |            "addressDetails":{
       |               "nonUKAddress":true,
       |               "line1":"${internationalAddress.addressLine1}",
       |               "line2":"${internationalAddress.addressLine2.value}",
       |               "line3":"${internationalAddress.addressLine3.value}",
       |               "line4":"${internationalAddress.addressLine4.value}",
       |               "postalCode":"${internationalAddress.postalCode.value}",
       |               "countryCode":"${internationalAddress.countryCode}"
       |            },
       |            "contactDetails":{
       |               "email":"${email}"
       |            }
       |         }
       |      }
       |   }
       |}""".stripMargin

  private val psaAssociationDetailsjsonNoAdvisor =
    s"""{
       |   "psaAssociationDetails":{
       |      "psaAssociationIDsDetails":{
       |         "inviteePSAID":"$psaIdInvitee",
       |         "inviterPSAID":"$psaId"
       |      },
       |      "declarationDetails":{
       |         "box1":true,
       |         "box2":true,
       |         "box3":true,
       |         "box4":true,
       |         "box5":true
       |      }
       |   }
       |}""".stripMargin

  private val successResponseJson =
    """{
      |	"processingDate": "2001-12-17T09:30:47Z",
      |	"formBundleNumber": "12345678912"
      |}""".stripMargin

  private val failureResponseJson =
    """{
      |	"failureDate": "2001-12-17T09:30:47Z",
      |	"failureDetails": "bla"
      |}""".stripMargin

  private val psaMinimunIndividualDetailPayload = Json.parse(
    """{
      |	"processingDate": "2001-12-17T09:30:47Z",
      |	"psaMinimalDetails": {
      |		"individualDetails": {
      |			"firstName": "testFirst",
      |			"middleName": "testMiddle",
      |			"lastName": "testLast"
      |		}
      |	},
      |	"email": "test@email.com",
      |	"psaSuspensionFlag": true
      |}""".stripMargin)

  val psaMinimalDetailsIndividualUser = PSAMinimalDetails(
    "test@email.com",
    isPsaSuspended = true,
    None,
    Some(IndividualDetails(
      "testFirst",
      Some("testMiddle"),
      "testLast"
    ))
  )
}
