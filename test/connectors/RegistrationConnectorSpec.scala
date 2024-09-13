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

import audit.{AuditService, PSARegistration, StubSuccessfulAuditService}
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.helper.{ConnectorBehaviours, HeaderUtils}
import models.registrationnoid._
import models.User
import org.mockito.Mockito.when
import org.scalatest.EitherValues
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http._
import utils.{InvalidPayloadHandler, InvalidPayloadHandlerImpl, WireMockHelper}

import java.time.LocalDate

class RegistrationConnectorSpec extends AsyncFlatSpec
  with JsonFileReader
  with Matchers
  with WireMockHelper
  with EitherValues
  with MockitoSugar
  with ConnectorBehaviours {

  import RegistrationConnectorSpec._

  private val mockHeaderUtils = mock[HeaderUtils]

  override def beforeEach(): Unit = {
    auditService.reset()
    when(mockHeaderUtils.desHeaderWithoutCorrelationId).thenReturn(Nil)
    when(mockHeaderUtils.integrationFrameworkHeader).thenReturn(Nil)
    when(mockHeaderUtils.desHeader).thenReturn(Nil)
    when(mockHeaderUtils.getCorrelationId).thenReturn(testCorrelationId)
    when(mockHeaderUtils.getCorrelationIdIF).thenReturn(testCorrelationId)
    super.beforeEach()
  }

  override protected def portConfigKeys: String = "microservice.services.des-hod.port"

  override protected def bindings: Seq[GuiceableModule] = Seq[GuiceableModule](
    bind(classOf[AuditService]).toInstance(auditService),
    bind(classOf[HeaderUtils]).toInstance(mockHeaderUtils),
    bind(classOf[InvalidPayloadHandler]).toInstance(invalidPayloadHandler),
    bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
    bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
    bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
    bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
    bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository]),
    bind[AdminDataRepository].toInstance(mock[AdminDataRepository]),
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
        response.value shouldBe registerIndividualResponse
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


    recoverToExceptionIf[UpstreamErrorResponse](connector.registerWithIdIndividual(testNino, testIndividual, testRegisterDataIndividual)) map {
      ex =>
        ex.statusCode shouldBe BAD_REQUEST
        ex.message should include("INVALID_NINO")
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

    recoverToExceptionIf[UpstreamErrorResponse](connector.registerWithIdIndividual(testNino, testIndividual, testRegisterDataIndividual)) map {
      ex =>
        ex.statusCode shouldBe CONFLICT
    }
  }

  it should behave like errorHandlerForPostApiFailures[JsValue](
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
    val thrown = intercept[RegistrationRequestValidationFailureException] {
      connector.registerWithIdIndividual(testNino, testIndividual, invalidData)
    }

    assert(thrown.getMessage === "Invalid payload for registerWithIdIndividual: " +
      "ValidationFailure(required,$: required property 'regime' not found,None)" +
      "ValidationFailure(required,$: required property 'isAnAgent' not found,None)" +
      "ValidationFailure(oneOf,$: should be valid to one and only one schema, but 0 are valid,None)" +
      "ValidationFailure(required,$: required property 'requiresNameMatch' not found,None)" +
      "ValidationFailure(required,$: required property 'organisation' not found,None)" +
      "ValidationFailure(required,$: required property 'individual' not found,None)" +
      "ValidationFailure(additionalProperties,$.data: is not defined in the schema and the schema does not allow additional properties,None)")
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
        response.value shouldBe registerOrganisationResponse
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

    recoverToExceptionIf[UpstreamErrorResponse](connector.registerWithIdOrganisation(testUtr, testOrganisation, testRegisterDataOrganisation)) map {
      ex =>

        ex.statusCode shouldBe BAD_REQUEST
        ex.message should include("INVALID_UTR")
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

    val thrown = intercept[RegistrationRequestValidationFailureException] {
      connector.registerWithIdOrganisation(testUtr, testOrganisation, invalidData)
    }

    assert(thrown.getMessage === "Invalid payload for registerWithIdOrganisation: " +
      "ValidationFailure(required,$: required property 'regime' not found,None)" +
      "ValidationFailure(required,$: required property 'isAnAgent' not found,None)" +
      "ValidationFailure(oneOf,$: should be valid to one and only one schema, but 0 are valid,None)" +
      "ValidationFailure(required,$: required property 'requiresNameMatch' not found,None)" +
      "ValidationFailure(required,$: required property 'organisation' not found,None)" +
      "ValidationFailure(required,$: required property 'individual' not found,None)" +
      "ValidationFailure(additionalProperties,$.data: is not defined in the schema and the schema does not allow additional properties,None)")

  }

  "registrationNoIdOrganisation" should "handle OK (200)" in {

    server.stubFor(
      post(urlEqualTo(registerOrganisationWithoutIdUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(registerWithoutIdResponseJson.toString())
        )
    )

    connector.registrationNoIdOrganisation(testOrganisation, organisationRegistrant).map {
      response =>
        response.value shouldBe registerWithoutIdResponseJson
    }
  }


  it should behave like errorHandlerForPostApiFailures(
    connector.registrationNoIdOrganisation(testOrganisation, organisationRegistrant),
    registerOrganisationWithoutIdUrl
  )

  it should "send a PSARegWithoutId audit event on success" in {

    server.stubFor(
      post(urlEqualTo(registerOrganisationWithoutIdUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(registerWithoutIdResponseJson.toString())
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
            request = testRegisterWithNoId,
            response = Some(registerWithoutIdResponseJson)
          )
        ) shouldBe true
    }
  }

  it should "send a PSARegWithoutId audit event on not found" in {

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
            request = testRegisterWithNoId,
            response = None
          )
        ) shouldBe true
    }
  }

  it should "not send a PSARegWithoutId audit event on failure" in {

    server.stubFor(
      post(urlEqualTo(registerOrganisationWithoutIdUrl))
        .willReturn(
          serverError
        )
    )

    recoverToExceptionIf[UpstreamErrorResponse](connector.registrationNoIdOrganisation(testOrganisation, organisationRegistrant)) map {
      _ =>
        auditService.verifyNothingSent() shouldBe true
    }
  }

  "registrationNoIdIndividual" should "return a success response on receiving a 200 Ok" in {

    server
      .stubFor(
        post(urlEqualTo(registerIndividualWithoutIdUrl))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.stringify(registerIndividualWithoutIdRequestJson)))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(registerWithoutIdResponseJson))
          )
      )

    connector.registrationNoIdIndividual(testIndividual, registerIndividualWithoutIdRequest) map {
      response =>
        response.value shouldBe registerWithoutIdResponseJson
    }

  }

  it should "handle 400 Bad Request INVALID_PAYLOAD" in {

    server
      .stubFor(
        post(urlEqualTo(registerIndividualWithoutIdUrl))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody(errorResponse("INVALID_PAYLOAD"))
          )
      )

    connector.registrationNoIdIndividual(testIndividual, registerIndividualWithoutIdRequest) map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_PAYLOAD")
    }

  }

  it should "return 400 Bad Request INVALID_SUBMISSION when DES returns 403 Forbidden INVALID_SUBMISSION" in {

    server
      .stubFor(
        post(urlEqualTo(registerIndividualWithoutIdUrl))
          .willReturn(
            aResponse()
              .withStatus(FORBIDDEN)
              .withBody(errorResponse("INVALID_SUBMISSION"))
          )
      )

    connector.registrationNoIdIndividual(testIndividual, registerIndividualWithoutIdRequest) map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_SUBMISSION")
    }

  }

  it should "return 502 Bad Gateway when DES returns a 500 response" in {

    server
      .stubFor(
        post(urlEqualTo(registerIndividualWithoutIdUrl))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

    recoverToExceptionIf[UpstreamErrorResponse](connector.registrationNoIdIndividual(testIndividual, registerIndividualWithoutIdRequest)) map {
      ex =>
        ex.reportAs shouldBe BAD_GATEWAY
    }

  }

  it should "return 502 Bad Gateway when DES returns a 503 response" in {

    server
      .stubFor(
        post(urlEqualTo(registerIndividualWithoutIdUrl))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
          )
      )

    recoverToExceptionIf[UpstreamErrorResponse](connector.registrationNoIdIndividual(testIndividual, registerIndividualWithoutIdRequest)) map {
      ex =>
        ex.reportAs shouldBe BAD_GATEWAY
    }

  }

  it should "send a PSARegWithoutId audit event on success" in {

    server
      .stubFor(
        post(urlEqualTo(registerIndividualWithoutIdUrl))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.stringify(registerIndividualWithoutIdRequestJson)))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.stringify(registerWithoutIdResponseJson))
          )
      )

    connector.registrationNoIdIndividual(testIndividual, registerIndividualWithoutIdRequest) map {
      _ =>
        auditService.verifySent(
          PSARegistration(
            withId = false,
            externalId = testIndividual.externalId,
            psaType = "Individual",
            found = true,
            isUk = Some(false),
            status = OK,
            request = Json.toJson(registerIndividualWithoutIdRequest),
            response = Some(registerWithoutIdResponseJson)
          )
        ) shouldBe true
    }
  }

  it should "not send a PSARegWithoutId audit event on failure" in {

    server
      .stubFor(
        post(urlEqualTo(registerIndividualWithoutIdUrl))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

    recoverToExceptionIf[UpstreamErrorResponse](connector.registrationNoIdIndividual(testIndividual, registerIndividualWithoutIdRequest)) map {
      ex =>
        ex.reportAs shouldBe BAD_GATEWAY
        auditService.verifyNothingSent() shouldBe true
    }
  }

}

