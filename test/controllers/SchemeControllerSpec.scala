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

package controllers

import base.JsonFileReader
import org.joda.time.LocalDate
import org.scalatest.{AsyncFlatSpec, MustMatchers}
import play.api.http.Status.BAD_GATEWAY
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import service.SchemeService
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

class SchemeControllerSpec extends AsyncFlatSpec with JsonFileReader with MustMatchers {

  import SchemeControllerSpec._

  private val validRequestData = readJsonFromFile("/data/validPsaRequest.json")

  "registerPSA" should "return OK when service returns successfully" in {

    val result = controller.registerPSA(fakeRequest.withJsonBody(validRequestData))

    status(result) mustBe OK
    contentAsJson(result) mustBe registerPsaResponseJson
  }

  it should "return BAD_REQUEST when service returns BAD_REQUEST" in {

    fakeSchemeService.setRegisterPsaResponse(
      Future.successful(Left(new BadRequestException("bad request")))
    )

    val result = controller.registerPSA(fakeRequest.withJsonBody(validRequestData))

    status(result) mustBe BAD_REQUEST
    contentAsString(result) mustBe "bad request"
  }

  it should "return CONFLICT when service returns CONFLICT" in {

    fakeSchemeService.setRegisterPsaResponse(
      Future.successful(Left(new ConflictException("conflict")))
    )

    val result = controller.registerPSA(fakeRequest.withJsonBody(validRequestData))

    status(result) mustBe CONFLICT
    contentAsString(result) mustBe "conflict"
  }

  it should "return NOT_FOUND when service returns NOT_FOUND" in {

    fakeSchemeService.setRegisterPsaResponse(
      Future.successful(Left(new NotFoundException("not found")))
    )

    val result = controller.registerPSA(fakeRequest.withJsonBody(validRequestData))

    status(result) mustBe NOT_FOUND
    contentAsString(result) mustBe "not found"
  }

  it should "return Forbidden when service return Forbidden" in {

    fakeSchemeService.setRegisterPsaResponse(
      Future.successful(Left(new ForbiddenException("forbidden")))
    )

    val result = controller.registerPSA(fakeRequest.withJsonBody(validRequestData))

    status(result) mustBe FORBIDDEN
    contentAsString(result) mustBe "forbidden"
  }

  it should "throw BadRequestException when no data recieved in the request" in {

    recoverToSucceededIf[BadRequestException] {
      controller.registerPSA(fakeRequest)
    }
  }

  it should "throw BadRequestException when service throws JsResultException" in {

    fakeSchemeService.setRegisterPsaResponse(Future.failed(JsResultException(Nil)))

    recoverToSucceededIf[BadRequestException] {
      controller.registerPSA(fakeRequest.withJsonBody(validRequestData))
    }
  }

  it should "throw Upstream5xxResponse when service throws Upstream5xxResponse" in {

    fakeSchemeService.setRegisterPsaResponse(Future.failed(Upstream5xxResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))

    recoverToSucceededIf[Upstream5xxResponse] {
      controller.registerPSA(fakeRequest.withJsonBody(validRequestData))
    }
  }

  it should "throw UpStream4xxResponse when service throws UpStream4xxResponse" in {

    fakeSchemeService.setRegisterPsaResponse(Future.failed(Upstream4xxResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))

    recoverToSucceededIf[Upstream4xxResponse] {
      controller.registerPSA(fakeRequest.withJsonBody(validRequestData))
    }
  }

  it should "throw Exception when service throws any unknown Exception" in {

    fakeSchemeService.setRegisterPsaResponse(Future.failed(new Exception("Unknown Exception")))

    recoverToSucceededIf[Exception] {
      controller.registerPSA(fakeRequest.withJsonBody(validRequestData))
    }
  }
}

object SchemeControllerSpec {
  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  class FakeSchemeService extends SchemeService {

    private var registerPsaResponse: Future[Either[HttpException, JsValue]] = Future.successful(Right(registerPsaResponseJson))

    def setRegisterPsaResponse(response: Future[Either[HttpException, JsValue]]): Unit = this.registerPsaResponse = response

    override def registerPSA(json: JsValue)(implicit headerCarrier: HeaderCarrier,
                                            ec: ExecutionContext,
                                            request: RequestHeader): Future[Either[HttpException, JsValue]] = registerPsaResponse
  }

  val registerPsaResponseJson: JsValue =
    Json.obj(
      "processingDate" -> LocalDate.now,
      "formBundle" -> "1121313",
      "psaId" -> "A21999999"
    )
  val fakeSchemeService = new FakeSchemeService
  val controller = new SchemeController(fakeSchemeService)
}