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

import audit.InvitationAuditEvent
import config.AppConfig
import connectors.{AssociationConnector, SchemeConnector}
import controllers.EmailResponseControllerSpec.fakeAuditService
import models.{AcceptedInvitation, IndividualDetails, Invitation, PSAMinimalDetails, _}
import org.joda.time.LocalDate
import org.mockito.Matchers.any
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncFlatSpec, EitherValues, Matchers, OptionValues}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsBoolean, JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.FakeRequest
import repositories.InvitationsCacheRepository
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http._
import utils.{DateHelper, FakeEmailConnector}

import scala.concurrent.{ExecutionContext, Future}

class InvitationServiceImplSpec extends AsyncFlatSpec with Matchers with EitherValues with OptionValues with MockitoSugar {

  import InvitationServiceImplSpec._

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  "registerPSA" should "return successfully when an individual PSA exists and names match" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(johnDoePsaId, johnDoe.individualDetails.value.name)).map {
      response =>
        verify(fixture.repository, times(1)).insert(any())(any())
        response.right.value should equal(())
    }

  }

  it should "return successfully when an organisation PSA exists and names match" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(acmeLtdPsaId, acmeLtd.organisationName.value)).map {
      response =>
        response.right.value should equal (())
    }

  }

  it should "return ConflictException when an organisation PSA exists and names match but invite already exists" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(acmeLtdPsaId, acmeLtd.organisationName.value)).map {
      response =>
        verify(fixture.repository, times(1)).insert(any())(any())
        response.right.value should equal(())
    }

  }

  it should "audit successfully when an organisation PSA exists and names match" in {
    val fixture = testFixture()
    fixture.invitationService.invitePSA(invitationJson(acmeLtdPsaId, acmeLtd.organisationName.value)).map {
      _ =>
        val i = invitation(acmeLtdPsaId, acmeLtd.organisationName.value)
        fakeAuditService.verifySent(InvitationAuditEvent(i)) should equal(true)
    }
  }

  it should "throw BadRequestException if the JSON cannot be parsed as Invitation" in {

    val fixture = testFixture()

    recoverToSucceededIf[BadRequestException] {
      fixture.invitationService.invitePSA(Json.obj())
    }

  }

  it should "throw BadRequestException if the Invitation is for a PSA already associated to that scheme" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(associatedPsaId, johnDoe.individualDetails.value.name)) map { response =>
      response.left.value shouldBe a[BadRequestException]
    }

  }

  it should "throw InternalServerException if check for association is not a boolean" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(invalidResponsePsaId, johnDoe.individualDetails.value.name)) map { response =>
      response.left.value shouldBe a[InternalServerException]
    }

  }

  it should "return NotFoundException if the PSA Id cannot be found" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(notFoundPsaId, "")) map {
      response =>
        verify(fixture.repository, never).insert(any())(any())
        response.left.value shouldBe a[NotFoundException]
        response.left.value.message should include("NOT_FOUND")
    }

  }

  it should "return NotFoundException if an individual PSA Id can be found but the name does not match" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(johnDoePsaId, "Wrong Name")) map {
      response =>
        verify(fixture.repository, never).insert(any())(any())
        response.left.value shouldBe a[NotFoundException]
        response.left.value.message should include("NOT_FOUND")
    }

  }

  it should "return NotFoundException if an organisation PSA Id can be found but the name does not match" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(acmeLtdPsaId, "Wrong Name")) map {
      response =>
        verify(fixture.repository, never).insert(any())(any())
        response.left.value shouldBe a[NotFoundException]
        response.left.value.message should include("NOT_FOUND")
    }

  }

  it should "match an individual when the invitation includes their middle name" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(joeBloggsPsaId, joeBloggs.individualDetails.value.fullName)) map {
      response =>
        verify(fixture.repository, times(1)).insert(any())(any())
        response.right.value should equal(())
    }

  }

  it should "match an individual when they have a middle name not specified in the invitation" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(joeBloggsPsaId, joeBloggs.individualDetails.value.name)) map {
      response =>
        verify(fixture.repository, times(1)).insert(any())(any())
        response.right.value should equal(())
    }

  }

  it should "return MongoDBFailedException if insertion failed with RunTimeException from mongodb" in {

    val fixture = testFixture()
    when(fixture.repository.insert(any())(any())).thenReturn(Future.failed(new RuntimeException("failed to perform DB operation")))
    fixture.invitationService.invitePSA(invitationJson(joeBloggsPsaId, joeBloggs.individualDetails.value.name)) map {
      response =>
        verify(fixture.repository, times(1)).insert(any())(any())
        response.left.value shouldBe a[MongoDBFailedException]
        response.left.value.message should include("failed to perform DB operation")
    }
  }

  it should "send an email to the invitee" in {

    import FakeEmailConnector._

    val expectedEmail =
      SendEmailRequest(
        List(johnDoeEmail),
        "pods_psa_invited",
        Map(
          "inviteeName" -> johnDoe.individualDetails.value.fullName,
          "schemeName" -> testSchemeName,
          "expiryDate" -> DateHelper.formatDate(LocalDate.now().plusDays(FakeConfig.invitationExpiryDays))
        )
      )

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(johnDoePsaId, johnDoe.individualDetails.value.name)).map {
      _ =>
        fixture.emailConnector.sentEmails should containEmail(expectedEmail)
    }

  }

}