// scalastyle:off magic.number

object RegistrationConnectorSpec {

  val testNino: String = "AB123456C"
  val testUtr: String = "1234567890"

  val psaType = "LLP"

  val registerIndividualWithIdUrl = s"/registration/individual/nino/$testNino"
  val registerOrganisationWithIdUrl = s"/registration/organisation/utr/$testUtr"
  val registerOrganisationWithoutIdUrl = "/registration/02.00.00/organisation"
  val registerIndividualWithoutIdUrl = "/registration/02.00.00/individual"

  val testOrganisation: User = User("test-external-id", AffinityGroup.Organisation)
  val testIndividual: User = User("test-external-id", AffinityGroup.Individual)
  val testCorrelationId = "testCorrelationId"

  val testRegisterDataIndividual: JsObject = Json.obj("regime" -> "PODA", "requiresNameMatch" -> false, "isAnAgent" -> false)
  val testRegisterDataOrganisation: JsObject = Json.obj(
    "regime" -> "PODA",
    "requiresNameMatch" -> true,
    "isAnAgent" -> false,
    "organisation" -> Json.obj(
      "organisationName" -> "Test Ltd",
      "organisationType" -> "LLP"
    ))

  val testRegisterWithNoId: JsObject = Json.obj(
    "regime" -> "PODA",
    "acknowledgementReference" -> testCorrelationId,
    "isAnAgent" -> false,
    "isAGroup" -> false,
    "contactDetails" -> Json.obj(
      "phoneNumber" -> JsNull,
      "mobileNumber" -> JsNull,
      "faxNumber" -> JsNull,
      "emailAddress" -> JsNull
    ),
    "organisation" -> Json.obj(
      "organisationName" -> "Name"
    ),
    "address" -> Json.obj(
      "addressLine1" -> "addressLine1",
      "addressLine2" -> "addressLine2",
      "countryCode" -> "US"
    ),
    "contactDetails" -> Json.obj(
      "phoneNumber" -> JsNull, "mobileNumber" -> JsNull, "faxNumber" -> JsNull, "emailAddress" -> JsNull
    ))

