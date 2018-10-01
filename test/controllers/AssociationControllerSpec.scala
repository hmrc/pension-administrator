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
import connectors.AssociationConnector
import models._
import org.scalatest.{AsyncFlatSpec, MustMatchers}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

class AssociationControllerSpec extends AsyncFlatSpec with JsonFileReader with MustMatchers {

  import AssociationControllerSpec._

  "getMinimalDetails" should "return OK when service returns successfully" in {

    val result = controller.getMinimalDetails(fakeRequest.withHeaders(("psaId", "A2123456")))

    status(result) mustBe OK
    contentAsJson(result) mustBe Json.toJson(psaMinimalDetailsIndividualUser)
  }

  it should "return bad request when connector returns BAD_REQUEST" in {

    fakeAssociationConnector.setPsaMinimalDetailsResponse(
      Future.successful(Left(new BadRequestException("bad request")))
    )

    val result = controller.getMinimalDetails(fakeRequest.withHeaders(("psaId", "A2123456")))

    status(result) mustBe BAD_REQUEST
    contentAsString(result) mustBe "bad request"
  }

  it should "return not found when connector returns NOT_FOUND" in {

    fakeAssociationConnector.setPsaMinimalDetailsResponse(
      Future.successful(Left(new NotFoundException("not found")))
    )

    val result = controller.getMinimalDetails(fakeRequest.withHeaders(("psaId", "A2123456")))

    status(result) mustBe NOT_FOUND
    contentAsString(result) mustBe "not found"
  }


  "acceptInvitation" should "return Created when the data is posted successfully" in {

    val result = controller.acceptInvitation(fakeRequest.withJsonBody(acceptedInvitationRequest))

    status(result) mustBe CREATED
  }

  it should "throw BadRequestException when no data received in the request" in {

    recoverToExceptionIf[BadRequestException](controller.acceptInvitation(fakeRequest)) map {
      ex =>
        ex.responseCode mustBe BAD_REQUEST
        ex.message mustBe "No Request Body received for accept invitation"
    }
  }

  it should "throw BadRequestException when invalid data received in the request" in {

    recoverToExceptionIf[BadRequestException](controller.acceptInvitation(fakeRequest.withJsonBody(Json.obj("invalid" -> "data")))) map {
      ex =>
        ex.responseCode mustBe BAD_REQUEST
        ex.message mustBe "Bad request received from frontend for accept invitation"
    }
  }

  it should "return CONFLICT when connector returns ConflictException" in {

    fakeAssociationConnector.setAcceptInvitationResponse(
      Future.successful(Left(new ConflictException("Conflict")))
    )
    val result = controller.acceptInvitation(fakeRequest.withJsonBody(acceptedInvitationRequest))

    status(result) mustBe CONFLICT
    contentAsString(result) mustBe "Conflict"
  }

  it should "return BAD_REQUEST when connector return BadRequestException" in {

    fakeAssociationConnector.setAcceptInvitationResponse(
      Future.successful(Left(new BadRequestException("Bad Request")))
    )
    val result = controller.acceptInvitation(fakeRequest.withJsonBody(acceptedInvitationRequest))

    status(result) mustBe BAD_REQUEST
    contentAsString(result) mustBe "Bad Request"
  }

  it should "return NOT_FOUND when connector return NotFoundException" in {

    fakeAssociationConnector.setAcceptInvitationResponse(
      Future.successful(Left(new NotFoundException("Not Found")))
    )
    val result = controller.acceptInvitation(fakeRequest.withJsonBody(acceptedInvitationRequest))

    status(result) mustBe NOT_FOUND
    contentAsString(result) mustBe "Not Found"
  }

  it should "throw Upstream5xxResponse when connector throws Upstream5xxResponse" in {

    fakeAssociationConnector.setAcceptInvitationResponse(Future.failed(Upstream5xxResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))

    recoverToExceptionIf[Upstream5xxResponse](controller.acceptInvitation(fakeRequest.withJsonBody(acceptedInvitationRequest))) map {
      ex =>
        ex.upstreamResponseCode mustBe SERVICE_UNAVAILABLE
        ex.message mustBe "Failed with 5XX"
    }
  }
}

object AssociationControllerSpec {
  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  val psaMinimalDetailsIndividualUser = PSAMinimalDetails(
    "test@email.com",
    isPsaSuspended = true,
    None,
    Some(IndividualDetails(
      "testFirst",
      Some("testMiddle"),
      "testLast"
    ))
  )

  class FakeAssociationConnector extends AssociationConnector {

    private var minimalPsaDetailsResponse: Future[Either[HttpException, PSAMinimalDetails]] = Future.successful(Right(psaMinimalDetailsIndividualUser))

    private var acceptInvitationResponse: Future[Either[HttpException, Unit]] = Future.successful(Right(()))

    def setPsaMinimalDetailsResponse(response: Future[Either[HttpException, PSAMinimalDetails]]): Unit = this.minimalPsaDetailsResponse = response

    def setAcceptInvitationResponse(response: Future[Either[HttpException, Unit]]): Unit = this.acceptInvitationResponse = response

    def getPSAMinimalDetails(psaId: String)(implicit
                                            headerCarrier: HeaderCarrier,
                                            ec: ExecutionContext): Future[Either[HttpException, PSAMinimalDetails]] = minimalPsaDetailsResponse

    override def acceptInvitation(invitation: AcceptedInvitation)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader):
    Future[Either[HttpException, Unit]] = acceptInvitationResponse
  }

  val fakeAssociationConnector = new FakeAssociationConnector

  val controller = new AssociationController(fakeAssociationConnector)

  val acceptedInvitationRequest = Json.parse(
    """
      |{"pstr":"test-pstr","inviteePsaId":"test-invitee-psa-id","inviterPsaId":"test-inviter-psa-id","declaration":true,"declarationDuties":true}
    """.stripMargin
  )
}