object InvitationServiceImplSpec extends MockitoSugar {

  val config: AppConfig = {
    new GuiceApplicationBuilder().build().injector.instanceOf[AppConfig]
  }

  trait TestFixture {

    val associationConnector: FakeAssociationConnector = new FakeAssociationConnector()
    val repository: InvitationsCacheRepository = mock[InvitationsCacheRepository]
    val fakeSchemeConnector: SchemeConnector = new FakeSchemeConnector()

    val emailConnector: FakeEmailConnector = new FakeEmailConnector()

    val invitationService: InvitationServiceImpl =
      new InvitationServiceImpl(
        associationConnector,
        emailConnector,
        config,
        repository,
        fakeAuditService,
        fakeSchemeConnector
      )

    when(repository.insert(any())(any()))
      .thenReturn(Future.successful(true))
  }

  def testFixture(): TestFixture = new TestFixture() {}

  val testSchemeName = "test-scheme"

  def invitation(inviteePsaId: PsaId, inviteeName: String): Invitation =
    Invitation("test-pstr", testSchemeName, PsaId("A7654321"), inviteePsaId, inviteeName)

  def invitationJson(inviteePsaId: PsaId, inviteeName: String): JsValue =
    Json.toJson(invitation(inviteePsaId, inviteeName))

  val johnDoePsaId = PsaId("A2000001")
  val johnDoeEmail = "john.doe@email.com"
  val johnDoe = PSAMinimalDetails(johnDoeEmail, false, None, Some(IndividualDetails("John", None, "Doe")))

  val joeBloggsPsaId = PsaId("A2000002")
  val joeBloggs = PSAMinimalDetails("joe.bloggs@email.com", false, None, Some(IndividualDetails("Joe", Some("Herbert"), "Bloggs")))

  val acmeLtdPsaId = PsaId("A2000003")
  val acmeLtd = PSAMinimalDetails("info@acme.com", false, Some("Acme Ltd"), None)

  val notFoundPsaId = PsaId("A2000004")
  val associatedPsaId = PsaId("A2000005")
  val invalidResponsePsaId = PsaId("A2000006")

  object FakeConfig {
    val invitationExpiryDays: Int = 30
  }

}

class FakeAssociationConnector extends AssociationConnector {

  import InvitationServiceImplSpec._

  def getPSAMinimalDetails(psaId: PsaId)
                          (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpException, PSAMinimalDetails]] = {

    psaId match {
      case `johnDoePsaId` | `associatedPsaId` | `invalidResponsePsaId` => Future.successful(Right(johnDoe))
      case `notFoundPsaId` => Future.successful(Left(new NotFoundException("NOT_FOUND")))
      case `joeBloggsPsaId` => Future.successful(Right(joeBloggs))
      case `acmeLtdPsaId` => Future.successful(Right(acmeLtd))
      case unknownPsaId => throw new IllegalArgumentException(s"FakeAssociationConnector cannot handle PSA Id $unknownPsaId")
    }

  }

  override def acceptInvitation(invitation: AcceptedInvitation)
    (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpException, Unit]] = {
    throw new NotImplementedError()
  }

}

class FakeSchemeConnector extends SchemeConnector {

  import InvitationServiceImplSpec._

  override def checkForAssociation(psaId: PsaId, srn: SchemeReferenceNumber)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, JsValue]] = {
    Future.successful {
      if (psaId equals invalidResponsePsaId) {
        Right(Json.obj())
      } else {
        Right(JsBoolean(psaId equals associatedPsaId))
      }
    }
  }

}
