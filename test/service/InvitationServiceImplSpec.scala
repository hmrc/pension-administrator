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

import base.{JsonFileReader, SpecBase}
import connectors.AssociationConnector
import models.{AcceptedInvitation, PSAMinimalDetails}
import org.scalatest.{AsyncFlatSpec, EitherValues, Matchers}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException}

import scala.concurrent.{ExecutionContext, Future}

class InvitationServiceImplSpec extends AsyncFlatSpec with Matchers with EitherValues {

  import FakeAssociationConnector._
  import InvitationServiceImplSpec._

  "registerPSA" should "return the result from the connector" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson).map {
      httpResponse =>
        httpResponse.right.value shouldBe inviteeMinimalPsaDetails
    }

  }
  it should "throw BadRequestException if the JSON cannot be parsed as Invitation" in {

    val fixture = testFixture()

    recoverToSucceededIf[BadRequestException] {
      fixture.invitationService.invitePSA(Json.obj())
    }

  }
}

object InvitationServiceImplSpec extends SpecBase {

  trait TestFixture {
    val associationConnector: FakeAssociationConnector = new FakeAssociationConnector()
    val invitationService: InvitationServiceImpl = new InvitationServiceImpl(associationConnector) {
    }
  }

  def testFixture(): TestFixture = new TestFixture {}

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  val invitationJson: JsValue = readJsonFromFile("/data/validInvitation.json")

}

class FakeAssociationConnector extends AssociationConnector {

  import FakeAssociationConnector._
  def getPSAMinimalDetails(psaId : String)(implicit
                                           headerCarrier: HeaderCarrier,
                                           ec: ExecutionContext): Future[Either[HttpException,PSAMinimalDetails]] =
    Future.successful(Right(inviteeMinimalPsaDetails.as[PSAMinimalDetails]))

  override def acceptInvitation(invitation: AcceptedInvitation)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpException, Unit]] = ???
}

object FakeAssociationConnector extends JsonFileReader {

  val inviteeMinimalPsaDetails = Json.toJson(readJsonFromFile("/data/validMinimalPsaDetails.json"))

}
