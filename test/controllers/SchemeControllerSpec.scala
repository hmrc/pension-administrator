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

package controllers

import base.{JsonFileReader, SpecBase}
import connectors.HipConnector
import connectors.RegistrationConnectorSpec.request
import models.PsaToBeRemovedFromScheme
import models.admin.PsaRegHipMigrationToggle
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.BAD_GATEWAY
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, BodyParsers}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.*
import service.SchemeService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.{BadRequestException, *}
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import utils.FakeDesConnector.{deregisterPsaResponseJson, removePsaResponseJson}
import utils.testhelpers.PsaSubscriptionBuilder.*
import utils.{AuthUtils, FakeDesConnector, FakePsaSchemeAuthAction}

import java.time.{LocalDate, ZoneId}
import scala.concurrent.Future

class SchemeControllerSpec extends AsyncFlatSpec with JsonFileReader with Matchers with BeforeAndAfterEach {

  import SchemeControllerSpec.*

  private val validRequestData = readJsonFromFile("/data/validPsaRequest.json")

  private val srn = AuthUtils.srn

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockAuthConnector, mockFeatureFlagService, mockHipConnector)
    AuthUtils.authStub(mockAuthConnector)
    when(mockSchemeService.registerPSA(any())(using any(), any(), any()))
      .thenReturn(Future.successful(Right(registerPsaResponseJson)))
    when(mockSchemeService.updatePSA(any(), any())(using any(), any(), any()))
      .thenReturn(Future.successful(Right(registerPsaResponseJson)))
    when(mockFeatureFlagService.get(PsaRegHipMigrationToggle))
      .thenReturn(Future.successful(FeatureFlag(PsaRegHipMigrationToggle, isEnabled = false)))
    fakeDesConnector.setDeregisterPsaResponse(Future.successful(Right(deregisterPsaResponseJson)))
    fakeDesConnector.setPsaDetailsResponse(Future.successful(Right(Json.toJson(psaSubscription))))
    fakeDesConnector.setRemovePsaResponse(Future.successful(Right(removePsaResponseJson)))

  }

  "registerPSA" should "return OK when service returns successfully" in {
    Mockito.reset(mockAuthConnector)
    AuthUtils.noEnrolmentAuthStub(mockAuthConnector)

    val result = controller.registerPSA(fakeRequest.withJsonBody(validRequestData))

    status(result).mustBe(OK)
    contentAsJson(result).mustBe(registerPsaResponseJson)
  }

  it should "return BAD_REQUEST when service returns BAD_REQUEST" in {

    when(mockSchemeService.registerPSA(any())(using any(), any(), any()))
      .thenReturn(Future.successful(Left(new BadRequestException("bad request"))))
    Mockito.reset(mockAuthConnector)
    AuthUtils.noEnrolmentAuthStub(mockAuthConnector)

    val result = controller.registerPSA(fakeRequest.withJsonBody(validRequestData))

    status(result).mustBe(BAD_REQUEST)
    contentAsString(result).mustBe("bad request")
  }

  it should "return CONFLICT when service returns CONFLICT" in {
    when(mockSchemeService.registerPSA(any())(using any(), any(), any()))
      .thenReturn(Future.successful(Left(new ConflictException("conflict"))))
    AuthUtils.noEnrolmentAuthStub(mockAuthConnector)

    val result = controller.registerPSA(fakeRequest.withJsonBody(validRequestData))

    status(result).mustBe(CONFLICT)
    contentAsString(result).mustBe("conflict")
  }

  it should "return NOT_FOUND when service returns NOT_FOUND" in {
    when(mockSchemeService.registerPSA(any())(using any(), any(), any()))
      .thenReturn(Future.successful(Left(new NotFoundException("not found"))))
    AuthUtils.noEnrolmentAuthStub(mockAuthConnector)

    val result = controller.registerPSA(fakeRequest.withJsonBody(validRequestData))

    status(result).mustBe(NOT_FOUND)
    contentAsString(result).mustBe("not found")
  }

  it should "return Forbidden when service return Forbidden" in {

    when(mockSchemeService.registerPSA(any())(using any(), any(), any()))
      .thenReturn(Future.successful(Left(new ForbiddenException("forbidden"))))
    AuthUtils.noEnrolmentAuthStub(mockAuthConnector)

    val result = controller.registerPSA(fakeRequest.withJsonBody(validRequestData))

    status(result).mustBe(FORBIDDEN)
    contentAsString(result).mustBe("forbidden")
  }

  it should "return Forbidden when service return invalid PsaId" in {

    when(mockSchemeService.registerPSA(any())(using any(), any(), any()))
      .thenReturn(Future.successful(Left(new ForbiddenException(
        "INVALID_PSAID : The back end has indicated that PSAID is already de-limited and hence not valid."))
      ))
    AuthUtils.noEnrolmentAuthStub(mockAuthConnector)

    val result = controller.registerPSA(fakeRequest.withJsonBody(validRequestData))

    status(result).mustBe(FORBIDDEN)
    contentAsString(result).mustBe("INVALID_PSAID")
  }

  it should "throw BadRequestException when no data recieved in the request" in {

    AuthUtils.noEnrolmentAuthStub(mockAuthConnector)
    recoverToSucceededIf[BadRequestException] {
      controller.registerPSA(fakeRequest)
    }
  }

  it should "throw BadRequestException when service throws JsResultException" in {

    when(mockSchemeService.registerPSA(any())(using any(), any(), any()))
      .thenReturn(Future.failed(JsResultException(Nil)))
    AuthUtils.noEnrolmentAuthStub(mockAuthConnector)
    recoverToSucceededIf[BadRequestException] {
      controller.registerPSA(fakeRequest.withJsonBody(validRequestData))
    }
  }

  it should "throw UpstreamErrorResponse when service throws UpstreamErrorResponse" in {

    when(mockSchemeService.registerPSA(any())(using any(), any(), any()))
      .thenReturn(Future.failed(UpstreamErrorResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))
    AuthUtils.noEnrolmentAuthStub(mockAuthConnector)
    recoverToSucceededIf[UpstreamErrorResponse] {
      controller.registerPSA(fakeRequest.withJsonBody(validRequestData))
    }
  }

  it should "throw Exception when service throws any unknown Exception" in {

    when(mockSchemeService.registerPSA(any())(using any(), any(), any()))
      .thenReturn(Future.failed(new Exception("Unknown Exception")))
    AuthUtils.noEnrolmentAuthStub(mockAuthConnector)
    recoverToSucceededIf[Exception] {
      controller.registerPSA(fakeRequest.withJsonBody(validRequestData))
    }
  }

  "getPsaDetailsSelf" should "return OK when service returns successfully" in {

    val result = controller.getPsaDetailsSelf(fakeRequest)

    status(result).mustBe(OK)
    contentAsJson(result).mustBe(Json.toJson(psaSubscription))
  }

  it should "return OK when service returns successfully when PsaRegHipMigrationToggle is enabled" in {
    when(mockFeatureFlagService.get(PsaRegHipMigrationToggle))
      .thenReturn(Future.successful(FeatureFlag(PsaRegHipMigrationToggle, isEnabled = true)))
    when(mockHipConnector.getPSASubscriptionDetails(any())(using any(), any()))
      .thenReturn(Future.successful(Right(Json.toJson(psaSubscription))))

    val result = controller.getPsaDetailsSelf(fakeRequest)

    status(result).mustBe(OK)
    contentAsJson(result).mustBe(Json.toJson(psaSubscription))
  }

  it should "return bad request when connector returns BAD_REQUEST" in {

    fakeDesConnector.setPsaDetailsResponse(
      Future.successful(Left(new BadRequestException("bad request")))
    )

    val result = controller.getPsaDetailsSelf(fakeRequest)

    status(result).mustBe(BAD_REQUEST)
    contentAsString(result).mustBe("bad request")
  }

  it should "return bad request when connector returns BAD_REQUEST when PsaRegHipMigrationToggle is enabled" in {
    when(mockFeatureFlagService.get(PsaRegHipMigrationToggle))
      .thenReturn(Future.successful(FeatureFlag(PsaRegHipMigrationToggle, isEnabled = true)))
    when(mockHipConnector.getPSASubscriptionDetails(any())(using any(), any()))
      .thenReturn(Future.successful(Left(new BadRequestException("bad request"))))

    val result = controller.getPsaDetailsSelf(fakeRequest)

    status(result).mustBe(BAD_REQUEST)
    contentAsString(result).mustBe("bad request")
  }

  it should "return not found when connector returns NOT_FOUND" in {

    fakeDesConnector.setPsaDetailsResponse(
      Future.successful(Left(new NotFoundException("not found")))
    )

    val result = controller.getPsaDetailsSelf(fakeRequest)

    status(result).mustBe(NOT_FOUND)
    contentAsString(result).mustBe("not found")
  }

  it should "return not found when connector returns NOT_FOUND when PsaRegHipMigrationToggle is enabled" in {
    when(mockFeatureFlagService.get(PsaRegHipMigrationToggle))
      .thenReturn(Future.successful(FeatureFlag(PsaRegHipMigrationToggle, isEnabled = true)))
    when(mockHipConnector.getPSASubscriptionDetails(any())(using any(), any()))
      .thenReturn(Future.successful(Left(new NotFoundException("not found"))))

    val result = controller.getPsaDetailsSelf(fakeRequest)

    status(result).mustBe(NOT_FOUND)
    contentAsString(result).mustBe("not found")
  }

  "removePSA" should "return NO_CONTENT when service returns successfully" in {

    val result = call(controller.removePsa(srn), removePsaFakeRequest(removePsaJson))
    status(result).mustBe(NO_CONTENT)

  }

  it should "return BAD_REQUEST when service returns BAD_REQUEST" in {

    fakeDesConnector.setRemovePsaResponse(
      Future.successful(Left(new BadRequestException("bad request")))
    )

    val result = call(controller.removePsa(srn), removePsaFakeRequest(removePsaJson))

    status(result).mustBe(BAD_REQUEST)
    contentAsString(result).mustBe("bad request")
  }

  it should "return CONFLICT when service returns CONFLICT" in {

    fakeDesConnector.setRemovePsaResponse(
      Future.successful(Left(new ConflictException("conflict")))
    )

    val result = call(controller.removePsa(srn), removePsaFakeRequest(removePsaJson))

    status(result).mustBe(CONFLICT)
    contentAsString(result).mustBe("conflict")
  }

  it should "return NOT_FOUND when service returns NOT_FOUND" in {

    fakeDesConnector.setRemovePsaResponse(
      Future.successful(Left(new NotFoundException("not found")))
    )

    val result = call(controller.removePsa(srn), removePsaFakeRequest(removePsaJson))

    status(result).mustBe(NOT_FOUND)
    contentAsString(result).mustBe("not found")
  }

  it should "return Forbidden when service return Forbidden" in {

    fakeDesConnector.setRemovePsaResponse(
      Future.successful(Left(new ForbiddenException("forbidden")))
    )

    val result = call(controller.removePsa(srn), removePsaFakeRequest(removePsaJson))

    status(result).mustBe(FORBIDDEN)
    contentAsString(result).mustBe("forbidden")
  }


  it should "throw UpstreamErrorResponse when service throws UpstreamErrorResponse" in {

    fakeDesConnector.setRemovePsaResponse(Future.failed(UpstreamErrorResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))

    recoverToSucceededIf[UpstreamErrorResponse] {
      call(controller.removePsa(srn), removePsaFakeRequest(removePsaJson))
    }
  }

  it should "throw UpStream4xxResponse when service throws UpStream4xxResponse" in {

    fakeDesConnector.setRemovePsaResponse(Future.failed(UpstreamErrorResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))

    recoverToSucceededIf[UpstreamErrorResponse] {
      call(controller.removePsa(srn), removePsaFakeRequest(removePsaJson))
    }
  }

  it should "throw Exception when service throws any unknown Exception" in {

    fakeDesConnector.setRemovePsaResponse(Future.failed(new Exception("Unknown Exception")))

    recoverToSucceededIf[Exception] {
      controller.registerPSA(fakeRequest.withJsonBody(validRequestData))
    }
  }

  "deregisterPSASelf" should "return OK when service returns successfully" in {

    val result = call(controller.deregisterPsaSelf, deregisterPsaFakeRequest)
    status(result).mustBe(NO_CONTENT)

  }

  it should "return BAD_REQUEST when service returns BAD_REQUEST" in {

    fakeDesConnector.setDeregisterPsaResponse(
      Future.successful(Left(new BadRequestException("bad request")))
    )

    val result = call(controller.deregisterPsaSelf, deregisterPsaFakeRequest)

    status(result).mustBe(BAD_REQUEST)
    contentAsString(result).mustBe("bad request")
  }

  it should "return CONFLICT when service returns CONFLICT" in {

    fakeDesConnector.setDeregisterPsaResponse(
      Future.successful(Left(new ConflictException("conflict")))
    )

    val result = call(controller.deregisterPsaSelf, deregisterPsaFakeRequest)

    status(result).mustBe(CONFLICT)
    contentAsString(result).mustBe("conflict")
  }

  it should "return NOT_FOUND when service returns NOT_FOUND" in {

    fakeDesConnector.setDeregisterPsaResponse(
      Future.successful(Left(new NotFoundException("not found")))
    )

    val result = call(controller.deregisterPsaSelf, deregisterPsaFakeRequest)

    status(result).mustBe(NOT_FOUND)
    contentAsString(result).mustBe("not found")
  }

  it should "return Forbidden when service return Forbidden" in {

    fakeDesConnector.setDeregisterPsaResponse(
      Future.successful(Left(new ForbiddenException("forbidden")))
    )

    val result = call(controller.deregisterPsaSelf, deregisterPsaFakeRequest)

    status(result).mustBe(FORBIDDEN)
    contentAsString(result).mustBe("forbidden")
  }
  
  
  it should "throw UpstreamErrorResponse when service throws UpstreamErrorResponse" in {

    fakeDesConnector.setDeregisterPsaResponse(Future.failed(UpstreamErrorResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))

    recoverToSucceededIf[UpstreamErrorResponse] {
      call(controller.deregisterPsaSelf, deregisterPsaFakeRequest)
    }
  }


  it should "throw Exception when service throws any unknown Exception" in {

    fakeDesConnector.setDeregisterPsaResponse(Future.failed(new Exception("Unknown Exception")))

    recoverToSucceededIf[Exception] {
      call(controller.deregisterPsaSelf, deregisterPsaFakeRequest)
    }
  }

  "updatePSASelf" should "return Ok when successful" in {

    val result = controller.updatePsaSelf(fakeRequest.withJsonBody(psaVariationData))

    status(result).mustBe(OK)
  }

  it should "return INVALID_PSAID when service returns INVALID_PSAID" in {

    when(mockSchemeService.updatePSA(any(), any())(using any(), any(), any()))
      .thenReturn(Future.successful(Left(new BadRequestException("INVALID_PSAID"))))

    val result = controller.updatePsaSelf(fakeRequest.withJsonBody(psaVariationData))

    status(result).mustBe(BAD_REQUEST)
    contentAsString(result).mustBe("INVALID_PSAID")
  }

  it should "throw BadRequestException when no data received in the request" in {
    AuthUtils.authStub(mockAuthConnector)

    recoverToSucceededIf[BadRequestException] {
      controller.updatePsaSelf(fakeRequest)
    }
  }

  it should "throw UpstreamErrorResponse when service throws UpstreamErrorResponse" in {

    when(mockSchemeService.updatePSA(any(), any())(using any(), any(), any()))
      .thenReturn(Future.failed(UpstreamErrorResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))
    
    AuthUtils.authStub(mockAuthConnector)

    recoverToSucceededIf[UpstreamErrorResponse] {
      controller.updatePsaSelf(fakeRequest.withJsonBody(psaVariationData))
    }
  }

}

