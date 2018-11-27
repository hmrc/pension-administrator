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
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncFlatSpec, MustMatchers}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http._
import utils.AuthRetrievals

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AssociationControllerSpec extends AsyncFlatSpec with JsonFileReader with MustMatchers {

  import AssociationControllerSpec._

  "getMinimalDetails" should "return OK when service returns successfully" in {

    val result = controller().getMinimalDetails(fakeRequest.withHeaders(("psaId", "A2123456")))

    status(result) mustBe OK
    contentAsJson(result) mustBe Json.toJson(psaMinimalDetailsIndividualUser)
  }

  it should "return bad request when connector returns BAD_REQUEST" in {

    fakeAssociationConnector.setPsaMinimalDetailsResponse(
      Future.successful(Left(new BadRequestException("bad request")))
    )

    val result = controller().getMinimalDetails(fakeRequest.withHeaders(("psaId", "A2123456")))

    status(result) mustBe BAD_REQUEST
    contentAsString(result) mustBe "bad request"
  }

  it should "return not found when connector returns NOT_FOUND" in {

    fakeAssociationConnector.setPsaMinimalDetailsResponse(
      Future.successful(Left(new NotFoundException("not found")))
    )

    val result = controller().getMinimalDetails(fakeRequest.withHeaders(("psaId", "A2123456")))

    status(result) mustBe NOT_FOUND
    contentAsString(result) mustBe "not found"
  }

  "acceptInvitation" should "return Created when the data is posted successfully" in {

    val result = controller().acceptInvitation(fakeRequest.withJsonBody(acceptedInvitationRequest))

    status(result) mustBe CREATED
  }

  it should "throw BadRequestException when no data received in the request" in {

    recoverToExceptionIf[BadRequestException](controller().acceptInvitation(fakeRequest)) map {
      ex =>
        ex.responseCode mustBe BAD_REQUEST
        ex.message mustBe "No Request Body received for accept invitation"
    }
  }

  it should "throw BadRequestException when invalid data received in the request" in {

    recoverToExceptionIf[BadRequestException](controller().acceptInvitation(fakeRequest.withJsonBody(Json.obj("invalid" -> "data")))) map {
      ex =>
        ex.responseCode mustBe BAD_REQUEST
        ex.message mustBe "Bad request received from frontend for accept invitation"
    }
  }

  it should "return CONFLICT when connector returns ConflictException" in {

    fakeAssociationConnector.setAcceptInvitationResponse(
      Future.successful(Left(new ConflictException("Conflict")))
    )
    val result = controller().acceptInvitation(fakeRequest.withJsonBody(acceptedInvitationRequest))

    status(result) mustBe CONFLICT
    contentAsString(result) mustBe "Conflict"
  }

  it should "return BAD_REQUEST when connector return BadRequestException" in {

    fakeAssociationConnector.setAcceptInvitationResponse(
      Future.successful(Left(new BadRequestException("Bad Request")))
    )
    val result = controller().acceptInvitation(fakeRequest.withJsonBody(acceptedInvitationRequest))

    status(result) mustBe BAD_REQUEST
    contentAsString(result) mustBe "Bad Request"
  }

  it should "return NOT_FOUND when connector return NotFoundException" in {

    fakeAssociationConnector.setAcceptInvitationResponse(
      Future.successful(Left(new NotFoundException("Not Found")))
    )
    val result = controller().acceptInvitation(fakeRequest.withJsonBody(acceptedInvitationRequest))

    status(result) mustBe NOT_FOUND
    contentAsString(result) mustBe "Not Found"
  }

  it should "throw Upstream5xxResponse when connector throws Upstream5xxResponse" in {

    fakeAssociationConnector.setAcceptInvitationResponse(Future.failed(Upstream5xxResponse("Failed with 5XX", SERVICE_UNAVAILABLE, BAD_GATEWAY)))

    recoverToExceptionIf[Upstream5xxResponse](controller().acceptInvitation(fakeRequest.withJsonBody(acceptedInvitationRequest))) map {
      ex =>
        ex.upstreamResponseCode mustBe SERVICE_UNAVAILABLE
        ex.message mustBe "Failed with 5XX"
    }
  }

  "getEmail" should "return email associated with PSAID for authorised user" in {

    fakeAssociationConnector.setPsaMinimalDetailsResponse(Future.successful(Right(
      psaMinimalDetailsIndividualUser
    )))

    val result = controller().getEmail(fakeRequest)

    status(result) mustBe OK
    contentAsString(result) mustBe "test@email.com"
  }

  it should "return UnauthorizedException with message for psaId not found in enrolments" in {

    recoverToExceptionIf[UnauthorizedException](controller(psaId = None).getEmail(fakeRequest)) map { ex =>
      ex.message mustBe "Cannot retrieve enrolment PSAID"
    }

  }

  it should "relay response from connector if not OK" in {

    fakeAssociationConnector.setPsaMinimalDetailsResponse(
      Future.successful(Left(new BadRequestException("bad request")))
    )

    val result = controller().getEmail(fakeRequest)

    status(result) mustBe BAD_REQUEST
    contentAsString(result) mustBe "bad request"
  }

  "getName" should "return name associated with PSAID for authorised Individual" in {

    fakeAssociationConnector.setPsaMinimalDetailsResponse(Future.successful(Right(
      psaMinimalDetailsIndividualUser
    )))

    val result = controller().getName(fakeRequest)

    status(result) mustBe OK
    contentAsString(result) mustBe individual.fullName
  }

  it should "return name associated with PSAID for authorised Organisation" in {

    fakeAssociationConnector.setPsaMinimalDetailsResponse(Future.successful(Right(
      psaMinimalDetailsOrganisationUser
    )))

    val result = controller(affinityGroup = Some(AffinityGroup.Organisation)).getName(fakeRequest)

    status(result) mustBe OK
    contentAsString(result) mustBe "PSA Ltd."
  }

  it should "return UnauthorizedException with message for psaId not found in enrolments" in {

    recoverToExceptionIf[UnauthorizedException](controller(psaId = None).getName(fakeRequest)) map { ex =>
      ex.message mustBe "Cannot retrieve enrolment PSAID"
    }

  }

  it should "relay response from connector if not OK" in {

    fakeAssociationConnector.setPsaMinimalDetailsResponse(
      Future.successful(Left(new BadRequestException("bad request")))
    )

    val result = controller().getName(fakeRequest)

    status(result) mustBe BAD_REQUEST
    contentAsString(result) mustBe "bad request"
  }

}

