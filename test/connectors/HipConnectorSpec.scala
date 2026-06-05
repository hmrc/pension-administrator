/*
 * Copyright 2026 HM Revenue & Customs
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

import audit.SchemeAuditService
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock.*
import connectors.helper.PSASubscriptionFixture
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, RecoverMethods}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.*
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc.{ControllerComponents, RequestHeader}
import play.api.test.FakeRequest
import play.api.{Application, inject}
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.{ConflictException, ForbiddenException, HeaderCarrier, HttpException, UnprocessableEntityException}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class HipConnectorSpec
  extends AsyncFlatSpec
    with WireMockSupport
    with Matchers
    with EitherValues
    with ScalaFutures
    with IntegrationPatience
    with RecoverMethods
    with JsonFileReader
    with MockitoSugar {

  private val mockSchemeAuditService: SchemeAuditService = mock[SchemeAuditService]

  implicit lazy val app: Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.hip-hod.host" -> wireMockHost,
        "microservice.services.hip-hod.port" -> wireMockPort,
      )
      .overrides(bind[SchemeAuditService].toInstance(mockSchemeAuditService))
      .build()

  val hipPsaSubscriptionUrl = "/etmp/RESTAdapter/psa/subscription"

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")
  private implicit val ec: ExecutionContext = app.injector.instanceOf[ControllerComponents].executionContext

  lazy val connector: HipConnector = app.injector.instanceOf[HipConnector]

  val registerPsaResponseJson: JsValue =
    Json.obj(
      "processingDate"   -> LocalDate.now,
      "formBundleNumber" -> "000020000000",
      "psaId"            -> "A2123456"
    )

  val variationResponse: JsValue =
    Json.obj(
      "processingDate"   -> LocalDate.now,
      "formBundleNumber" -> "000020000000",
    )

  val psaSubscriptionDetails: JsValue =
    readJsonFromFile("/data/validPSASubscriptionDetails.json")

  override def beforeEach(): Unit = {
    reset(mockSchemeAuditService)
  }

  "HipConnector GET" should "return PSA details" in {
    wireMockServer.stubFor(
      get(urlEqualTo(s"$hipPsaSubscriptionUrl/A123456"))
        .willReturn(ok(Json.stringify(Json.obj("success" -> psaSubscriptionDetails))))
    )

    val result = connector.getPSASubscriptionDetails("A123456").futureValue

    verify(mockSchemeAuditService, times(1)).sendPSADetailsEvent(any())(any())

    result.isRight.shouldBe(true)
  }

  it should "throw JsResultException if invalid data returned from HIP" in {
    wireMockServer.stubFor(
      get(urlEqualTo(s"$hipPsaSubscriptionUrl/A123456"))
        .willReturn(ok(Json.stringify(Json.obj("key" -> "value"))))
    )

    recoverToExceptionIf[JsResultException](connector.getPSASubscriptionDetails("A123456")).map {
      ex =>
        ex.shouldBe(a[JsResultException])
    }
  }

  it should "throw JsResultException if PSA data cannot be transformed to UA" in {
    wireMockServer.stubFor(
      get(urlEqualTo(s"$hipPsaSubscriptionUrl/A123456"))
        .willReturn(ok(Json.stringify(Json.obj("success" -> "value"))))
    )

    recoverToExceptionIf[JsResultException](connector.getPSASubscriptionDetails("A123456")).map {
      ex =>
        ex.shouldBe(a[JsResultException])
    }
  }

  it should "return 409 CONFLICT for 422 from HIP with error code 004" in {
    wireMockServer.stubFor(
      get(urlEqualTo(s"$hipPsaSubscriptionUrl/A123456"))
        .willReturn(aResponse()
          .withStatus(UNPROCESSABLE_ENTITY)
          .withBody(Json.stringify(Json.obj("errors" -> Json.obj("code" -> "004")))))
    )

    val result = connector.getPSASubscriptionDetails("A123456").futureValue

    verify(mockSchemeAuditService, times(1)).sendPSADetailsEvent(any())(any())

    result.isLeft.shouldBe(true)
    result.left.value.responseCode.shouldBe(CONFLICT)
  }

  it should "return 422 UNPROCESSABLE_ENTITY for 422 from HIP with other error code than 004" in {
    wireMockServer.stubFor(
      get(urlEqualTo(s"$hipPsaSubscriptionUrl/A123456"))
        .willReturn(aResponse()
          .withStatus(UNPROCESSABLE_ENTITY)
          .withBody(Json.stringify(Json.obj("errors" -> Json.obj("code" -> "003")))))
    )

    val result = connector.getPSASubscriptionDetails("A123456").futureValue

    verify(mockSchemeAuditService, times(1)).sendPSADetailsEvent(any())(any())

    result.left.value.responseCode.shouldBe(UNPROCESSABLE_ENTITY)
  }

  it should "return 400 BAD_REQUEST for 422 from HIP with error code 046" in {
    wireMockServer.stubFor(
      get(urlEqualTo(s"$hipPsaSubscriptionUrl/invalid"))
        .willReturn(aResponse()
          .withStatus(UNPROCESSABLE_ENTITY)
          .withBody(Json.stringify(Json.obj("errors" -> Json.obj("code" -> "046")))))
    )

    val result = connector.getPSASubscriptionDetails("invalid").futureValue

    verify(mockSchemeAuditService, times(1)).sendPSADetailsEvent(any())(any())

    result.left.value.responseCode.shouldBe(BAD_REQUEST)
  }

  it should "return HttpException for any other error from HIP" in {
    wireMockServer.stubFor(
      get(urlEqualTo(s"$hipPsaSubscriptionUrl/A123456"))
        .willReturn(aResponse()
          .withStatus(INTERNAL_SERVER_ERROR)
          .withBody(Json.stringify(Json.obj("key" -> "value"))))
    )

    val result = connector.getPSASubscriptionDetails("A123456").futureValue

    verify(mockSchemeAuditService, times(1)).sendPSADetailsEvent(any())(any())

    result.left.value.responseCode.shouldBe(INTERNAL_SERVER_ERROR)
    result.left.value.message.shouldBe("""{"key":"value"}""")
  }

  "HipConnector POST" should "create subscription" in {
    wireMockServer.stubFor(
      post(urlEqualTo(hipPsaSubscriptionUrl))
        .withRequestBody(equalToJson(Json.stringify(PSASubscriptionFixture.registerPSAValidPayload)))
        .willReturn(created().withBody(Json.stringify(Json.obj("success" -> registerPsaResponseJson))))
    )

    connector
      .registerPSA(PSASubscriptionFixture.registerPSAValidPayload)
      .futureValue
      .value
      .shouldBe(registerPsaResponseJson)
  }

  it should "return HttpException with response body and status from HIP if invalid data returned" in {
    wireMockServer.stubFor(
      post(urlEqualTo(hipPsaSubscriptionUrl))
        .withRequestBody(equalToJson(Json.stringify(PSASubscriptionFixture.registerPSAValidPayload)))
        .willReturn(created().withBody(Json.stringify(Json.obj("key" -> "value"))))
    )

    val result = connector.registerPSA(PSASubscriptionFixture.registerPSAValidPayload).futureValue

    result.left.value.shouldBe(a[HttpException])
    result.left.value.responseCode.shouldBe(CREATED)
    result.left.value.message.shouldBe("""{"key":"value"}""")
  }

  it should "return 409 CONFLICT for 422 from HIP with error code 004" in {
    wireMockServer.stubFor(
      post(urlEqualTo(hipPsaSubscriptionUrl))
        .withRequestBody(equalToJson(Json.stringify(PSASubscriptionFixture.registerPSAValidPayload)))
        .willReturn(aResponse()
          .withStatus(UNPROCESSABLE_ENTITY)
          .withBody(Json.stringify(Json.obj("errors" -> Json.obj("code" -> "004")))))
    )

    val result = connector.registerPSA(PSASubscriptionFixture.registerPSAValidPayload).futureValue

    result.left.value.shouldBe(a[ConflictException])
    result.left.value.responseCode.shouldBe(CONFLICT)
  }

  it should "return 422 UNPROCESSABLE_ENTITY for 422 from HIP with other error code than 004" in {
    wireMockServer.stubFor(
      post(urlEqualTo(hipPsaSubscriptionUrl))
        .withRequestBody(equalToJson(Json.stringify(PSASubscriptionFixture.registerPSAValidPayload)))
        .willReturn(aResponse()
          .withStatus(UNPROCESSABLE_ENTITY)
          .withBody(Json.stringify(Json.obj("errors" -> Json.obj("code" -> "003")))))
    )

    val result = connector.registerPSA(PSASubscriptionFixture.registerPSAValidPayload).futureValue

    result.left.value.shouldBe(a[UnprocessableEntityException])
    result.left.value.responseCode.shouldBe(UNPROCESSABLE_ENTITY)
  }

  it should "return 403 FORBIDDEN for 422 from HIP with error code 007" in {
    wireMockServer.stubFor(
      post(urlEqualTo(hipPsaSubscriptionUrl))
        .withRequestBody(equalToJson(Json.stringify(PSASubscriptionFixture.registerPSAValidPayload)))
        .willReturn(aResponse()
          .withStatus(UNPROCESSABLE_ENTITY)
          .withBody(Json.stringify(Json.obj("errors" -> Json.obj("code" -> "007")))))
    )

    val result = connector.registerPSA(PSASubscriptionFixture.registerPSAValidPayload).futureValue

    result.left.value.shouldBe(a[ForbiddenException])
    result.left.value.responseCode.shouldBe(FORBIDDEN)
    result.left.value.message.shouldBe("PSA_ACTIVE_RELATIONSHIP_EXISTS")
  }

  it should "return HttpException for any other error from HIP" in {
    wireMockServer.stubFor(
      post(urlEqualTo(hipPsaSubscriptionUrl))
        .withRequestBody(equalToJson(Json.stringify(PSASubscriptionFixture.registerPSAValidPayload)))
        .willReturn(aResponse()
          .withStatus(INTERNAL_SERVER_ERROR)
          .withBody(Json.stringify(Json.obj("key" -> "value"))))
    )

    val result = connector.registerPSA(PSASubscriptionFixture.registerPSAValidPayload).futureValue

    result.left.value.shouldBe(a[HttpException])
    result.left.value.responseCode.shouldBe(INTERNAL_SERVER_ERROR)
    result.left.value.message.shouldBe("""{"key":"value"}""")
  }

  "HipConnector PUT" should "update subscription" in {
    wireMockServer.stubFor(
      put(urlEqualTo(s"$hipPsaSubscriptionUrl/A123456"))
        .withRequestBody(equalToJson(Json.stringify(PSASubscriptionFixture.psaVariation)))
        .willReturn(ok(Json.stringify(Json.obj("success" -> variationResponse))))
    )

    connector
      .updatePSA("A123456", PSASubscriptionFixture.psaVariation)
      .futureValue
      .value
      .shouldBe(variationResponse)
  }

  it should "return HttpException with response body and status from HIP if invalid data returned" in {
    wireMockServer.stubFor(
      put(urlEqualTo(s"$hipPsaSubscriptionUrl/A123456"))
        .withRequestBody(equalToJson(Json.stringify(PSASubscriptionFixture.psaVariation)))
        .willReturn(ok(Json.stringify(Json.obj("key" -> "value"))))
    )

    val result = connector.updatePSA("A123456", PSASubscriptionFixture.psaVariation).futureValue

    result.left.value.shouldBe(a[HttpException])
    result.left.value.responseCode.shouldBe(OK)
    result.left.value.message.shouldBe("""{"key":"value"}""")
  }

  it should "return 409 CONFLICT for 422 from HIP with error code 004" in {
    wireMockServer.stubFor(
      put(urlEqualTo(s"$hipPsaSubscriptionUrl/A123456"))
        .withRequestBody(equalToJson(Json.stringify(PSASubscriptionFixture.psaVariation)))
        .willReturn(aResponse()
          .withStatus(UNPROCESSABLE_ENTITY)
          .withBody(Json.stringify(Json.obj("errors" -> Json.obj("code" -> "004")))))
    )

    val result = connector.updatePSA("A123456", PSASubscriptionFixture.psaVariation).futureValue

    result.left.value.shouldBe(a[ConflictException])
    result.left.value.responseCode.shouldBe(CONFLICT)
  }

  it should "return 422 UNPROCESSABLE_ENTITY for 422 from HIP with other error code than 004" in {
    wireMockServer.stubFor(
      put(urlEqualTo(s"$hipPsaSubscriptionUrl/A123456"))
        .withRequestBody(equalToJson(Json.stringify(PSASubscriptionFixture.psaVariation)))
        .willReturn(aResponse()
          .withStatus(UNPROCESSABLE_ENTITY)
          .withBody(Json.stringify(Json.obj("errors" -> Json.obj("code" -> "003")))))
    )

    val result = connector.updatePSA("A123456", PSASubscriptionFixture.psaVariation).futureValue

    result.left.value.shouldBe(a[UnprocessableEntityException])
    result.left.value.responseCode.shouldBe(UNPROCESSABLE_ENTITY)
  }

  it should "return 403 FORBIDDEN for 422 from HIP with error code 007" in {
    wireMockServer.stubFor(
      put(urlEqualTo(s"$hipPsaSubscriptionUrl/A123456"))
        .withRequestBody(equalToJson(Json.stringify(PSASubscriptionFixture.psaVariation)))
        .willReturn(aResponse()
          .withStatus(UNPROCESSABLE_ENTITY)
          .withBody(Json.stringify(Json.obj("errors" -> Json.obj("code" -> "007")))))
    )

    val result = connector.updatePSA("A123456", PSASubscriptionFixture.psaVariation).futureValue

    result.left.value.shouldBe(a[ForbiddenException])
    result.left.value.responseCode.shouldBe(FORBIDDEN)
    result.left.value.message.shouldBe("PSA_ACTIVE_RELATIONSHIP_EXISTS")
  }

  it should "return HttpException with response body and status for any other error from HIP" in {
    wireMockServer.stubFor(
      put(urlEqualTo(s"$hipPsaSubscriptionUrl/A123456"))
        .withRequestBody(equalToJson(Json.stringify(PSASubscriptionFixture.psaVariation)))
        .willReturn(aResponse()
          .withStatus(INTERNAL_SERVER_ERROR)
          .withBody(Json.stringify(Json.obj("key" -> "value"))))
    )

    val result = connector.updatePSA("A123456", PSASubscriptionFixture.psaVariation).futureValue

    result.left.value.shouldBe(a[HttpException])
    result.left.value.responseCode.shouldBe(INTERNAL_SERVER_ERROR)
    result.left.value.message.shouldBe("""{"key":"value"}""")
  }
}

