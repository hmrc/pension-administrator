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

package service

import audit.{PSASubscription, StubSuccessfulAuditService}
import base.SpecBase
import connector.SchemeConnector
import models.PensionSchemeAdministrator
import org.joda.time.LocalDate
import org.scalatest.{AsyncFlatSpec, EitherValues, Matchers}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class SchemeServiceImplSpec extends AsyncFlatSpec with Matchers with EitherValues {

  import FakeSchemeConnector._
  import SchemeServiceImplSpec._

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

}

object SchemeServiceImplSpec extends SpecBase {

  trait TestFixture {
    val schemeConnector: FakeSchemeConnector = new FakeSchemeConnector()
    val auditService: StubSuccessfulAuditService = new StubSuccessfulAuditService()
    val schemeService: SchemeServiceImpl = new SchemeServiceImpl(schemeConnector, auditService, appConfig) {
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
      "lastName" -> "test-last-name",
      "dateOfBirth" -> "2000-01-01"
    ),
    "declaration" -> true,
    "declarationWorkingKnowledge" -> "test-declaration-working-knowledge",
    "declarationFitAndProper" -> true
  )

  def registerPsaRequestJson(userAnswersJson: JsValue): JsValue = {
    implicit val contactAddressEnabled: Boolean = true
    val psa = userAnswersJson.as[PensionSchemeAdministrator](PensionSchemeAdministrator.apiReads)
    val requestJson = Json.toJson(psa)(PensionSchemeAdministrator.psaSubmissionWrites)

    requestJson
  }

}

class FakeSchemeConnector extends SchemeConnector {

  import FakeSchemeConnector._

  private var registerPsaResponse: Future[Either[HttpException, JsValue]] = Future.successful(Right(registerPsaResponseJson))

  def setRegisterPsaResponse(response: Future[Either[HttpException, JsValue]]): Unit = this.registerPsaResponse = response

  override def registerPSA(registerData: JsValue)(implicit
                                                  headerCarrier: HeaderCarrier,
                                                  ec: ExecutionContext,
                                                  request: RequestHeader): Future[Either[HttpException, JsValue]] = registerPsaResponse

  override def getCorrelationId(requestId: Option[String]): String = "4725c81192514c069b8ff1d84659b2df"
}

object FakeSchemeConnector {

  val registerPsaResponseJson: JsValue =
    Json.obj(
      "processingDate" -> LocalDate.now,
      "formBundle" -> "1121313",
      "psaId" -> "A21999999"
    )
}
