/*
 * Copyright 2020 HM Revenue & Customs
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

import audit.{PSAChanges, PSASubscription, SchemeAuditService, StubSuccessfulAuditService}
import base.SpecBase
import models.PensionSchemeAdministrator
import org.scalatest.{AsyncFlatSpec, EitherValues, Matchers}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.FakeDesConnector

import scala.concurrent.Future

class SchemeServiceImplSpec extends AsyncFlatSpec with Matchers with EitherValues {

  import SchemeServiceImplSpec._
  import utils.FakeDesConnector._

  "registerPSA" should "return the result from the connector" in {

    val fixture = testFixture()

    fixture.schemeService.registerPSA(psaJson).map {
      httpResponse =>
        httpResponse.right.value shouldBe registerPsaResponseJson
    }

  }

  it should "throw BadRequestException if the JSON cannot be parsed as PensionSchemeAdministrator" in {

    val fixture = testFixture()

    recoverToSucceededIf[BadRequestException] {
      fixture.schemeService.registerPSA(Json.obj())
    }

  }

  it should "send an audit event on success" in {

    val fixture = testFixture()
    val requestJson = registerPsaRequestJson(psaJson)

    fixture.schemeService.registerPSA(psaJson).map {
      httpResponse =>
        fixture.auditService.lastEvent shouldBe
          Some(
            PSASubscription(
              existingUser = false,
              legalStatus = "test-legal-status",
              status = Status.OK,
              request = requestJson,
              response = Some(httpResponse.right.value)
            )
          )
    }

  }

  it should "send an audit event on failure" in {

    val fixture = testFixture()
    val requestJson = registerPsaRequestJson(psaJson)

    fixture.schemeConnector.setRegisterPsaResponse(Future.successful(Left(new BadRequestException("bad request"))))

    fixture.schemeService.registerPSA(psaJson).map {
      _ =>
        fixture.auditService.lastEvent shouldBe
          Some(
            PSASubscription(
              existingUser = false,
              legalStatus = "test-legal-status",
              status = Status.BAD_REQUEST,
              request = requestJson,
              response = None
            )
          )
    }

  }

  "updatePSA" should "return the result from the connector" in {

    val fixture = testFixture()

    fixture.schemeService.updatePSA(psaId, psaJson).map {
      httpResponse =>
        httpResponse.right.value shouldBe updatePsaResponseJson
    }

  }

  it should "throw BadRequestException if the JSON cannot be parsed as PensionSchemeAdministrator" in {

    val fixture = testFixture()

    recoverToSucceededIf[BadRequestException] {
      fixture.schemeService.updatePSA(psaId, Json.obj())
    }

  }

  it should "send an audit event on success" in {

    val fixture = testFixture()

    val requestJson = updatePsaRequestJson(psaJson)

    fixture.schemeService.updatePSA(psaId, psaJson).map {
      httpResponse =>
        fixture.auditService.lastEvent shouldBe
          Some(
            PSAChanges(
              legalStatus = "test-legal-status",
              status = Status.OK,
              request = requestJson,
              response = Some(httpResponse.right.value)
            )
          )
    }

  }

  it should "send an audit event on failure" in {

    val fixture = testFixture()
    val requestJson = updatePsaRequestJson(psaJson)

    fixture.schemeConnector.setUpdatePsaResponse(Future.successful(Left(new BadRequestException("bad request"))))

    fixture.schemeService.updatePSA(psaId, psaJson).map {
      _ =>
        fixture.auditService.lastEvent shouldBe
          Some(
            PSAChanges(
              legalStatus = "test-legal-status",
              status = Status.BAD_REQUEST,
              request = requestJson,
              response = None
            )
          )
    }

  }

}

object SchemeServiceImplSpec extends SpecBase {

  trait TestFixture {
    val schemeConnector: FakeDesConnector = new FakeDesConnector()
    val auditService: StubSuccessfulAuditService = new StubSuccessfulAuditService()
    val schemeAuditService: SchemeAuditService = new SchemeAuditService()
    val schemeService: SchemeServiceImpl = new SchemeServiceImpl(schemeConnector, auditService, schemeAuditService) {
    }
  }

  def testFixture(): TestFixture = new TestFixture {}

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
    val psa = userAnswersJson.as[PensionSchemeAdministrator](PensionSchemeAdministrator.apiReads)
    val requestJson = Json.toJson(psa)(PensionSchemeAdministrator.psaSubmissionWrites)
    requestJson
  }

  def updatePsaRequestJson(userAnswersJson: JsValue): JsValue = {
    val psa = userAnswersJson.as[PensionSchemeAdministrator](PensionSchemeAdministrator.apiReads)
    val requestJson = Json.toJson(psa)(PensionSchemeAdministrator.psaUpdateWrites)
    requestJson
  }

}

