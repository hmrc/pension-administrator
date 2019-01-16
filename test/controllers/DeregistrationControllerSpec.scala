/*
 * Copyright 2019 HM Revenue & Customs
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
import connectors.SchemeConnector
import controllers.DeregistrationControllerSpec.psaId
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import play.api.libs.json._
import play.api.test.Helpers._
import uk.gov.hmrc.http.{BadRequestException, NotFoundException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeregistrationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with GeneratorDrivenPropertyChecks {

  import DeregistrationControllerSpec._

  private val mockSchemeConnector = mock[SchemeConnector]

  implicit val mat: Materializer = app.materializer

  private def deregistrationController: DeregistrationController =
    new DeregistrationController(mockSchemeConnector)

  before(reset(mockSchemeConnector))

  "Controller" must {
    "return OK and false when canDeregister called with psa ID having some schemes" in {
      when(mockSchemeConnector.listOfSchemes(Matchers.eq(psaId))(any(), any(), any()))
        .thenReturn(Future.successful(Right(validListSchemesResponse)))
      val result = deregistrationController.canDeregister(psaId = psaId)(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustEqual JsBoolean(false)
    }

    "return OK and true when canDeregister called with psa ID having no schemes" in {
      when(mockSchemeConnector.listOfSchemes(Matchers.eq(psaId))(any(), any(), any()))
        .thenReturn(Future.successful(Right(listSchemesResponseEmpty)))
      val result = deregistrationController.canDeregister(psaId = psaId)(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustEqual JsBoolean(true)
    }

    "return OK and true when canDeregister called with psa ID having only wound-up schemes" in {
      when(mockSchemeConnector.listOfSchemes(Matchers.eq(psaId))(any(), any(), any()))
        .thenReturn(Future.successful(Right(validListSchemesWoundUpOnlyResponse)))
      val result = deregistrationController.canDeregister(psaId = psaId)(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustEqual JsBoolean(true)
    }

    "return OK and false when canDeregister called with psa ID having both wound-up schemes and non-wound-up schemes" in {
      when(mockSchemeConnector.listOfSchemes(Matchers.eq(psaId))(any(), any(), any()))
        .thenReturn(Future.successful(Right(validListSchemesIncWoundUpResponse)))
      val result = deregistrationController.canDeregister(psaId = psaId)(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustEqual JsBoolean(false)
    }

    "return http exception when non OK httpresponse returned" in {
      when(mockSchemeConnector.listOfSchemes(Matchers.eq(psaId))(any(), any(), any()))
        .thenReturn(Future.successful(Left(new BadRequestException("bad request"))))
      val result = deregistrationController.canDeregister(psaId = psaId)(fakeRequest)
      status(result) mustBe BAD_REQUEST
    }
  }
}

object DeregistrationControllerSpec extends JsonFileReader {
  private val validListSchemesWoundUpOnlyResponse = readJsonFromFile("/data/validListOfSchemesWoundUpOnlyResponse.json")
  private val validListSchemesIncWoundUpResponse = readJsonFromFile("/data/validListOfSchemesIncWoundUpResponse.json")
  private val validListSchemesResponse = readJsonFromFile("/data/validListOfSchemesResponse.json")
  private val listSchemesResponseEmpty = Json.parse( """{
                                           |  "processingDate": "2001-12-17T09:30:47Z",
                                           |  "totalSchemesRegistered": "0",
                                           |  "schemeDetail": []
                                           |}""".stripMargin )
  private val psaId = "A123456"

}


