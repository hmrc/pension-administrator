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

import connectors.AssociationConnector
import models.{AcceptedInvitation, IndividualDetails, Invitation, PSAMinimalDetails}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito.{verify, when, times, never}
import org.mockito.Matchers.{any}
import org.scalatest.{AsyncFlatSpec, EitherValues, Matchers, OptionValues}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import repositories.InvitationsCacheRepository
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

class InvitationServiceImplSpec extends AsyncFlatSpec with Matchers with EitherValues with OptionValues with MockitoSugar{

  import InvitationServiceImplSpec._

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  "registerPSA" should "return successfully when an individual PSA exists and names match" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(johnDoePsaId, johnDoe.individualDetails.value.name)).map {
      response =>
        verify(fixture.repository, times(1)).insert(any(), any(), any())(any())
        response.right.value should equal (true)
    }

  }

  it should "return successfully when an organisation PSA exists and names match" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(acmeLtdPsaId, acmeLtd.organisationName.value)).map {
      response =>
        verify(fixture.repository, times(1)).insert(any(), any(), any())(any())
        response.right.value should equal (true)
    }

  }

  it should "throw BadRequestException if the JSON cannot be parsed as Invitation" in {

    val fixture = testFixture()

    recoverToSucceededIf[BadRequestException] {
      fixture.invitationService.invitePSA(Json.obj())
    }

  }

  it should "return NotFoundException if the PSA Id cannot be found" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(notFoundPsaId, "")) map {
      response =>
        verify(fixture.repository, never).insert(any(), any(), any())(any())
        response.left.value shouldBe a[NotFoundException]
        response.left.value.message should include ("NOT_FOUND")
    }

  }

  it should "return NotFoundException if an individual PSA Id can be found but the name does not match" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(johnDoePsaId, "Wrong Name")) map {
      response =>
        verify(fixture.repository, never).insert(any(), any(), any())(any())
        response.left.value shouldBe a[NotFoundException]
        response.left.value.message should include ("NOT_FOUND")
    }

  }

  it should "return NotFoundException if an organisation PSA Id can be found but the name does not match" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(acmeLtdPsaId, "Wrong Name")) map {
      response =>
        verify(fixture.repository, never).insert(any(), any(), any())(any())
        response.left.value shouldBe a[NotFoundException]
        response.left.value.message should include ("NOT_FOUND")
    }

  }

  it should "match an individual when the invitation includes their middle name" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(joeBloggsPsaId, joeBloggs.individualDetails.value.fullName)) map {
      response =>
        verify(fixture.repository, times(1)).insert(any(), any(), any())(any())
        response.right.value should equal (true)
    }

  }

  it should "match an individual when they have a middle name not specified in the invitation" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(joeBloggsPsaId, joeBloggs.individualDetails.value.name)) map {
      response =>
        verify(fixture.repository, times(1)).insert(any(), any(), any())(any())
        response.right.value should equal (true)
    }

  }

  it should "return RuntimeException if insertion failed for mongodb" in {

    val fixture = testFixture()
    when(fixture.repository.insert(any(), any(), any())(any())).thenReturn(Future.failed(new RuntimeException("failed to perform DB operation")))
    fixture.invitationService.invitePSA(invitationJson(joeBloggsPsaId, joeBloggs.individualDetails.value.name)) map {
      response =>
        verify(fixture.repository, times(1)).insert(any(), any(), any())(any())
        response.left.value shouldBe a[MongoDBFailedException]
        response.left.value.message should include ("failed to perform DB operation")
    }

  }

}

object InvitationServiceImplSpec extends MockitoSugar{

  trait TestFixture {
    val associationConnector: FakeAssociationConnector = new FakeAssociationConnector()
    val repository: InvitationsCacheRepository = mock[InvitationsCacheRepository]
    val invitationService: InvitationServiceImpl = new InvitationServiceImpl(associationConnector, repository) {
      when(repository.insert(any(), any(), any())(any())).thenReturn(Future.successful(true))
    }
  }

  def testFixture(): TestFixture = new TestFixture {}

  def invitationJson(inviteePsaId: String, inviteeName: String): JsValue =
    Json.toJson(Invitation("test-pstr", "test-scheme", "test-inviter-psa-id", inviteePsaId, inviteeName))

  val johnDoePsaId = "A2000001"
  val notFoundPsaId = "A2000002"
  val joeBloggsPsaId = "A2000003"
  val acmeLtdPsaId = "A2000004"

  val johnDoe = PSAMinimalDetails("john.doe@email.com", false, None, Some(IndividualDetails("John", None, "Doe")))
  val joeBloggs = PSAMinimalDetails("joe.bloggs@email.com", false, None, Some(IndividualDetails("Joe", Some("Herbert"), "Bloggs")))
  val acmeLtd = PSAMinimalDetails("info@acme.com", false, Some("Acme Ltd"), None)

}

class FakeAssociationConnector extends AssociationConnector {

  import InvitationServiceImplSpec._

  def getPSAMinimalDetails(psaId : String)
    (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpException, PSAMinimalDetails]] = {

    psaId match {
      case `johnDoePsaId` => Future.successful(Right(johnDoe))
      case `notFoundPsaId` => Future.successful(Left(new NotFoundException("NOT_FOUND")))
      case `joeBloggsPsaId` => Future.successful(Right(joeBloggs))
      case `acmeLtdPsaId` => Future.successful(Right(acmeLtd))
      case unknownPsaId => throw new IllegalArgumentException(s"FakeAssociationConnector cannot handle PSA Id $unknownPsaId")
    }

  }

  //noinspection NotImplementedCode
  override def acceptInvitation(invitation: AcceptedInvitation)
    (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpException, Unit]] = ???

}
