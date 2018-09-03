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
import audit.{AuditService, PSARegistration}
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.helper.ConnectorBehaviours
import models._
import org.scalatest.{AsyncFlatSpec, EitherValues, Matchers}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http._
import utils.WireMockHelper

class RegistrationConnectorPostSpec extends AsyncFlatSpec
  with JsonFileReader
  with Matchers
  with WireMockHelper
  with EitherValues
  with ConnectorBehaviours {

  import RegistrationConnectorPostSpec._

  override def beforeEach(): Unit = {
    auditService.reset()
    super.beforeEach()
  }

  override protected def portConfigKey: String = "microservice.services.des-hod.port"

  override protected def bindings: Seq[GuiceableModule] = Seq[GuiceableModule](
    bind(classOf[AuditService]).toInstance(auditService)
  )

  def connector: RegistrationConnector = app.injector.instanceOf[RegistrationConnector]

  "registerWithIdIndividual" should "handle OK (200)" in {

    server.stubFor(
      post(urlEqualTo(registerIndividualWithIdUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(registerIndividualResponse.toString())
        )
    )

    connector.registerWithIdIndividual(testNino, testIndividual, testRegisterDataIndividual).map {
      response =>
        response.right.value shouldBe registerIndividualResponse
    }

  }

  it should "handle BAD_REQUEST (400) - INVALID_NINO" in {

    server.stubFor(
      post(urlEqualTo(registerIndividualWithIdUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_NINO"))
        )
    )

    connector.registerWithIdIndividual(testNino, testIndividual, testRegisterDataIndividual) map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_NINO")
    }

  }

  it should "handle CONFLICT (409)" in {
    server.stubFor(
      post(urlEqualTo(registerIndividualWithIdUrl))
        .willReturn(
          aResponse()
            .withStatus(CONFLICT)
        )
    )
    connector.registerWithIdIndividual(testNino, testIndividual, testRegisterDataIndividual).map {
      response =>
        response.left.value shouldBe a[ConflictException]
    }
  }

  it should behave like errorHandlerForPostApiFailures(
    connector.registerWithIdIndividual(testNino, testIndividual, testRegisterDataIndividual),
    registerIndividualWithIdUrl
  )

  it should "send a PSARegistration audit event on success" in {

    server.stubFor(
      post(urlEqualTo(registerIndividualWithIdUrl))
        .withRequestBody(equalToJson(Json.stringify(testRegisterDataIndividual)))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(registerIndividualResponse.toString())
        )
    )

    connector.registerWithIdIndividual(testNino, testIndividual, testRegisterDataIndividual) map {
      _ =>
        auditService.verifySent(
          PSARegistration(
            withId = true,
            externalId = testIndividual.externalId,
            psaType = "Individual",
            found = true,
            isUk = Some(true),
            status = OK,
            request = testRegisterDataIndividual,
            response = Some(registerIndividualResponse)
          )
        ) shouldBe true
    }
  }

  it should "send a PSARegistration audit event on not found" in {

    server.stubFor(
      post(urlEqualTo(registerIndividualWithIdUrl))
        .willReturn(
          notFound
        )
    )

    connector.registerWithIdIndividual(testNino, testIndividual, testRegisterDataIndividual) map {
      _ =>
        auditService.verifySent(
          PSARegistration(
            withId = true,
            externalId = testIndividual.externalId,
            psaType = "Individual",
            found = false,
            isUk = None,
            status = NOT_FOUND,
            request = testRegisterDataIndividual,
            response = None
          )
        ) shouldBe true
    }
  }

  it should "not send a PSARegistration audit event on failure" in {

    val invalidData = Json.obj("data" -> "invalid")
    val failureResponse = Json.obj(
      "code" -> "INVALID_PAYLOAD",
      "reason" -> "Submission has not passed validation. Invalid PAYLOAD"
    )

    server.stubFor(
      post(urlEqualTo(registerIndividualWithIdUrl))
        .willReturn(
          serverError
            .withBody(failureResponse.toString)
        )
    )

    recoverToExceptionIf[Upstream5xxResponse](connector.registerWithIdIndividual(testNino, testIndividual, invalidData)) map {
      _ =>
        auditService.verifyNothingSent shouldBe true
    }

  }

  "registerWithIdOrganisation" should "handle OK (200)" in {

    server.stubFor(
      post(urlEqualTo(registerOrganisationWithIdUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(registerOrganisationResponse.toString())
        )
    )

    connector.registerWithIdOrganisation(testUtr, testOrganisation, testRegisterDataOrganisation).map {
      response =>
        response.right.value shouldBe registerOrganisationResponse
    }

  }

  it should "handle BAD_REQUEST (400) - INVALID_UTR" in {

    server.stubFor(
      post(urlEqualTo(registerOrganisationWithIdUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_UTR"))
        )
    )

    connector.registerWithIdOrganisation(testUtr, testOrganisation, testRegisterDataOrganisation) map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_UTR")
    }

  }

  it should "handle CONFLICT (409)" in {
    server.stubFor(
      post(urlEqualTo(registerOrganisationWithIdUrl))
        .willReturn(
          aResponse()
            .withStatus(CONFLICT)
        )
    )
    connector.registerWithIdOrganisation(testUtr, testOrganisation, testRegisterDataOrganisation).map {
      response =>
        response.left.value shouldBe a[ConflictException]
    }
  }

  it should behave like errorHandlerForPostApiFailures(
    connector.registerWithIdOrganisation(testUtr, testOrganisation, testRegisterDataOrganisation),
    registerOrganisationWithIdUrl
  )

  it should "send a PSARegistration audit event on success" in {

    server.stubFor(
      post(urlEqualTo(registerOrganisationWithIdUrl))
        .withRequestBody(equalToJson(Json.stringify(testRegisterDataOrganisation)))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(registerOrganisationResponse.toString())
        )
    )

    connector.registerWithIdOrganisation(testUtr, testOrganisation, testRegisterDataOrganisation) map {
      _ =>
        auditService.verifySent(
          PSARegistration(
            withId = true,
            externalId = testOrganisation.externalId,
            psaType = psaType,
            found = true,
            isUk = Some(true),
            status = OK,
            request = testRegisterDataOrganisation,
            response = Some(registerOrganisationResponse)
          )
        ) shouldBe true
    }
  }

  it should "send a PSARegistration audit event on not found" in {

    server.stubFor(
      post(urlEqualTo(registerOrganisationWithIdUrl))
        .willReturn(
          notFound
        )
    )

    connector.registerWithIdOrganisation(testUtr, testOrganisation, testRegisterDataOrganisation) map {
      _ =>
        auditService.verifySent(
          PSARegistration(
            withId = true,
            externalId = testOrganisation.externalId,
            psaType = psaType,
            found = false,
            isUk = None,
            status = NOT_FOUND,
            request = testRegisterDataOrganisation,
            response = None
          )
        ) shouldBe true
    }
  }

  it should "not send a PSARegistration audit event on failure" in {

    val invalidData = Json.obj("data" -> "invalid")

    server.stubFor(
      post(urlEqualTo(registerOrganisationWithIdUrl))
        .willReturn(
          serverError
        )
    )

    recoverToExceptionIf[Upstream5xxResponse](connector.registerWithIdOrganisation(testUtr, testOrganisation, invalidData)) map {
      _ =>
        auditService.verifyNothingSent shouldBe true
    }

  }

  "registrationNoIdOrganisation" should "handle OK (200)" in {

    server.stubFor(
      post(urlEqualTo(registerOrganisationWithoutIdUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(registerOrganisationWithoutIdResponse.toString())
        )
    )

    connector.registrationNoIdOrganisation(testOrganisation, organisationRegistrant).map {
      response =>
        response.right.value shouldBe registerOrganisationWithoutIdResponse
    }

  }

  it should "handle FORBIDDEN (403) - INVALID_SUBMISSION" in {

    server.stubFor(
      post(urlEqualTo(registerOrganisationWithoutIdUrl))
        .willReturn(
          forbidden
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_SUBMISSION"))
        )
    )

    connector.registrationNoIdOrganisation(testOrganisation, organisationRegistrant) map {
      response =>
        response.left.value shouldBe a[ForbiddenException]
        response.left.value.message should include("INVALID_SUBMISSION")
    }

  }

  it should "handle CONFLICT (409)" in {
    server.stubFor(
      post(urlEqualTo(registerOrganisationWithoutIdUrl))
        .willReturn(
          aResponse()
            .withStatus(CONFLICT)
        )
    )
    connector.registrationNoIdOrganisation(testOrganisation, organisationRegistrant).map {
      response =>
        response.left.value shouldBe a[ConflictException]
    }
  }

  it should behave like errorHandlerForPostApiFailures(
    connector.registrationNoIdOrganisation(testOrganisation, organisationRegistrant),
    registerOrganisationWithoutIdUrl
  )

  it should "send a PSARegistration audit event on success" in {

    server.stubFor(
      post(urlEqualTo(registerOrganisationWithoutIdUrl))
        .withRequestBody(equalToJson(Json.stringify(testRegisterDataOrganisationWithoutId)))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(registerOrganisationWithoutIdResponse.toString)
        )
    )

    connector.registrationNoIdOrganisation(testOrganisation, organisationRegistrant) map {
      _ =>
        auditService.verifySent(
          PSARegistration(
            withId = false,
            externalId = testOrganisation.externalId,
            psaType = "Organisation",
            found = true,
            isUk = Some(false),
            status = OK,
            request = Json.toJson(organisationRegistrant),
            response = Some(registerOrganisationWithoutIdResponse)
          )
        ) shouldBe true
    }
  }

  it should "send a PSARegistration audit event on not found" in {

    server.stubFor(
      post(urlEqualTo(registerOrganisationWithoutIdUrl))
        .willReturn(
          notFound
        )
    )

    connector.registrationNoIdOrganisation(testOrganisation, organisationRegistrant) map {
      _ =>
        auditService.verifySent(
          PSARegistration(
            withId = false,
            externalId = testOrganisation.externalId,
            psaType = "Organisation",
            found = false,
            isUk = None,
            status = NOT_FOUND,
            request = Json.toJson(organisationRegistrant),
            response = None
          )
        ) shouldBe true
    }
  }

  it should "not send a PSARegistration audit event on failure" in {

    server.stubFor(
      post(urlEqualTo(registerOrganisationWithoutIdUrl))
        .willReturn(
          serverError
        )
    )

    recoverToExceptionIf[Upstream5xxResponse](connector.registrationNoIdOrganisation(testOrganisation, organisationRegistrant)) map {
      _ =>
        auditService.verifyNothingSent shouldBe true
    }

  }

}