  val organisationRegistrant: OrganisationRegistrant = OrganisationRegistrant(
    OrganisationName("Name"),
    Address("addressLine1", "addressLine2", None, None, None, "US")
  )

  val auditService = new StubSuccessfulAuditService()
  val invalidPayloadHandler = new InvalidPayloadHandlerImpl()

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  val registerWithoutIdResponse: RegisterWithoutIdResponse = RegisterWithoutIdResponse(
    "XE0001234567890",
    "1234567890",
    LocalDate.of(2024, 4, 3)
  )


//  val registerWithoutIdResponse: JsValue = Json.parse(
//    """
//      |{
//      |  "safeId": "XE0001234567890",
//      |  "sapNumber": "1234567890",
//      |  "processingDate": "2024-04-03",
//      |}
//    """.stripMargin)

  val registerWithoutIdResponseJson: JsValue = Json.toJson(registerWithoutIdResponse)

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
       |  "isEditable": false,
       |  "sapNumber": "1234567890",
       |  "isAnIndividual": false,
       |  "isAnAgent": false,
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

  val registerIndividualWithoutIdRequest: RegistrationNoIdIndividualRequest =
    RegistrationNoIdIndividualRequest(
      "test-first-name",
      "test-last-name",
      LocalDate.of(2000, 1, 1),
      Address(
        "test-address-line-1",
        "test-address-line-2",
        None,
        None,
        None,
        "AD"
      )
    )

  val registerIndividualWithoutIdRequestJson: JsValue =
    Json.toJson(registerIndividualWithoutIdRequest)(RegistrationNoIdIndividualRequest.writesRegistrationNoIdIndividualRequest(testCorrelationId))

}
