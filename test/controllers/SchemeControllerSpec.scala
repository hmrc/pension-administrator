/*
 * Copyright 2021 HM Revenue & Customs
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

import akka.stream.Materializer
import base.{JsonFileReader, SpecBase}
import models.PsaToBeRemovedFromScheme
import org.joda.time.LocalDate
import org.scalatest.{AsyncFlatSpec, MustMatchers}
import play.api.http.Status.BAD_GATEWAY
import play.api.libs.json.JodaWrites._
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import service.SchemeService
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.{BadRequestException, _}
import utils.FakeDesConnector
import utils.testhelpers.PsaSubscriptionBuilder._

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

  it should "throw UpstreamErrorResponse when service throws UpstreamErrorResponse" in {

    fakeSchemeService.setRegisterPsaResponse(Future.failed(UpstreamErrorResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))

    recoverToSucceededIf[UpstreamErrorResponse] {
      controller.registerPSA(fakeRequest.withJsonBody(validRequestData))
    }
  }

  it should "throw Exception when service throws any unknown Exception" in {

    fakeSchemeService.setRegisterPsaResponse(Future.failed(new Exception("Unknown Exception")))

    recoverToSucceededIf[Exception] {
      controller.registerPSA(fakeRequest.withJsonBody(validRequestData))
    }
  }

  "getPsaDetails" should "return OK when service returns successfully" in {

    val result = controller.getPsaDetails(fakeRequest.withHeaders(("psaId", "A2123456")))

    status(result) mustBe OK
    contentAsJson(result) mustBe Json.toJson(psaSubscription)
  }

  it should "return bad request when connector returns BAD_REQUEST" in {

    fakeDesConnector.setPsaDetailsResponse(
      Future.successful(Left(new BadRequestException("bad request")))
    )

    val result = controller.getPsaDetails(fakeRequest.withHeaders(("psaId", "A2123456")))

    status(result) mustBe BAD_REQUEST
    contentAsString(result) mustBe "bad request"
  }

  it should "return not found when connector returns NOT_FOUND" in {

    fakeDesConnector.setPsaDetailsResponse(
      Future.successful(Left(new NotFoundException("not found")))
    )

    val result = controller.getPsaDetails(fakeRequest.withHeaders(("psaId", "A2123456")))

    status(result) mustBe NOT_FOUND
    contentAsString(result) mustBe "not found"
  }

  "removePSA" should "return NO_CONTENT when service returns successfully" in {

    val result = call(controller.removePsa, removePsaFakeRequest(removePsaJson))
    status(result) mustBe NO_CONTENT

  }

  it should "return BAD_REQUEST when service returns BAD_REQUEST" in {

    fakeDesConnector.setRemovePsaResponse(
      Future.successful(Left(new BadRequestException("bad request")))
    )

    val result = call(controller.removePsa, removePsaFakeRequest(removePsaJson))

    status(result) mustBe BAD_REQUEST
    contentAsString(result) mustBe "bad request"
  }

  it should "return CONFLICT when service returns CONFLICT" in {

    fakeDesConnector.setRemovePsaResponse(
      Future.successful(Left(new ConflictException("conflict")))
    )

    val result = call(controller.removePsa, removePsaFakeRequest(removePsaJson))

    status(result) mustBe CONFLICT
    contentAsString(result) mustBe "conflict"
  }

  it should "return NOT_FOUND when service returns NOT_FOUND" in {

    fakeDesConnector.setRemovePsaResponse(
      Future.successful(Left(new NotFoundException("not found")))
    )

    val result = call(controller.removePsa, removePsaFakeRequest(removePsaJson))

    status(result) mustBe NOT_FOUND
    contentAsString(result) mustBe "not found"
  }

  it should "return Forbidden when service return Forbidden" in {

    fakeDesConnector.setRemovePsaResponse(
      Future.successful(Left(new ForbiddenException("forbidden")))
    )

    val result = call(controller.removePsa, removePsaFakeRequest(removePsaJson))

    status(result) mustBe FORBIDDEN
    contentAsString(result) mustBe "forbidden"
  }


  it should "throw UpstreamErrorResponse when service throws UpstreamErrorResponse" in {

    fakeDesConnector.setRemovePsaResponse(Future.failed(UpstreamErrorResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))

    recoverToSucceededIf[UpstreamErrorResponse] {
      call(controller.removePsa, removePsaFakeRequest(removePsaJson))
    }
  }

  it should "throw UpStream4xxResponse when service throws UpStream4xxResponse" in {

    fakeDesConnector.setRemovePsaResponse(Future.failed(UpstreamErrorResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))

    recoverToSucceededIf[UpstreamErrorResponse] {
      call(controller.removePsa, removePsaFakeRequest(removePsaJson))
    }
  }

  it should "throw Exception when service throws any unknown Exception" in {

    fakeDesConnector.setRemovePsaResponse(Future.failed(new Exception("Unknown Exception")))

    recoverToSucceededIf[Exception] {
      controller.registerPSA(fakeRequest.withJsonBody(validRequestData))
    }
  }

  "deregisterPSA" should "return OK when service returns successfully" in {

    val result = call(controller.deregisterPsa(psaId.id), deregisterPsaFakeRequest)
    status(result) mustBe NO_CONTENT

  }

  it should "return BAD_REQUEST when service returns BAD_REQUEST" in {

    fakeDesConnector.setDeregisterPsaResponse(
      Future.successful(Left(new BadRequestException("bad request")))
    )

    val result = call(controller.deregisterPsa(psaId.id), deregisterPsaFakeRequest)

    status(result) mustBe BAD_REQUEST
    contentAsString(result) mustBe "bad request"
  }

  it should "return CONFLICT when service returns CONFLICT" in {

    fakeDesConnector.setDeregisterPsaResponse(
      Future.successful(Left(new ConflictException("conflict")))
    )

    val result = call(controller.deregisterPsa(psaId.id), deregisterPsaFakeRequest)

    status(result) mustBe CONFLICT
    contentAsString(result) mustBe "conflict"
  }

  it should "return NOT_FOUND when service returns NOT_FOUND" in {

    fakeDesConnector.setDeregisterPsaResponse(
      Future.successful(Left(new NotFoundException("not found")))
    )

    val result = call(controller.deregisterPsa(psaId.id), deregisterPsaFakeRequest)

    status(result) mustBe NOT_FOUND
    contentAsString(result) mustBe "not found"
  }

  it should "return Forbidden when service return Forbidden" in {

    fakeDesConnector.setDeregisterPsaResponse(
      Future.successful(Left(new ForbiddenException("forbidden")))
    )

    val result = call(controller.deregisterPsa(psaId.id), deregisterPsaFakeRequest)

    status(result) mustBe FORBIDDEN
    contentAsString(result) mustBe "forbidden"
  }


  it should "throw UpstreamErrorResponse when service throws UpstreamErrorResponse" in {

    fakeDesConnector.setDeregisterPsaResponse(Future.failed(UpstreamErrorResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))

    recoverToSucceededIf[UpstreamErrorResponse] {
      call(controller.deregisterPsa(psaId.id), deregisterPsaFakeRequest)
    }
  }

  it should "throw UpStream4xxResponse when service throws UpStream4xxResponse" in {

    fakeDesConnector.setDeregisterPsaResponse(Future.failed(UpstreamErrorResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))

    recoverToSucceededIf[UpstreamErrorResponse] {
      call(controller.deregisterPsa(psaId.id), deregisterPsaFakeRequest)
    }
  }

  it should "throw Exception when service throws any unknown Exception" in {

    fakeDesConnector.setDeregisterPsaResponse(Future.failed(new Exception("Unknown Exception")))

    recoverToSucceededIf[Exception] {
      call(controller.deregisterPsa(psaId.id), deregisterPsaFakeRequest)
    }
  }

  "updatePSA" should "return Ok when successful" in {

    val result = controller.updatePSA(psaId.id)(fakeRequest.withJsonBody(psaVariationData))

    status(result) mustBe OK
  }

  it should "return INVALID_PSAID when service returns INVALID_PSAID" in {

    fakeSchemeService.setUpdatePsaResponse(
      Future.successful(Left(new BadRequestException("INVALID_PSAID")))
    )

    val result = controller.updatePSA(psaId.id)(fakeRequest.withJsonBody(psaVariationData))

    status(result) mustBe BAD_REQUEST
    contentAsString(result) mustBe "INVALID_PSAID"
  }

  it should "return NOT_FOUND when service returns NOT_FOUND" in {

    fakeSchemeService.setUpdatePsaResponse(
      Future.successful(Left(new NotFoundException("not found")))
    )

    val result = controller.updatePSA(psaId.id)(fakeRequest.withJsonBody(psaVariationData))

    status(result) mustBe NOT_FOUND
    contentAsString(result) mustBe "not found"
  }

  it should "throw BadRequestException when no data received in the request" in {

    recoverToSucceededIf[BadRequestException] {
      controller.updatePSA(psaId.id)(fakeRequest)
    }
  }

  it should "throw UpstreamErrorResponse when service throws UpstreamErrorResponse" in {

    fakeSchemeService.setUpdatePsaResponse(Future.failed(UpstreamErrorResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))

    recoverToSucceededIf[UpstreamErrorResponse] {
      controller.updatePSA(psaId.id)(fakeRequest.withJsonBody(psaVariationData))
    }
  }

  it should "throw UpStream4xxResponse when service throws UpStream4xxResponse" in {

    fakeSchemeService.setUpdatePsaResponse(Future.failed(UpstreamErrorResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))

    recoverToSucceededIf[UpstreamErrorResponse] {
      controller.updatePSA(psaId.id)(fakeRequest.withJsonBody(psaVariationData))
    }
  }

}

object SchemeControllerSpec extends SpecBase {

  implicit val mat: Materializer = app.materializer
  override def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  class FakeSchemeService extends SchemeService {

    private var registerPsaResponse: Future[Either[HttpException, JsValue]] = Future.successful(Right(registerPsaResponseJson))
    private var updatePsaResponse: Future[Either[HttpException, JsValue]] = Future.successful(Right(registerPsaResponseJson))

    def setRegisterPsaResponse(response: Future[Either[HttpException, JsValue]]): Unit = this.registerPsaResponse = response
    def setUpdatePsaResponse(response: Future[Either[HttpException, JsValue]]): Unit = this.updatePsaResponse = response

    override def registerPSA(json: JsValue)(implicit headerCarrier: HeaderCarrier,
                                            ec: ExecutionContext,
                                            request: RequestHeader): Future[Either[HttpException, JsValue]] = registerPsaResponse

    override def updatePSA(psaId: String, json: JsValue)(
      implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, JsValue]] = updatePsaResponse
  }

  private val registerPsaResponseJson: JsValue =
    Json.obj(
      "processingDate" -> LocalDate.now,
      "formBundle" -> "1121313",
      "psaId" -> "A21999999"
    )

  private val updatePsaResponseJson = Json.obj(
    "processingDate" -> "2001-12-17T09:30:47Z",
    "formBundleNumber" -> "12345678912"
  )

  private val psaVariationData : JsValue = readJsonFromFile("/data/validPsaVariationRequest.json")

  private val fakeSchemeService = new FakeSchemeService
  private val fakeDesConnector: FakeDesConnector = new FakeDesConnector()
  private val controller = new SchemeController(fakeSchemeService, fakeDesConnector, controllerComponents)

  private val psaId = PsaId("A7654321")
  private val pstr: String = "123456789AB"
  private val removalDate = new LocalDate(2018,2,1)
  private val removeDate: LocalDate = LocalDate.parse("2018-02-01")
  private val removePsaDataModel: PsaToBeRemovedFromScheme = PsaToBeRemovedFromScheme(psaId.id, pstr, removeDate)
  private val removePsaJson: JsValue = Json.toJson(removePsaDataModel)

  private def removePsaFakeRequest(data: JsValue): FakeRequest[JsValue] = FakeRequest("DELETE", "/").withBody(data)

  private def deregisterPsaFakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("DELETE", "/")
}
