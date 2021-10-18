/*
 * Copyright 2021 HM Revenue & Customs
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
import models.enumeration.JourneyType
import models.{AcceptedInvitation, IndividualDetails, Invitation, MinimalDetails, _}
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, times, verify, when}
import org.mockito.MockitoSugar
import org.scalatest.{AsyncFlatSpec, EitherValues, Matchers, OptionValues}
import play.api.Application
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsBoolean, JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.InvitationsCacheRepository
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException, NotFoundException, _}
import utils.{DateHelper, FakeEmailConnector}

import scala.concurrent.{ExecutionContext, Future}

class InvitationServiceImplSpec extends AsyncFlatSpec with Matchers with EitherValues with OptionValues with MockitoSugar {

  import InvitationServiceImplSpec._

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")


  "registerPSA" should "return successfully when an individual PSA exists and names match" in {
    running() { app =>
      val fixture = testFixture(app)

      fixture.invitationService.invitePSA(invitationJson(johnDoePsaId, johnDoe.individualDetails.value.name)).map {
        response =>
          verify(fixture.repository, times(1)).insert(any())(any())
          response.right.value should equal(())
      }
    }
  }

  it should "throw exception when no organisation name or individual details returned in minimal details" in {
    running() { app =>
      val fixture = testFixture(app)
      recoverToSucceededIf[IllegalArgumentException](
        fixture.invitationService.invitePSA(invitationJson(blankPsaId, johnDoe.individualDetails.value.name))
      )
    }
  }

  it should "return successfully when an organisation PSA exists and names match" in {
    running() { app =>
      val fixture = testFixture(app)

      fixture.invitationService.invitePSA(invitationJson(acmeLtdPsaId, acmeLtd.organisationName.value)).map {
        response =>
          response.right.value should equal(())
      }
    }
  }

  it should "return ConflictException when an organisation PSA exists and names match but invite already exists" in {
    running() { app =>
      val fixture = testFixture(app)

      fixture.invitationService.invitePSA(invitationJson(acmeLtdPsaId, acmeLtd.organisationName.value)).map {
        response =>
          verify(fixture.repository, times(1)).insert(any())(any())
          response.right.value should equal(())
      }
    }
  }

  it should "audit successfully when an organisation PSA exists and names match" in {
    running() { app =>
      val fixture = testFixture(app)
      fixture.invitationService.invitePSA(invitationJson(acmeLtdPsaId, acmeLtd.organisationName.value)).map {
        _ =>
          val i = invitation(acmeLtdPsaId, acmeLtd.organisationName.value)
          fakeAuditService.verifySent(InvitationAuditEvent(i)) should equal(true)
      }
    }
  }

  it should "throw BadRequestException if the JSON cannot be parsed as Invitation" in {
    running() { app =>
      val fixture = testFixture(app)

      recoverToSucceededIf[BadRequestException] {
        fixture.invitationService.invitePSA(Json.obj())
      }
    }
  }

  it should "throw ForbiddenException if the Invitation is for a PSA already associated to that scheme" in {
    running() { app =>
      val fixture = testFixture(app)

      fixture.invitationService.invitePSA(invitationJson(associatedPsaId, johnDoe.individualDetails.value.name)) map { response =>
        response.left.value shouldBe a[ForbiddenException]
      }
    }
  }

  it should "throw InternalServerException if check for association is not a boolean" in {
    running() { app =>
      val fixture = testFixture(app)

      fixture.invitationService.invitePSA(invitationJson(invalidResponsePsaId, johnDoe.individualDetails.value.name)) map { response =>
        response.left.value shouldBe a[InternalServerException]
      }
    }
  }

  it should "relay HttpExceptions throw from SchemeConnector" in {
    running() { app =>
      val fixture = testFixture(app)

      fixture.invitationService.invitePSA(invitationJson(exceptionResponsePsaId, johnDoe.individualDetails.value.name)) map { response =>
        response.left.value shouldBe a[NotFoundException]
      }
    }
  }

  it should "return NotFoundException if the PSA Id cannot be found" in {
    running() { app =>
      val fixture = testFixture(app)

      fixture.invitationService.invitePSA(invitationJson(notFoundPsaId, "")) map {
        response =>
          verify(fixture.repository, never).insert(any())(any())
          response.left.value shouldBe a[NotFoundException]
          response.left.value.message should include("NOT_FOUND")
      }

    }
  }

  it should "return NotFoundException if an individual PSA Id can be found but the name does not match" in {
    running() { app =>
      val fixture = testFixture(app)

      fixture.invitationService.invitePSA(invitationJson(johnDoePsaId, "Wrong Name")) map {
        response =>
          verify(fixture.repository, never).insert(any())(any())
          response.left.value shouldBe a[NotFoundException]
          response.left.value.message should include("The name and PSA Id do not match")
      }
    }
  }

  it should "return NotFoundException if an organisation PSA Id can be found but the name does not match" in {
    running() { app =>
      val fixture = testFixture(app)

      fixture.invitationService.invitePSA(invitationJson(acmeLtdPsaId, "Wrong Name")) map {
        response =>
          verify(fixture.repository, never).insert(any())(any())
          response.left.value shouldBe a[NotFoundException]
          response.left.value.message should include("The name and PSA Id do not match")
      }
    }
  }

  it should "return ForbiddenException if an organisation PSA Id can be found and the name matches and already associated" in {
    running() { app =>
      val fixture = testFixture(app)
      fixture.invitationService.invitePSA(invitationJson(associatedPsaId, johnDoe.individualDetails.value.name)) map {
        response =>
          response.left.value shouldBe a[ForbiddenException]
          response.left.value.message should include("The invitation is to a PSA already associated with this scheme")
      }
    }
  }

  it should "return NotFoundException if an organisation PSA Id can be found and the name doesn't match and already associated" in {
    running() { app =>
      val fixture = testFixture(app)
      fixture.invitationService.invitePSA(invitationJson(associatedPsaId, "waa")) map {
        response =>
          response.left.value shouldBe a[NotFoundException]
          response.left.value.message should include("The name and PSA Id do not match")
      }
    }
  }

  it should "match an individual when the invitation includes their middle name" in {
    running() { app =>
      val fixture = testFixture(app)

      fixture.invitationService.invitePSA(invitationJson(joeBloggsPsaId, joeBloggs.individualDetails.value.fullName)) map {
        response =>
          verify(fixture.repository, times(1)).insert(any())(any())
          response.right.value should equal(())
      }
    }
  }

  it should "match an individual when they have a middle name not specified in the invitation" in {
    running() { app =>
      val fixture = testFixture(app)

      fixture.invitationService.invitePSA(invitationJson(joeBloggsPsaId, joeBloggs.individualDetails.value.name)) map {
        response =>
          verify(fixture.repository, times(1)).insert(any())(any())
          response.right.value should equal(())
      }
    }
  }

  it should "return MongoDBFailedException if insertion failed with RunTimeException from mongodb" in {
    running() { app =>
      val fixture = testFixture(app)
      when(fixture.repository.insert(any())(any())).thenReturn(Future.failed(new RuntimeException("failed to perform DB operation")))
      fixture.invitationService.invitePSA(invitationJson(joeBloggsPsaId, joeBloggs.individualDetails.value.name)) map {
        response =>
          verify(fixture.repository, times(1)).insert(any())(any())
          response.left.value shouldBe a[MongoDBFailedException]
          response.left.value.message should include("failed to perform DB operation")
      }
    }
  }

  it should "send an email to the invitee" in {
    running() { app =>

      import FakeEmailConnector._
      val fixture = testFixture(app)
      val encryptedPsaId = fixture.crypto.QueryParameterCrypto.encrypt(PlainText(johnDoePsaId.value)).value

      val expectedEmail =
        SendEmailRequest(
          List(johnDoeEmail),
          "pods_psa_invited",
          Map(
            "inviteeName" -> johnDoe.individualDetails.value.fullName,
            "schemeName" -> testSchemeName,
            "expiryDate" -> DateHelper.formatDate(expiryDate.toLocalDate)
          ),
          force = false,
          Some(fixture.config.invitationCallbackUrl.format(JourneyType.INVITE, encryptedPsaId))
        )

      fixture.invitationService.invitePSA(invitationJson(johnDoePsaId, johnDoe.individualDetails.value.name)).map {
        _ =>
          fixture.emailConnector.sentEmails should containEmail(expectedEmail)
      }
    }
  }

}

object InvitationServiceImplSpec extends MockitoSugar {

  trait TestFixture {

    def runningApp: Application = new GuiceApplicationBuilder().build()

    val injector: Injector = runningApp.injector

    val config: AppConfig = injector.instanceOf[AppConfig]
    val associationConnector: FakeAssociationConnector = new FakeAssociationConnector()
    val repository: InvitationsCacheRepository = mock[InvitationsCacheRepository]
    val fakeSchemeConnector: SchemeConnector = new FakeSchemeConnector()
    val crypto: ApplicationCrypto = injector.instanceOf[ApplicationCrypto]
    val emailConnector: FakeEmailConnector = new FakeEmailConnector()

    val invitationService: InvitationServiceImpl =
      new InvitationServiceImpl(
        associationConnector,
        emailConnector,
        config,
        repository,
        fakeAuditService,
        fakeSchemeConnector,
        crypto
      )

    when(repository.insert(any())(any()))
      .thenReturn(Future.successful(true))
  }

  def testFixture(app: Application): TestFixture = new TestFixture() {
    override def runningApp: Application = app
  }

  val testSchemeName = "test-scheme"
  val inviterPsaId: PsaId = PsaId("A7654321")

  def invitation(inviteePsaId: PsaId, inviteeName: String): Invitation =
    Invitation(testSrn, "test-pstr", testSchemeName, inviterPsaId, inviteePsaId, inviteeName, expiryDate)

  def invitationJson(inviteePsaId: PsaId, inviteeName: String): JsValue =
    Json.toJson(Invitation(testSrn, "test-pstr", testSchemeName, inviterPsaId, inviteePsaId, inviteeName, expiryDate))

  val testSrn: SchemeReferenceNumber = SchemeReferenceNumber("S0987654321")

  val johnDoePsaId: PsaId = PsaId("A2000001")
  val johnDoeEmail: String = "john.doe@email.com"
  val johnDoe: MinimalDetails = MinimalDetails(johnDoeEmail, isPsaSuspended = false, organisationName = None,
    individualDetails = Some(IndividualDetails("John", None, "Doe")), rlsFlag = true, deceasedFlag = true)

  val expiryDate: DateTime = new DateTime("2018-10-10")

  val joeBloggsPsaId: PsaId = PsaId("A2000002")
  val joeBloggs: MinimalDetails = MinimalDetails("joe.bloggs@email.com", isPsaSuspended = false, None,
    Some(IndividualDetails("Joe", Some("Herbert"), "Bloggs")), rlsFlag = true, deceasedFlag = true)

  val acmeLtdPsaId: PsaId = PsaId("A2000003")
  val blankPsaId: PsaId = PsaId("A2222222")
  val acmeLtd: MinimalDetails = MinimalDetails("info@acme.com", isPsaSuspended = false, Some("Acme Ltd"), None,
    rlsFlag = true,
    deceasedFlag = true)
  val blank: MinimalDetails = MinimalDetails("info@acme.com", isPsaSuspended = false, None, None,
    rlsFlag = true,
    deceasedFlag = true)

  val notFoundPsaId: PsaId = PsaId("A2000004")
  val associatedPsaId: PsaId = PsaId("A2000005")
  val invalidResponsePsaId: PsaId = PsaId("A2000006")
  val exceptionResponsePsaId: PsaId = PsaId("A2000007")

  object FakeConfig {
    val invitationExpiryDays: Int = 30
  }

}

class FakeAssociationConnector extends AssociationConnector {

  import InvitationServiceImplSpec._

  def getMinimalDetails(idValue: String, idType: String, regime: String)
                          (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext,
                           request: RequestHeader): Future[Either[HttpException, MinimalDetails]] = {

    PsaId(idValue) match {
      case `johnDoePsaId` | `associatedPsaId` => Future.successful(Right(johnDoe))
      case `joeBloggsPsaId` | `exceptionResponsePsaId` => Future.successful(Right(joeBloggs))
      case `acmeLtdPsaId` | `invalidResponsePsaId` => Future.successful(Right(acmeLtd))
      case `blankPsaId` => Future.successful(Right(blank))
      case `notFoundPsaId` => Future.successful(Left(new NotFoundException("NOT_FOUND")))
      case unknownPsaId => throw new IllegalArgumentException(s"FakeAssociationConnector cannot handle PSA Id $unknownPsaId")
    }

  }


  def findMinimalDetailsByID(idValue: String, idType: String, regime: String)(implicit
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext,
    request: RequestHeader): Future[Either[HttpException, Option[MinimalDetails]]] =
    getMinimalDetails(idValue, idType, regime).map(_.map(Option(_)))


  override def acceptInvitation(invitation: AcceptedInvitation)
    (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, requestHeader: RequestHeader): Future[Either[HttpException, Unit]] = {
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
      } else if (psaId equals exceptionResponsePsaId) {
        Left(new NotFoundException("Cannot find this endpoint"))
      } else {
        Right(JsBoolean(psaId equals associatedPsaId))
      }
    }
  }

  override def listOfSchemes(psaId: String)(implicit headerCarrier: HeaderCarrier,
                                            ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, JsValue]] = ???

  override def getSchemeDetails(psaId: String, schemeIdType: String, idNumber: String)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpException, JsValue]] = ???
}
