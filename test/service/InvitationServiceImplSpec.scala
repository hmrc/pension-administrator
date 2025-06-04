/*
 * Copyright 2025 HM Revenue & Customs
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

import audit.{AuditService, InvitationAuditEvent, StubSuccessfulAuditService}
import config.AppConfig
import connectors.{AssociationConnector, EmailConnector, SchemeConnector}
import models.*
import models.enumeration.JourneyType
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, EitherValues, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Format, JsValue, Json, OWrites}
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.FakeRequest
import repositories.*
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.domain.{PsaId, PspId}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException, NotFoundException, *}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import utils.FakeEmailConnector.containEmail
import utils.{DateHelper, FakeEmailConnector}

import java.time.{Clock, Instant, LocalDate, ZoneOffset}
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class InvitationServiceImplSpec extends AsyncFlatSpec
          with Matchers with EitherValues with OptionValues
          with MockitoSugar with BeforeAndAfterEach {

  import InvitationServiceImplSpec.*

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  override def beforeEach(): Unit = {
    fakeAuditService.reset()
    reset(invitationsCacheRepository)
    when(invitationsCacheRepository.upsert(any())(using any()))
      .thenReturn(Future.successful(()))
  }

  "registerPSA" should "return successfully when an individual PSA exists and names match" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    invitationService.invitePSA(invitationJson(johnDoePsaId, johnDoe.individualDetails.value.name)).map {
      response =>
        verify(invitationsCacheRepository, times(1)).upsert(any())(using any())
        response.value.shouldEqual(())
    }
  }

  it should "throw exception when no organisation name or individual details returned in minimal details" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    recoverToSucceededIf[IllegalArgumentException](
      invitationService.invitePSA(invitationJson(blankPsaId, johnDoe.individualDetails.value.name))
    )
  }

  it should "return successfully when an organisation PSA exists and names match" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    invitationService.invitePSA(invitationJson(acmeLtdPsaId, acmeLtd.organisationName.value)).map {
      response =>
        response.value.shouldEqual(())
    }
  }

  it should "return ConflictException when an organisation PSA exists and names match but invite already exists" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    invitationService.invitePSA(invitationJson(acmeLtdPsaId, acmeLtd.organisationName.value)).map {
      response =>
        verify(invitationsCacheRepository, times(1)).upsert(any())(using any())
        response.value.shouldEqual(())
    }
  }

  it should "audit successfully when an organisation PSA exists and names match" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    invitationService.invitePSA(invitationJson(acmeLtdPsaId, acmeLtd.organisationName.value)).map {
      _ =>
        val i = invitation(acmeLtdPsaId, acmeLtd.organisationName.value)
        fakeAuditService.verifySent(InvitationAuditEvent(i)).shouldEqual(true)
    }
  }

  it should "throw BadRequestException if the JSON cannot be parsed as Invitation" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    recoverToSucceededIf[BadRequestException] {
      invitationService.invitePSA(Json.obj())
    }
  }

  it should "throw ForbiddenException if the Invitation is for a PSA already associated to that scheme" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    invitationService.invitePSA(invitationJson(associatedPsaId, johnDoe.individualDetails.value.name)) map { response =>
      response.left.value.shouldBe(a[ForbiddenException])
    }
  }

  it should "throw InternalServerException if check for association is not a boolean" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    invitationService.invitePSA(invitationJson(invalidResponsePsaId, johnDoe.individualDetails.value.name)) map { response =>
      response.left.value.shouldBe(a[InternalServerException])
    }
  }

  it should "relay HttpExceptions throw from SchemeConnector" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    invitationService.invitePSA(invitationJson(exceptionResponsePsaId, johnDoe.individualDetails.value.name)) map { response =>
      response.left.value.shouldBe(a[NotFoundException])
    }
  }

  it should "return NotFoundException if the PSA Id cannot be found" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    invitationService.invitePSA(invitationJson(notFoundPsaId, "")) map {
      response =>
        verify(invitationsCacheRepository, never).upsert(any())(using any())
        response.left.value.shouldBe(a[NotFoundException])
        response.left.value.message.should(include("NOT_FOUND"))
    }
  }

  it should "return NotFoundException if an individual PSA Id can be found but the name does not match" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    invitationService.invitePSA(invitationJson(johnDoePsaId, "Wrong Name")) map {
      response =>
        verify(invitationsCacheRepository, never).upsert(any())(using any())
        response.left.value.shouldBe(a[NotFoundException])
        response.left.value.message.should(include("The name and PSA Id do not match"))
    }
  }

  it should "return NotFoundException if an organisation PSA Id can be found but the name does not match" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    invitationService.invitePSA(invitationJson(acmeLtdPsaId, "Wrong Name")) map {
      response =>
        verify(invitationsCacheRepository, never).upsert(any())(using any())
        response.left.value.shouldBe(a[NotFoundException])
        response.left.value.message.should(include("The name and PSA Id do not match"))
    }
  }

  it should "return ForbiddenException if an organisation PSA Id can be found and the name matches and already associated" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    invitationService.invitePSA(invitationJson(associatedPsaId, johnDoe.individualDetails.value.name)) map {
      response =>
        response.left.value.shouldBe(a[ForbiddenException])
        response.left.value.message.should(include("The invitation is to a PSA already associated with this scheme"))
    }
  }

  it should "return NotFoundException if an organisation PSA Id can be found and the name doesn't match and already associated" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    invitationService.invitePSA(invitationJson(associatedPsaId, "waa")) map {
      response =>
        response.left.value.shouldBe(a[NotFoundException])
        response.left.value.message.should(include("The name and PSA Id do not match"))
    }
  }

  it should "match an individual when the invitation includes their middle name" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    invitationService.invitePSA(invitationJson(joeBloggsPsaId, joeBloggs.individualDetails.value.fullName)) map {
      response =>
        verify(invitationsCacheRepository, times(1)).upsert(any())(using any())
        response.value.shouldEqual(())
    }
  }

  it should "match lineantly an individual when the invitation includes their middle name" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    invitationService.invitePSA(invitationJson(johnDoePsaId, "John Paul Doe")) map {
      response =>
        verify(invitationsCacheRepository, times(1)).upsert(any())(using any())
        response.value.shouldEqual(())
    }
  }

  it should "match an individual when they have a middle name not specified in the invitation" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    invitationService.invitePSA(invitationJson(joeBloggsPsaId, joeBloggs.individualDetails.value.name)) map {
      response =>
        verify(invitationsCacheRepository, times(1)).upsert(any())(using any())
        response.value.shouldEqual(())
    }
  }

  it should "return MongoDBFailedException if insertion failed with RunTimeException from mongodb" in {
    when(invitationsCacheRepository.upsert(any())(using any())).thenReturn(Future.failed(new RuntimeException("failed to perform DB operation")))
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    invitationService.invitePSA(invitationJson(joeBloggsPsaId, joeBloggs.individualDetails.value.name)) map {
      response =>
        verify(invitationsCacheRepository, times(1)).upsert(any())(using any())
        response.left.value.shouldBe(a[MongoDBFailedException])
        response.left.value.message.should(include("failed to perform DB operation"))
    }
  }

  it should "send an email to the invitee" in {
    val invitationService: InvitationService = app.injector.instanceOf[InvitationService]
    val encryptedPsaId = crypto.QueryParameterCrypto.encrypt(PlainText(johnDoePsaId.value)).value

    val expectedEmail =
      SendEmailRequest(
        List(johnDoeEmail),
        "pods_psa_invited",
        Map(
          "inviteeName" -> johnDoe.individualDetails.value.fullName,
          "schemeName" -> testSchemeName,
          "expiryDate" -> DateHelper.formatDate(expiryDate)
        ),
        force = false,
        Some(config.invitationCallbackUrl.format(JourneyType.INVITE, encryptedPsaId))
      )

    invitationService.invitePSA(invitationJson(johnDoePsaId, johnDoe.individualDetails.value.name)).map {
      _ =>
        emailConnector.sentEmails.should(containEmail(expectedEmail))
    }
  }

}

object InvitationServiceImplSpec extends MockitoSugar {

  val invitationsCacheRepository: InvitationsCacheRepository = mock[InvitationsCacheRepository]
  val emailConnector: FakeEmailConnector = new FakeEmailConnector()
  val fakeAuditService: StubSuccessfulAuditService = new StubSuccessfulAuditService()

  private val app = new GuiceApplicationBuilder()
    .overrides(
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
      bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
      bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
      bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
      bind[InvitationsCacheRepository].toInstance(invitationsCacheRepository),
      bind[SchemeConnector].toInstance(new FakeSchemeConnector()),
      bind[EmailConnector].toInstance(emailConnector),
      bind[AssociationConnector].toInstance(new FakeAssociationConnector()),
      bind[AuditService].toInstance(fakeAuditService)
    )
    .build()

  val crypto: ApplicationCrypto = app.injector.instanceOf[ApplicationCrypto]
  val config: AppConfig = app.injector.instanceOf[AppConfig]

  val testSchemeName = "test-scheme"
  val inviterPsaId: PsaId = PsaId("A7654321")

  val date: LocalDate = LocalDate.parse("2018-10-10")
  val expiryDate: Instant = Instant.now(Clock.fixed(Instant.parse("2018-10-10T00:00:00Z"), ZoneOffset.UTC))

  def invitation(inviteePsaId: PsaId, inviteeName: String): Invitation =
    Invitation(testSrn, "test-pstr", testSchemeName, inviterPsaId, inviteePsaId, inviteeName, expiryDate)



  case class TestInvitation(srn: SchemeReferenceNumber,
                             pstr: String,
                             schemeName: String,
                             inviterPsaId: PsaId,
                             inviteePsaId: PsaId,
                             inviteeName: String,
                             expireAt: Instant)

  object TestInvitation {
    implicit val dateFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
    implicit val writes: OWrites[TestInvitation] = Json.writes[TestInvitation]
  }

  def invitationJson(inviteePsaId: PsaId, inviteeName: String): JsValue =
    Json.toJson(TestInvitation(testSrn, "test-pstr", testSchemeName, inviterPsaId, inviteePsaId, inviteeName, expiryDate))

  val testSrn: SchemeReferenceNumber = SchemeReferenceNumber("S0987654321")

  val johnDoePsaId: PsaId = PsaId("A2000001")
  val johnDoeEmail: String = "john.doe@email.com"
  val johnDoe: MinimalDetails = MinimalDetails(johnDoeEmail, isPsaSuspended = false, organisationName = None,
    individualDetails = Some(IndividualDetails("John", None, "Doe")), rlsFlag = true, deceasedFlag = true)

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

}

class FakeAssociationConnector extends AssociationConnector {

  import InvitationServiceImplSpec.*

  def getMinimalDetails(idValue: String, idType: String, regime: String)
                       (implicit
                        @unused headerCarrier: HeaderCarrier,
                        @unused ec: ExecutionContext,
                        @unused request: RequestHeader): Future[Either[HttpException, MinimalDetails]] = {

    PsaId(idValue) match {
      case `johnDoePsaId` | `associatedPsaId` => Future.successful(Right(johnDoe))
      case `joeBloggsPsaId` | `exceptionResponsePsaId` => Future.successful(Right(joeBloggs))
      case `acmeLtdPsaId` | `invalidResponsePsaId` => Future.successful(Right(acmeLtd))
      case `blankPsaId` => Future.successful(Right(blank))
      case `notFoundPsaId` => Future.successful(Left(new NotFoundException("NOT_FOUND")))
      case unknownPsaId => throw new IllegalArgumentException(s"FakeAssociationConnector cannot handle PSA Id $unknownPsaId")
    }

  }


  def findMinimalDetailsByID(idValue: String,
                             idType: String,
                             regime: String)(implicit
                                              headerCarrier: HeaderCarrier,
                                              ec: ExecutionContext,
                                              request: RequestHeader): Future[Either[HttpException, Option[MinimalDetails]]] =
    getMinimalDetails(idValue, idType, regime).map(_.map(Option(_)))


  override def acceptInvitation(invitation: AcceptedInvitation)
                               (implicit headerCarrier: HeaderCarrier,
                                ec: ExecutionContext,
                                requestHeader: RequestHeader): Future[Either[HttpException, Unit]] = {
    throw new NotImplementedError()
  }

}

class FakeSchemeConnector extends SchemeConnector {

  import InvitationServiceImplSpec.*

  override def checkForAssociation(psaIdOrPspId: Either[PsaId, PspId], srn: SchemeReferenceNumber)
                                  (implicit @unused hc: HeaderCarrier,
                                            @unused ec: ExecutionContext): Future[Either[HttpException, Boolean]] = {
    val psaId = psaIdOrPspId.swap.getOrElse(throw new RuntimeException("no psa id"))
    Future.successful {
      if (psaId equals invalidResponsePsaId) {
        Left(new InternalServerException("Response from pension-scheme cannot be parsed to boolean"))
      } else if (psaId equals exceptionResponsePsaId) {
        Left(new NotFoundException("Cannot find this endpoint"))
      } else {
        Right(psaId equals associatedPsaId)
      }
    }
  }

  override def listOfSchemes(implicit headerCarrier: HeaderCarrier,
                                            ec: ExecutionContext): Future[Either[HttpException, JsValue]] = ???

  override def getSchemeDetails(schemeIdType: String, idNumber: String, srn: SchemeReferenceNumber)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpException, JsValue]] = ???
}
