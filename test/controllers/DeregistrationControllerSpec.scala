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
import connectors.SchemeConnector
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.*
import play.api.test.Helpers.*
import repositories.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.BadRequestException
import utils.AuthUtils

import scala.concurrent.Future

class DeregistrationControllerSpec
  extends SpecBase
    with MockitoSugar
    with BeforeAndAfter
    with ScalaCheckDrivenPropertyChecks {

  import DeregistrationControllerSpec.*

  private val authConnector: AuthConnector = mock[AuthConnector]

  private val mockSchemeConnector = mock[SchemeConnector]

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
      bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
      bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
      bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
      bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository]),
      bind[SchemeConnector].toInstance(mockSchemeConnector),
      bind[AuthConnector].toInstance(authConnector)
    )

  implicit val mat: Materializer = app.materializer

  def deregistrationController: DeregistrationController = app.injector.instanceOf[DeregistrationController]

  before {
    reset(mockSchemeConnector)
    reset(authConnector)
    AuthUtils.authStub(authConnector)

  }

  "canDeregisterSelf" must {
    "return OK and false when canDeregister called with psa ID having some schemes" in {
      when(mockSchemeConnector.listOfSchemes(ArgumentMatchers.eq(psaId))(using any(), any(), any()))
        .thenReturn(Future.successful(Right(validListSchemesResponse)))

      val result = deregistrationController.canDeregisterSelf(fakeRequest)

      status(result).mustBe(OK)
      contentAsJson(result).mustEqual(Json.obj("canDeregister" -> JsBoolean(false), "isOtherPsaAttached" -> JsBoolean(false)))
    }

    "return OK and true when canDeregister called with psa ID having no scheme detail item at all" in {
      when(mockSchemeConnector.listOfSchemes(ArgumentMatchers.eq(psaId))(using any(), any(), any()))
        .thenReturn(Future.successful(Right(listSchemesResponseNoSchemeDetail)))

      val result = deregistrationController.canDeregisterSelf(fakeRequest)

      status(result).mustBe(OK)
      contentAsJson(result).mustEqual(Json.obj("canDeregister" -> JsBoolean(true), "isOtherPsaAttached" -> JsBoolean(false)))
    }

    "return OK and true when canDeregister called with psa ID having only wound-up or rejected schemes" in {
      when(mockSchemeConnector.listOfSchemes(ArgumentMatchers.eq(psaId))(using any(), any(), any()))
        .thenReturn(Future.successful(Right(schemesWoundUpOrRejectedResponse)))

      val result = deregistrationController.canDeregisterSelf(fakeRequest)

      status(result).mustBe(OK)
      contentAsJson(result).mustEqual(Json.obj("canDeregister" -> JsBoolean(true), "isOtherPsaAttached" -> JsBoolean(false)))
    }

    "return OK and false when canDeregister called with psa ID having both wound-up schemes and non-wound-up schemes and they are the only psa associated" in {
      when(mockSchemeConnector.listOfSchemes(ArgumentMatchers.eq(psaId))(using any(), any(), any()))
        .thenReturn(Future.successful(Right(validListSchemesIncWoundUpResponse)))
      when(mockSchemeConnector.getSchemeDetails(ArgumentMatchers.eq(psaId), any(), any())(using any(), any()))
        .thenReturn(Future.successful(Right(getSchemeDetails(Json.arr(psaObject(psaId))))))

      val result = deregistrationController.canDeregisterSelf(fakeRequest)

      status(result).mustBe(OK)
      contentAsJson(result).mustEqual(Json.obj("canDeregister" -> JsBoolean(false), "isOtherPsaAttached" -> JsBoolean(false)))
    }

    "return OK and false when canDeregister called with psa ID having Open scheme and there are other PSAs associated" in {
      when(mockSchemeConnector.listOfSchemes(ArgumentMatchers.eq(psaId))(using any(), any(), any()))
        .thenReturn(Future.successful(Right(validListSchemesIncWoundUpResponse)))
      when(mockSchemeConnector.getSchemeDetails(ArgumentMatchers.eq(psaId), any(), any())(using any(), any()))
        .thenReturn(Future.successful(Right(getSchemeDetails(Json.arr(psaObject(psaId))))))

      val result = deregistrationController.canDeregisterSelf(fakeRequest)

      status(result).mustBe(OK)
      contentAsJson(result).mustEqual(Json.obj("canDeregister" -> JsBoolean(false), "isOtherPsaAttached" -> JsBoolean(false)))
    }

    "return http exception when non OK httpresponse returned" in {
      when(mockSchemeConnector.listOfSchemes(ArgumentMatchers.eq(psaId))(using any(), any(), any()))
        .thenReturn(Future.successful(Left(new BadRequestException("bad request"))))

      val result = deregistrationController.canDeregisterSelf(fakeRequest)
      status(result).mustBe(BAD_REQUEST)
    }
  }
}

object DeregistrationControllerSpec extends JsonFileReader {
  private val schemesWoundUpOrRejectedResponse = readJsonFromFile("/data/validSchemesWoundUpOrRejectedResponse.json")
  private val validListSchemesIncWoundUpResponse = readJsonFromFile("/data/validListOfSchemesIncWoundUpResponse.json")
  private val validListSchemesResponse = readJsonFromFile("/data/validListOfSchemesResponse.json")

  private val listSchemesResponseNoSchemeDetail = Json.parse(
    """{
      |  "processingDate": "2001-12-17T09:30:47Z",
      |  "totalSchemesRegistered": "0"
      |}""".stripMargin)
  private val psaId = AuthUtils.psaId

  private def getSchemeDetails(psaArray: JsArray): JsObject =
    Json.obj("psaDetails" -> psaArray)

  private def psaObject(psaId: String) = Json.obj(
    "id" -> psaId,
    "organisationOrPartnershipName" -> "partnership name"
  )
}
