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
import models.{AcceptedInvitation, IndividualDetails, PSAMinimalDetails}
import org.scalatest.{AsyncFlatSpec, MustMatchers}
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
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

    def setPsaMinimalDetailsResponse(response: Future[Either[HttpException, PSAMinimalDetails]]): Unit = this.minimalPsaDetailsResponse = response

    def getPSAMinimalDetails(psaId: String)(implicit
                                            headerCarrier: HeaderCarrier,
                                            ec: ExecutionContext): Future[Either[HttpException, PSAMinimalDetails]] = minimalPsaDetailsResponse

    override def acceptInvitation(invitation: AcceptedInvitation)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext):
    Future[Either[HttpException, Unit]] = Future.successful(Right(()))
  }

  val fakeAssociationConnector = new FakeAssociationConnector


  val controller = new AssociationController(fakeAssociationConnector)
}