object RegistrationConnectorPostSpec {

  val testNino: String = "AB123456C"
  val testUtr: String = "1234567890"

  val psaType = "LLP"

  val registerIndividualWithIdUrl = s"/registration/individual/nino/$testNino"
  val registerOrganisationWithIdUrl = s"/registration/organisation/utr/$testUtr"
  val registerOrganisationWithoutIdUrl = "/registration/02.00.00/organisation"

  val testOrganisation: User = User("test-external-id", AffinityGroup.Organisation)
  val testIndividual: User = User("test-external-id", AffinityGroup.Individual)

  val testRegisterDataIndividual: JsObject = Json.obj("regime" -> "PODA", "requiresNameMatch" -> false, "isAnAgent" -> false)
  val testRegisterDataOrganisation: JsObject = Json.obj(
    "regime" -> "PODA",
    "requiresNameMatch" -> false,
    "isAnAgent" -> false,
    "organisation" -> Json.obj(
      "organisationName" -> "Test Ltd",
      "organisationType" -> "LLP"
    ))
  val testRegisterDataOrganisationWithoutId: JsObject = Json.obj(
    "regime" -> "PODA",
    "acknowledgementReference" -> "ackReference",
    "isAnAgent" -> false,
    "isAGroup" -> false,
    "organisation" -> OrganisationName("Name"),
    "address" -> Json.obj(
      "line1" -> "addressLine1",
      "countryCode" -> "US",
      "addressType" -> "NON-UK"
    ),
    "contactDetails" -> ContactDetailsType(None, None)
  )

