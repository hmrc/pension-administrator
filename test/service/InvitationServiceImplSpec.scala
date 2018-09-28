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

import config.AppConfig
import connectors.AssociationConnector
import connectors.helper.FakeSchemeConnector
import models.{AcceptedInvitation, IndividualDetails, Invitation, PSAMinimalDetails, _}
import org.joda.time.LocalDate
import org.scalatest.{AsyncFlatSpec, EitherValues, Matchers, OptionValues}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException, NotFoundException}
import utils.{DateHelper, FakeEmailConnector}

import scala.concurrent.{ExecutionContext, Future}

class InvitationServiceImplSpec extends AsyncFlatSpec with Matchers with EitherValues with OptionValues {

  import InvitationServiceImplSpec._

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  "registerPSA" should "return successfully when an individual PSA exists and names match" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(johnDoePsaId, johnDoe.individualDetails.value.name)).map {
      response =>
        response.right.value should equal (())
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
        response.right.value should equal (())
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
        response.left.value shouldBe a[NotFoundException]
        response.left.value.message should include ("NOT_FOUND")
    }

  }

  it should "return NotFoundException if an individual PSA Id can be found but the name does not match" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(johnDoePsaId, "Wrong Name")) map {
      response =>
        response.left.value shouldBe a[NotFoundException]
        response.left.value.message should include ("NOT_FOUND")
    }

  }

  it should "return NotFoundException if an organisation PSA Id can be found but the name does not match" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(acmeLtdPsaId, "Wrong Name")) map {
      response =>
        response.left.value shouldBe a[NotFoundException]
        response.left.value.message should include ("NOT_FOUND")
    }

  }

  it should "match an individual when the invitation includes their middle name" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(joeBloggsPsaId, joeBloggs.individualDetails.value.fullName)) map {
      response =>
        response.right.value should equal (())
    }

  }

  it should "match an individual when they have a middle name not specified in the invitation" in {

    val fixture = testFixture()

    fixture.invitationService.invitePSA(invitationJson(joeBloggsPsaId, joeBloggs.individualDetails.value.name)) map {
      response =>
        response.right.value should equal (())
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

object InvitationServiceImplSpec {

  val config: AppConfig = {
    new GuiceApplicationBuilder().build().injector.instanceOf[AppConfig]
  }

  trait TestFixture {

    val associationConnector: FakeAssociationConnector = new FakeAssociationConnector()
    val fakeSchemeConnector: FakeSchemeConnector = new FakeSchemeConnector()
    val emailConnector: FakeEmailConnector = new FakeEmailConnector()

    val invitationService: InvitationServiceImpl =
      new InvitationServiceImpl(
        associationConnector,
        emailConnector,
        config,
        fakeSchemeConnector
      )

  }

  def testFixture(): TestFixture = new TestFixture() {}

  val testSchemeName = "test-scheme"

  def invitationJson(inviteePsaId: String, inviteeName: String): JsValue =
    Json.toJson(Invitation("test-pstr", testSchemeName, "test-inviter-psa-id", inviteePsaId, inviteeName))

  val johnDoePsaId = "A2000001"
  val johnDoeEmail = "john.doe@email.com"
  val johnDoe = PSAMinimalDetails(johnDoeEmail, false, None, Some(IndividualDetails("John", None, "Doe")))

  val joeBloggsPsaId = "A2000002"
  val joeBloggs = PSAMinimalDetails("joe.bloggs@email.com", false, None, Some(IndividualDetails("Joe", Some("Herbert"), "Bloggs")))

  val acmeLtdPsaId = "A2000003"
  val acmeLtd = PSAMinimalDetails("info@acme.com", false, Some("Acme Ltd"), None)

  val notFoundPsaId = "A2000004"

  object FakeConfig {
    val invitationExpiryDays: Int = 30
  }

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

  override def acceptInvitation(invitation: AcceptedInvitation)
    (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpException, Unit]] = {
    throw new NotImplementedError()
  }

}