object AssociationControllerSpec extends MockitoSugar {

  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  val individual = IndividualDetails("testFirst", Some("testMiddle"), "testLast")

  val psaMinimalDetailsIndividualUser = PSAMinimalDetails(
    "test@email.com",
    isPsaSuspended = true,
    None,
    Some(individual)
  )

  val psaMinimalDetailsOrganisationUser = PSAMinimalDetails(
    "test@email.com",
    isPsaSuspended = true,
    Some("PSA Ltd."),
    None
  )

  class FakeAssociationConnector extends AssociationConnector {

    private var minimalPsaDetailsResponse: Future[Either[HttpException, PSAMinimalDetails]] = Future.successful(Right(psaMinimalDetailsIndividualUser))

    private var acceptInvitationResponse: Future[Either[HttpException, Unit]] = Future.successful(Right(()))

    def setPsaMinimalDetailsResponse(response: Future[Either[HttpException, PSAMinimalDetails]]): Unit = this.minimalPsaDetailsResponse = response

    def setAcceptInvitationResponse(response: Future[Either[HttpException, Unit]]): Unit = this.acceptInvitationResponse = response

    def getPSAMinimalDetails(psaId: PsaId)(implicit
                                            headerCarrier: HeaderCarrier,
                                            ec: ExecutionContext): Future[Either[HttpException, PSAMinimalDetails]] = minimalPsaDetailsResponse

    override def acceptInvitation(invitation: AcceptedInvitation)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader):
    Future[Either[HttpException, Unit]] = acceptInvitationResponse
  }

  val fakeAssociationConnector = new FakeAssociationConnector

  lazy val mockAuthRetrievals: AuthRetrievals = mock[AuthRetrievals]

  def controller(psaId: Option[PsaId] = Some(PsaId("A2123456")),
                 affinityGroup: Option[AffinityGroup] = Some(AffinityGroup.Individual)): AssociationController = {

    when(mockAuthRetrievals.getPsaId(any(), any()))
      .thenReturn(Future.successful(psaId))

    new AssociationController(fakeAssociationConnector, mockAuthRetrievals, stubControllerComponents())

  }

  val acceptedInvitationRequest: JsValue = Json.parse(
    """
      |{"pstr":"test-pstr","inviteePsaId":"A7654321","inviterPsaId":"A1234567","declaration":true,"declarationDuties":true}
    """.stripMargin
  )
}