  val organisationRegistrant = OrganisationRegistrant(
    "ackReference",
    OrganisationName("Name"),
    InternationalAddress("addressLine1", None, None, None, "US", None),
    ContactDetailsType(None, None)
  )

  val auditService = new StubSuccessfulAuditService()

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  val registerOrganisationWithoutIdResponse: JsValue = Json.obj(
    "sapNumber" -> "1234567890",
    "safeId" -> "XE0001234567890"
  )

  val registerIndividualResponse: JsValue = Json.parse(
    """
      |{
      |  "safeId": "XE0001234567890",
      |  "sapNumber": "1234567890",
      |  "agentReferenceNumber": "AARN1234567",
      |  "isEditable": true,
      |  "isAnAgent": false,
      |  "isAnASAgent": false,
      |  "isAnIndividual": true,
      |  "individual": {
      |    "firstName": "Stephen",
      |    "lastName": "Wood",
      |    "dateOfBirth": "1990-04-03"
      |  },
      |  "address": {
      |    "addressLine1": "100 SuttonStreet",
      |    "addressLine2": "Wokingham",
      |    "addressLine3": "Surrey",
      |    "addressLine4": "London",
      |    "postalCode": "DH14EJ",
      |    "countryCode": "GB"
      |  },
      |  "contactDetails": {
      |    "primaryPhoneNumber": "01332752856",
      |    "secondaryPhoneNumber": "07782565326",
      |    "faxNumber": "01332754256",
      |    "emailAddress": "stephen@manncorpone.co.uk"
      |  }
      |}
      |
    """.stripMargin)

  val registerOrganisationResponse: JsValue = Json.parse(
    s"""{
       |  "safeId": "XE0001234567890",
       |  "sapNumber": "1234567890",
       |  "isAnIndividual": false,
       |  "organisation": {
       |    "organisationName": "Test Ltd",
       |    "isAGroup": false,
       |    "organisationType": "$psaType"
       |  },
       |  "address": {
       |    "addressLine1": "100 SuttonStreet",
       |    "addressLine2": "Wokingham",
       |    "addressLine3": "Surrey",
       |    "addressLine4": "London",
       |    "postalCode": "DH14EJ",
       |    "countryCode": "GB"
       |  },
       |  "contactDetails": {
       |    "primaryPhoneNumber": "01332752856",
       |    "secondaryPhoneNumber": "07782565326",
       |    "faxNumber": "01332754256",
       |    "emailAddress": "stephen@manncorpone.co.uk"
       |  }
       |}
       |
    """.stripMargin)

  def errorResponse(code: String): String = {
    Json.obj(
      "code" -> code,
      "reason" -> s"Reason for $code"
    ).toString()
  }

}