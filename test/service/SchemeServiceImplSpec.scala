/*
 * Copyright 2025 HM Revenue & Customs
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

package service

import audit.*
import connectors.DesConnector
import models.PensionSchemeAdministrator
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import repositories.MinimalDetailsCacheRepository
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.FakeDesConnector

import scala.concurrent.Future

class SchemeServiceImplSpec extends AsyncFlatSpec with Matchers with EitherValues with MockitoSugar with BeforeAndAfterEach {

  import SchemeServiceImplSpec.*
  import utils.FakeDesConnector.*

  override def beforeEach(): Unit = {
    reset(minimalDetailsCacheRepository)
  }

  "registerPSA" should "return the result from the connector" in {

    val schemeService: SchemeService = app.injector.instanceOf[SchemeServiceImpl]

    schemeService.registerPSA(psaJson).map {
      httpResponse =>
        httpResponse.value.shouldBe(registerPsaResponseJson)
    }

  }

  it should "throw BadRequestException if the JSON cannot be parsed as PensionSchemeAdministrator" in {

    val schemeService: SchemeService = app.injector.instanceOf[SchemeService]

    recoverToSucceededIf[BadRequestException] {
      schemeService.registerPSA(Json.obj())
    }

  }

  it should "send an audit event on success" in {

    val schemeService: SchemeService = app.injector.instanceOf[SchemeService]
    val requestJson = registerPsaRequestJson(psaJson)

    schemeService.registerPSA(psaJson).map {
      httpResponse =>
        fakeAuditService.lastEvent.shouldBe(
          Some(
            PSASubscription(
              existingUser = false,
              legalStatus = "test-legal-status",
              status = Status.OK,
              request = requestJson,
              response = Some(httpResponse.value)
            )
          )
        )
    }

  }

  it should "send an audit event on failure" in {

    val schemeService: SchemeService = app.injector.instanceOf[SchemeService]
    val requestJson = registerPsaRequestJson(psaJson)

    fakeSchemeConnector.setRegisterPsaResponse(Future.successful(Left(new BadRequestException("bad request"))))

    schemeService.registerPSA(psaJson).map {
      _ =>
        fakeAuditService.lastEvent.shouldBe(
          Some(
            PSASubscription(
              existingUser = false,
              legalStatus = "test-legal-status",
              status = Status.BAD_REQUEST,
              request = requestJson,
              response = None
            )
          )
        )
    }

  }

  "updatePSA" should "return the result from the connector" in {
    when(minimalDetailsCacheRepository.remove(ArgumentMatchers.eq(psaId))(using any())).thenReturn(Future.successful(true))

    val schemeService: SchemeService = app.injector.instanceOf[SchemeService]

    schemeService.updatePSA(psaId, psaJson).map {
      httpResponse =>
        verify(minimalDetailsCacheRepository, times(1)).remove(ArgumentMatchers.eq(psaId))(using any())
        httpResponse.value.shouldBe(updatePsaResponseJson)
    }

  }

  it should "throw BadRequestException if the JSON cannot be parsed as PensionSchemeAdministrator" in {

    val schemeService: SchemeService = app.injector.instanceOf[SchemeService]
    recoverToExceptionIf[BadRequestException] {
      schemeService.updatePSA(psaId, Json.obj())
    }.map { _ =>
      verify(minimalDetailsCacheRepository, never()).remove(any())(using any())
      assert(true)
    }

  }

  it should "send an audit event on success" in {

    when(minimalDetailsCacheRepository.remove(ArgumentMatchers.eq(psaId))(using any())).thenReturn(Future.successful(true))

    val schemeService: SchemeService = app.injector.instanceOf[SchemeService]

    val requestJson = updatePsaRequestJson(psaJson)

    schemeService.updatePSA(psaId, psaJson).map {
      httpResponse =>
        verify(minimalDetailsCacheRepository, times(1)).remove(ArgumentMatchers.eq(psaId))(using any())
        fakeAuditService.lastEvent.shouldBe(
          Some(
            PSAChanges(
              legalStatus = "test-legal-status",
              status = Status.OK,
              request = requestJson,
              response = Some(httpResponse.value)
            )
          )
        )
    }

  }

  it should "send an audit event on failure" in {

    val schemeService: SchemeService = app.injector.instanceOf[SchemeService]
    val requestJson = updatePsaRequestJson(psaJson)

    fakeSchemeConnector.setUpdatePsaResponse(Future.successful(Left(new BadRequestException("bad request"))))

    schemeService.updatePSA(psaId, psaJson).map {
      _ =>
        verify(minimalDetailsCacheRepository, never()).remove(any())(using any())
        fakeAuditService.lastEvent.shouldBe(
          Some(
            PSAChanges(
              legalStatus = "test-legal-status",
              status = Status.BAD_REQUEST,
              request = requestJson,
              response = None
            )
          )
        )
    }
  }
}

object SchemeServiceImplSpec extends MockitoSugar {

  val fakeSchemeConnector: FakeDesConnector = new FakeDesConnector()
  val minimalDetailsCacheRepository: MinimalDetailsCacheRepository = mock[MinimalDetailsCacheRepository]
  val fakeAuditService: StubSuccessfulAuditService = new StubSuccessfulAuditService()

  private val app = new GuiceApplicationBuilder()
    .overrides(
      bind[MinimalDetailsCacheRepository].toInstance(minimalDetailsCacheRepository),
      bind[DesConnector].toInstance(fakeSchemeConnector),
      bind[AuditService].toInstance(fakeAuditService)
    )
    .build()

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  val psaId: String = "test-psa-id"

  val psaJson: JsValue = Json.obj(
    "registrationInfo" -> Json.obj(
      "legalStatus" -> "test-legal-status",
      "sapNumber" -> "test-sap-number",
      "noIdentifier" -> false,
      "customerType" -> "test-customer-type"
    ),
    "individualContactDetails" -> Json.obj(
      "phone" -> "test-phone",
      "email" -> "test-email"
    ),
    "individualContactAddress" -> Json.obj(
      "addressLine1" -> "test-address-line-1",
      "countryCode" -> "GB",
      "postalCode" -> "test-postal-code"
    ),
    "individualAddressYears" -> "test-individual-address-years",
    "existingPSA" -> Json.obj(
      "isExistingPSA" -> false
    ),
    "individualDetails" -> Json.obj(
      "firstName" -> "test-first-name",
      "lastName" -> "test-last-name"
    ),
    "individualDateOfBirth" -> "2000-01-01",
    "declaration" -> true,
    "declarationWorkingKnowledge" -> "test-declaration-working-knowledge",
    "declarationFitAndProper" -> true
  )

  def registerPsaRequestJson(userAnswersJson: JsValue): JsValue = {
    val psa = userAnswersJson.as[PensionSchemeAdministrator](using PensionSchemeAdministrator.apiReads)
    val requestJson = Json.toJson(psa)(using PensionSchemeAdministrator.psaSubmissionWrites)
    requestJson
  }

  def updatePsaRequestJson(userAnswersJson: JsValue): JsValue = {
    val psa = userAnswersJson.as[PensionSchemeAdministrator](using PensionSchemeAdministrator.apiReads)
    val requestJson = Json.toJson(psa)(using PensionSchemeAdministrator.psaUpdateWrites)
    requestJson
  }

}