object SchemeControllerSpec extends SpecBase with MockitoSugar {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val mockHipConnector: HipConnector = mock[HipConnector]
  private val mockSchemeService = mock[SchemeService]
  private val mockFeatureFlagService = mock[FeatureFlagService]

  implicit val mat: Materializer = app.materializer

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
      bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
      bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
      bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
      bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository]),
      bind[actions.PsaPspEnrolmentAuthAction].toInstance(mock[actions.PsaPspEnrolmentAuthAction]),
      bind[SchemeService].toInstance(mockSchemeService),
      bind[HipConnector].toInstance(mockHipConnector),
      bind[FeatureFlagService].toInstance(mockFeatureFlagService)
  )

  override def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  private val registerPsaResponseJson: JsValue =
    Json.obj(
      "processingDate" -> LocalDate.now,
      "formBundle" -> "1121313",
      "psaId" -> "A21999999"
    )

  private val psaVariationData: JsValue = readJsonFromFile("/data/validPsaVariationRequest.json")

  private val fakeDesConnector: FakeDesConnector = new FakeDesConnector()
  val bodyParser: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]
  private val controller = new SchemeController(mockSchemeService,
                                                fakeDesConnector,
                                                mockHipConnector,
                                                controllerComponents,
    new actions.PsaPspEnrolmentAuthAction(mockAuthConnector, bodyParser),
    new actions.NoEnrolmentAuthAction(mockAuthConnector, bodyParser),
    new actions.PsaEnrolmentAuthAction(mockAuthConnector, bodyParser),
    new FakePsaSchemeAuthAction(),
    mockFeatureFlagService
  )
  private val psaId = PsaId("A7654321")
  private val pstr: String = "123456789AB"
  private val removeDate: LocalDate = LocalDate.parse("2018-02-01").atStartOfDay(ZoneId.of("UTC")).toLocalDate
  private val removePsaDataModel: PsaToBeRemovedFromScheme = PsaToBeRemovedFromScheme(psaId.id, pstr, removeDate)
  private val removePsaJson: JsValue = Json.toJson(removePsaDataModel)

  private def removePsaFakeRequest(data: JsValue): FakeRequest[JsValue] = FakeRequest("DELETE", "/").withBody(data)

  private def deregisterPsaFakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("DELETE", "/")
}
