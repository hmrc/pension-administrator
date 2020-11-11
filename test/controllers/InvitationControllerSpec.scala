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

package controllers

import base.JsonFileReader
import models.{IndividualDetails, MinimalDetails}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncFlatSpec, MustMatchers}
import play.api.libs.json._
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import service.InvitationService
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

class InvitationControllerSpec extends AsyncFlatSpec with MustMatchers  {

  import InvitationControllerSpec._

  "invite" should "return Created when service returns successfully" in {

      val result = controller.invite()(fakeRequest.withJsonBody(invitation))

      status(result) mustBe CREATED
    }

    it should "return BAD_REQUEST when service returns BAD_REQUEST" in {

      fakeInvitationService.setInvitePsaResponse(
        Future.successful(Left(new BadRequestException("bad request")))
      )

      val result = controller.invite()(fakeRequest.withJsonBody(invitation))

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "bad request"
    }

    it should "return NOT_FOUND when service returns NOT_FOUND" in {

      fakeInvitationService.setInvitePsaResponse(
        Future.successful(Left(new NotFoundException("not found")))
      )

      val result = controller.invite()(fakeRequest.withJsonBody(invitation))

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "not found"
    }

    it should "throw BadRequestException when no data received in the request" in {

      recoverToSucceededIf[BadRequestException] {
        controller.invite()(fakeRequest)
      }
    }

    it should "throw UpstreamErrorResponse when service throws UpstreamErrorResponse" in {

      fakeInvitationService.setInvitePsaResponse(Future.failed(UpstreamErrorResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))

      recoverToSucceededIf[UpstreamErrorResponse] {
        controller.invite()(fakeRequest.withJsonBody(invitation))
      }
    }

    it should "throw Exception when service throws any unknown Exception" in {

      fakeInvitationService.setInvitePsaResponse(Future.failed(new Exception("Unknown Exception")))

      recoverToSucceededIf[Exception] {
        controller.invite()(fakeRequest.withJsonBody(invitation))
      }
    }
}

object InvitationControllerSpec extends JsonFileReader with MockitoSugar{

  private val invitation = readJsonFromFile("/data/validInvitation.json")

  val response = MinimalDetails("aaa@email.com",true,None,Some(IndividualDetails("John",Some("Doe"),"Doe")),
    rlsFlag = true,
    deceasedFlag = true)

  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  class FakeInvitationService extends InvitationService {

    private var invitePsaResponse: Future[Either[HttpException, Unit]] = Future.successful(Right(()))

    def setInvitePsaResponse(response: Future[Either[HttpException, Unit]]): Unit = this.invitePsaResponse = response

    def invitePSA(jsValue: JsValue)
                 (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, Unit]] =
    invitePsaResponse
  }

  val fakeInvitationService = new FakeInvitationService
  val controller = new InvitationController(fakeInvitationService, stubControllerComponents())

}
