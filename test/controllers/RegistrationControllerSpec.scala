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

package controllers

import base.SpecBase
import connectors.RegistrationConnector
import models.registrationnoid.{OrganisationRegistrant, RegistrationNoIdIndividualRequest}
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.*
import play.api.mvc.BodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.{BadRequestException, *}
import utils.FakeAuthConnector

import java.time.LocalDate
import scala.concurrent.Future

class RegistrationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with ScalaCheckDrivenPropertyChecks {

  import RegistrationControllerSpec.*

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
      bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
      bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
      bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
      bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository])
    )

  private val dataFromFrontend = readJsonFromFile("/data/validRegistrationNoIDOrganisationFE.json")
  private val dataToEmtp = readJsonFromFile("/data/validRegistrationNoIDOrganisationToEMTP.json").as[OrganisationRegistrant]

  private val individualNoIdFrontend = readJsonFromFile("/data/validRegNoIdIndividualFE.json")
  private val individualNoIdToConnector = individualNoIdFrontend.as[RegistrationNoIdIndividualRequest]

  private val mockRegistrationConnector = mock[RegistrationConnector]

  implicit val mat: Materializer = fakeApplication().materializer

  private def registrationController(retrievals: Future[?]): RegistrationController =
    new RegistrationController(
      mockRegistrationConnector,
      controllerComponents,
      new actions.NoEnrolmentAuthAction(new FakeAuthConnector(retrievals), app.injector.instanceOf[BodyParsers.Default])
    )

  before(reset(mockRegistrationConnector))

  "registerWithIdIndividual " must {

    val mandatoryRequestData = Json.obj("regime" -> "PODA", "requiresNameMatch" -> false, "isAnAgent" -> false)
    val requestBody = Json.obj("nino" -> nino)

    "return OK when the registration with id is successful for Individual" in {

      val jsResponse = readJsonFromFile("/data/validRegisterWithIdIndividualResponse.json")

      when(mockRegistrationConnector.registerWithIdIndividual(eqTo(nino), any(), eqTo(mandatoryRequestData))
      (using any(), any(), any()))
        .thenReturn(Future.successful(Right(jsResponse)))

      val result = registrationController(idRetrievals).registerWithIdIndividual(fakeRequest.withJsonBody(requestBody))

      ScalaFutures.whenReady(result) { _ =>
        status(result).mustBe(OK)
        contentAsJson(result).mustBe(jsResponse)
      }
    }

    "throw BadRequestException" when {
      "nino cannot be read from request" in {

        val badRequestGen: Gen[JsObject] = Gen.oneOf(Seq(
          Json.obj(),
          Json.obj("bad" -> "request")
        ))

        forAll(badRequestGen) { badRequest =>

          val result = registrationController(idRetrievals).registerWithIdIndividual(fakeRequest.withJsonBody(badRequest))

          ScalaFutures.whenReady(result.failed) { e =>
            e.mustBe(a[BadRequestException])
            e.getMessage.must(startWith("Bad Request returned from frontend for Register With Id Individual"))
          }

        }

      }

      "there is no body in the request" in {
        val result = registrationController(idRetrievals).registerWithIdIndividual(fakeRequest)

        ScalaFutures.whenReady(result.failed) { e =>
          e.mustBe(a[BadRequestException])
          e.getMessage.mustEqual("No request body received for register with Id Individual")
        }
      }
    }

    "return result from registration when connector returns failure" in {

      val connectorFailureGen: Gen[HttpException] = Gen.oneOf(Seq(
        new BadRequestException("INVALID_PAYLOAD"),
        new NotFoundException("NOT FOUND"),
        new ConflictException("CONFLICT")
      ))

      forAll(connectorFailureGen) { connectorFailure =>

        when(mockRegistrationConnector.registerWithIdIndividual(eqTo(nino), any(), eqTo(mandatoryRequestData))(using any(), any(), any()))
          .thenReturn(Future.successful(Left(connectorFailure)))

        val result = registrationController(idRetrievals).registerWithIdIndividual(fakeRequest.withJsonBody(requestBody))

        ScalaFutures.whenReady(result) { _ =>
          status(result).mustBe(connectorFailure.responseCode)
        }

      }

    }

    "throw Exception when authorisation retrievals fails" in {

      val retrievals = InsufficientConfidenceLevel()

      val result = registrationController(Future.failed(retrievals)).registerWithIdIndividual(fakeRequest.withJsonBody(requestBody))

      ScalaFutures.whenReady(result.failed) { e =>
        e.mustBe(a[Exception])
        e.getMessage.mustBe(retrievals.msg)
      }
    }

    "throw UpstreamErrorResponse when auth all retrievals are not present" in {

      val retrievals = None

      val result = registrationController(Future.successful(retrievals)).registerWithIdIndividual(fakeRequest.withJsonBody(requestBody))

      ScalaFutures.whenReady(result.failed) { e =>
        e.mustBe(a[UpstreamErrorResponse])
        e.getMessage.mustBe("Not authorized")
      }


    }

    "throw UpstreamErrorResponse when given UpstreamErrorResponse from connector" in {

      val failureResponse = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "DES is currently experiencing problems that require live service intervention."
      )

      when(mockRegistrationConnector.registerWithIdIndividual(eqTo(nino), any(), eqTo(mandatoryRequestData))(using any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(failureResponse.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = registrationController(idRetrievals).registerWithIdIndividual(fakeRequest.withJsonBody(requestBody))

      ScalaFutures.whenReady(result.failed) { e =>
        e.mustBe(a[UpstreamErrorResponse])
        e.getMessage.mustBe(failureResponse.toString())

        verify(mockRegistrationConnector, times(1))
          .registerWithIdIndividual(eqTo(nino), any(), eqTo(mandatoryRequestData))(using any(), any(), any())
      }
    }

    "throw Exception when any other exception returned from connector" in {

      when(mockRegistrationConnector.registerWithIdIndividual(eqTo(nino), any(), eqTo(mandatoryRequestData))(using any(), any(), any()))
        .thenReturn(Future.failed(new Exception("Generic Exception")))

      val result = registrationController(idRetrievals).registerWithIdIndividual(fakeRequest.withJsonBody(requestBody))

      ScalaFutures.whenReady(result.failed) { e =>
        e.mustBe(a[Exception])
        e.getMessage.mustBe("Generic Exception")

        verify(mockRegistrationConnector, times(1))
          .registerWithIdIndividual(eqTo(nino), any(), eqTo(mandatoryRequestData))(using any(), any(), any())
      }
    }
  }

  "registerWithIdOrganisation" must {

    val inputData = Json.obj("utr" -> "1100000000", "organisationName" -> "Test Ltd", "organisationType" -> "LLP")

    "return OK when request utr and organisation get successful response from connector, stripping out any invalid characters from the name" in {
      val inputData = Json.obj(
        "utr" -> "1100000000k",
        "organisationName" -> """(Test) %"Ltd"^$£!""",
        "organisationType" -> "LLP"
      )

      val jsResponse = readJsonFromFile("/data/validRegisterWithIdOrganisationResponse.json")

      val expectedJsonForConnector = Json.obj(
        "regime" -> "PODA",
        "requiresNameMatch" -> true,
        "isAnAgent" -> false,
        "organisation" -> Json.obj(
          "organisationName" -> "(Test) Ltd",
          "organisationType" -> "LLP"
        )
      )

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsValue])

      when(mockRegistrationConnector
        .registerWithIdOrganisation(eqTo("1100000000"), any(), jsonCaptor.capture())(using any(), any(), any()))
        .thenReturn(Future.successful(Right(jsResponse)))

      val result = registrationController(idRetrievals).registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))

      ScalaFutures.whenReady(result) { _ =>
        status(result).mustBe(OK)
        contentAsJson(result).mustBe(jsResponse)
        jsonCaptor.getValue.mustBe(expectedJsonForConnector)
      }
    }

    "throw BadRequestException" when {
      "utr and organisation cannot be read from request" in {

        val badRequestGen: Gen[JsObject] = Gen.oneOf(Seq(
          Json.obj("organisationName" -> "Test Ltd", "organisationType" -> "LLP"),
          Json.obj("utr" -> "1100000000", "organisationName" -> "Test Ltd"),
          Json.obj("utr" -> "1100000000", "organisationType" -> "LLP"),
          Json.obj("utr" -> "1100000000"),
          Json.obj(),
          Json.obj("bad" -> "request")
        ))

        forAll(badRequestGen) { badRequest =>

          val result = registrationController(idRetrievals).registerWithIdOrganisation(fakeRequest.withJsonBody(badRequest))

          ScalaFutures.whenReady(result.failed) { e =>
            e.mustBe(a[BadRequestException])
            e.getMessage.must(startWith("Bad Request returned for Register With Id Organisation"))
          }
        }
      }

      "there is no body in the request" in {
        val result = registrationController(idRetrievals).registerWithIdOrganisation(fakeRequest)

        ScalaFutures.whenReady(result.failed) { e =>
          e.mustBe(a[BadRequestException])
          e.getMessage.mustBe("No request body received for Organisation")
        }
      }
    }

    "return result from registration when connector returns failure" in {

      val connectorFailureGen: Gen[HttpException] = Gen.oneOf(Seq(
        new BadRequestException("INVALID_PAYLOAD"),
        new NotFoundException("NOT FOUND"),
        new ConflictException("CONFLICT")
      ))

      forAll(connectorFailureGen) { connectorFailure =>

        when(mockRegistrationConnector.registerWithIdOrganisation(eqTo("1100000000"), any(), any())(using any(), any(), any()))
          .thenReturn(Future.successful(Left(connectorFailure)))

        val result = registrationController(idRetrievals).registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))

        ScalaFutures.whenReady(result) { _ =>
          status(result).mustBe(connectorFailure.responseCode)
        }
      }

    }

    "throw Exception when authorisation retrievals fails" in {

      val retrievals = InsufficientConfidenceLevel()

      val result = registrationController(Future.failed(retrievals)).registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))

      ScalaFutures.whenReady(result.failed) { e =>
        e.mustBe(a[Exception])
        e.getMessage.mustBe(retrievals.msg)
      }
    }

    "throw UpstreamErrorResponse when auth all retrievals are not present" in {

      val retrievals = None

      val result = registrationController(Future.successful(retrievals)).registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))

      ScalaFutures.whenReady(result.failed) { e =>
        e.mustBe(a[UpstreamErrorResponse])
        e.getMessage.mustBe("Not authorized")
      }
    }

    "throw UpstreamErrorResponse when given UpstreamErrorResponse from connector" in {

      val failureResponse = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "DES is currently experiencing problems that require live service intervention."
      )

      when(mockRegistrationConnector.registerWithIdOrganisation(eqTo("1100000000"), any(), any())(using any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(failureResponse.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = registrationController(idRetrievals).registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))

      ScalaFutures.whenReady(result.failed) { e =>
        e.mustBe(a[UpstreamErrorResponse])
        e.getMessage.mustBe(failureResponse.toString())

        verify(mockRegistrationConnector, times(1))
          .registerWithIdOrganisation(eqTo("1100000000"), any(), any())(using any(), any(), any())
      }
    }

  }

  "registrationNoIdOrganisation" must {

    def fakeRequest(data: JsValue): FakeRequest[JsValue] = FakeRequest("POST", "/").withBody(data)

    "return OK with successful response from connector" in {

      val jsResponse = Json.obj(
        "processingDate" -> LocalDate.now,
        "sapNumber" -> "1234567890",
        "safeId" -> "XE0001234567890"
      )

      when(mockRegistrationConnector.registrationNoIdOrganisation(any(), eqTo(dataToEmtp))(using any(), any(), any()))
        .thenReturn(Future.successful(Right(jsResponse)))

      val result = call(registrationController(idRetrievals).registrationNoIdOrganisation, fakeRequest(dataFromFrontend))

      ScalaFutures.whenReady(result) { _ =>
        status(result).mustBe(OK)
        verify(mockRegistrationConnector, times(1)).registrationNoIdOrganisation(any(), eqTo(dataToEmtp))(using any(), any(), any())
      }
    }

    "return result from registration when connector returns failure" in {

      val connectorFailureGen: Gen[HttpException] = Gen.oneOf(Seq(
        new BadRequestException("INVALID_PAYLOAD"),
        new NotFoundException("NOT FOUND"),
        new ConflictException("CONFLICT")
      ))

      forAll(connectorFailureGen) { connectorFailure =>

        when(mockRegistrationConnector.registrationNoIdOrganisation(any(), eqTo(dataToEmtp))(using any(), any(), any()))
          .thenReturn(Future.successful(Left(connectorFailure)))

        val result = call(registrationController(idRetrievals).registrationNoIdOrganisation, fakeRequest(dataFromFrontend))

        ScalaFutures.whenReady(result) { _ =>
          status(result).mustBe(connectorFailure.responseCode)
        }
      }
    }

    "throw Exception when authorisation retrievals fails" in {

      val retrievals = InsufficientConfidenceLevel()

      val result = call(registrationController(Future.failed(retrievals)).registrationNoIdOrganisation, fakeRequest(dataFromFrontend))

      ScalaFutures.whenReady(result.failed) { e =>
        e.mustBe(a[Exception])
        e.getMessage.mustBe(retrievals.msg)
      }
    }

    "throw UpstreamErrorResponse when auth all retrievals are not present" in {

      val retrievals = None

      val result = call(registrationController(Future.successful(retrievals)).registrationNoIdOrganisation, fakeRequest(dataFromFrontend))

      ScalaFutures.whenReady(result.failed) { e =>
        e.mustBe(a[UpstreamErrorResponse])
        e.getMessage.mustBe("Not authorized")
      }

    }

    "throw UpstreamErrorResponse when given UpstreamErrorResponse from connector" in {

      val failureResponse = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "DES is currently experiencing problems that require live service intervention."
      )

      when(mockRegistrationConnector.registrationNoIdOrganisation(any(), eqTo(dataToEmtp))(using any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(failureResponse.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = call(registrationController(idRetrievals).registrationNoIdOrganisation, fakeRequest(dataFromFrontend))

      ScalaFutures.whenReady(result.failed) { e =>
        e.mustBe(a[UpstreamErrorResponse])
        e.getMessage.mustBe(failureResponse.toString())

        verify(mockRegistrationConnector, times(1))
          .registrationNoIdOrganisation(any(), eqTo(dataToEmtp))(using any(), any(), any())
      }
    }
  }

  "registrationNoIdIndividual" must {

    def fakeRequest(data: JsValue): FakeRequest[JsValue] = FakeRequest("POST", "/").withBody(data)

    "return OK with successful response from connector" in {
      val jsResponse = Json.obj("safeId" -> " XE0001234567890", "sapNumber" -> "1234567890")

      when(mockRegistrationConnector.registrationNoIdIndividual(any(), eqTo(individualNoIdToConnector))(using any(), any(), any()))
        .thenReturn(Future.successful(Right(jsResponse)))

      val result = call(registrationController(idRetrievals).registrationNoIdIndividual, fakeRequest(individualNoIdFrontend))

      ScalaFutures.whenReady(result) { _ =>
        status(result).mustBe(OK)
        verify(mockRegistrationConnector, times(1)).registrationNoIdIndividual(any(), eqTo(individualNoIdToConnector))(using any(), any(), any())
      }
    }

    "return result from registration when connector returns failure" in {

      val connectorFailureGen: Gen[HttpException] = Gen.oneOf(Seq(
        new BadRequestException("INVALID_PAYLOAD"),
        new NotFoundException("NOT FOUND"),
        new ConflictException("CONFLICT")
      ))

      forAll(connectorFailureGen) { connectorFailure =>

        when(mockRegistrationConnector.registrationNoIdIndividual(any(), eqTo(individualNoIdToConnector))(using any(), any(), any()))
          .thenReturn(Future.successful(Left(connectorFailure)))

        val result = call(registrationController(idRetrievals).registrationNoIdIndividual, fakeRequest(individualNoIdFrontend))

        ScalaFutures.whenReady(result) { _ =>
          status(result).mustBe(connectorFailure.responseCode)
        }

      }

    }

    "throw UpstreamErrorResponse when auth all retrievals are not present" in {

      val retrievals = None

      val result = call(registrationController(Future.successful(retrievals)).registrationNoIdIndividual, fakeRequest(individualNoIdFrontend))

      ScalaFutures.whenReady(result.failed) { e =>
        e.mustBe(a[UpstreamErrorResponse])
        e.getMessage.mustBe("Not authorized")
      }
    }


    "throw UpstreamErrorResponse when given UpstreamErrorResponse from connector" in {

      val failureResponse = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "DES is currently experiencing problems that require live service intervention."
      )

      when(mockRegistrationConnector.registrationNoIdIndividual(any(), eqTo(individualNoIdToConnector))(using any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(failureResponse.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = call(registrationController(idRetrievals).registrationNoIdIndividual, fakeRequest(individualNoIdFrontend))

      ScalaFutures.whenReady(result.failed) { e =>
        e.mustBe(a[UpstreamErrorResponse])
        e.getMessage.mustBe(failureResponse.toString())

        verify(mockRegistrationConnector, times(1))
          .registrationNoIdIndividual(any(), eqTo(individualNoIdToConnector))(using any(), any(), any())
      }
    }
  }
}

object RegistrationControllerSpec {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  private val nino = "test-nino"
  private val externalId = "test-external-id"

  private val idRetrievals =
    Future.successful(Some(externalId))

}
