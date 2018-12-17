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

import akka.stream.Materializer
import base.SpecBase
import connectors.RegistrationConnector
import models._
import models.registrationnoid.{OrganisationRegistrant, RegisterWithoutIdResponse, RegistrationNoIdIndividualRequest}
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{BadRequestException, _}
import utils.FakeAuthConnector
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with GeneratorDrivenPropertyChecks {

  import RegistrationControllerSpec._

  private val dataFromFrontend = readJsonFromFile("/data/validRegistrationNoIDOrganisationFE.json")
  private val dataToEmtp = readJsonFromFile("/data/validRegistrationNoIDOrganisationToEMTP.json").as[OrganisationRegistrant]

  private val individualNoIdFrontend = readJsonFromFile("/data/validRegNoIdIndividualFE.json")
  private val individualNoIdToConnector = individualNoIdFrontend.as[RegistrationNoIdIndividualRequest]
  private val individualNoIdToEmtp = readJsonFromFile("/data/validRegNoIdIndividualToEtmp.json")

  private val mockRegistrationConnector = mock[RegistrationConnector]

  implicit val mat: Materializer = app.materializer

  private def registrationController(retrievals: Future[_]): RegistrationController =
    new RegistrationController(
      new FakeAuthConnector(retrievals),
      mockRegistrationConnector
    )

  before(reset(mockRegistrationConnector))

  "registerWithIdIndividual" must {

    val inputRequestData = Json.obj("regime" -> "PODA", "requiresNameMatch" -> false, "isAnAgent" -> false)

    "return OK when the registration with id is successful for Individual" in {

      val successResponse = Json.toJson(readJsonFromFile("/data/validRegisterWithIdIndividualResponse.json").as[SuccessResponse])

      when(mockRegistrationConnector.registerWithIdIndividual(Matchers.eq(nino), any(), Matchers.eq(inputRequestData))(any(), any(), any()))
        .thenReturn(Future.successful(Right(successResponse)))

      val result = registrationController(individualRetrievals).registerWithIdIndividual(fakeRequest.withJsonBody(inputRequestData))

      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual successResponse
      }
    }

    "return result from registration when connector returns failure" in {

      val connectorFailureGen: Gen[HttpException] = Gen.oneOf(Seq(
        new BadRequestException("INVALID_PAYLOAD"),
        new NotFoundException("NOT FOUND"),
        new ConflictException("CONFLICT")
      ))

      forAll(connectorFailureGen) { connectorFailure =>

        when(mockRegistrationConnector.registerWithIdIndividual(Matchers.eq(nino), any(), Matchers.eq(inputRequestData))(any(), any(), any()))
          .thenReturn(Future.successful(Left(connectorFailure)))

        val result = registrationController(individualRetrievals).registerWithIdIndividual(fakeRequest.withJsonBody(inputRequestData))

        ScalaFutures.whenReady(result) { _ =>
          status(result) mustBe connectorFailure.responseCode
        }

      }

    }

    "throw Exception when authorisation retrievals fails" in {

      val retrievals = InsufficientConfidenceLevel()

      val result = registrationController(Future.failed(retrievals)).registerWithIdIndividual(fakeRequest.withJsonBody(inputRequestData))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe retrievals.msg
      }
    }

    "throw Upstream4xxResponse when auth all retrievals are not present" in {

      val retrievalsGen = Gen.oneOf(Seq(
        new ~(new ~(None, None), None),
        new ~(new ~(None, None), Some(AffinityGroup.Individual)),
        new ~(new ~(None, Some(externalId)), Some(AffinityGroup.Individual)),
        new ~(new ~(None, Some(externalId)), None),
        new ~(new ~(Some(nino), None), Some(AffinityGroup.Individual)),
        new ~(new ~(Some(nino), None), None),
        new ~(new ~(Some(nino), Some(externalId)), None)
      ))

      forAll(retrievalsGen) { retrievals =>

        val result = registrationController(Future.successful(retrievals)).registerWithIdIndividual(fakeRequest.withJsonBody(inputRequestData))

        ScalaFutures.whenReady(result.failed) { e =>
          e mustBe a[Upstream4xxResponse]
          e.getMessage mustBe "Nino not found in auth record"
        }

      }

    }

    "throw Upstream5xxResponse when given Upstream5xxResponse from connector" in {

      val failureResponse = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "DES is currently experiencing problems that require live service intervention."
      )

      when(mockRegistrationConnector.registerWithIdIndividual(Matchers.eq(nino), any(), Matchers.eq(inputRequestData))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse(failureResponse.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = registrationController(individualRetrievals).registerWithIdIndividual(fakeRequest.withJsonBody(inputRequestData))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream5xxResponse]
        e.getMessage mustBe failureResponse.toString()

        verify(mockRegistrationConnector, times(1))
          .registerWithIdIndividual(Matchers.eq(nino), any(), Matchers.eq(inputRequestData))(any(), any(), any())
      }
    }

    "throw Exception when any other exception returned from connector" in {

      when(mockRegistrationConnector.registerWithIdIndividual(Matchers.eq(nino), any(), Matchers.eq(inputRequestData))(any(), any(), any()))
        .thenReturn(Future.failed(new Exception("Generic Exception")))

      val result = registrationController(individualRetrievals).registerWithIdIndividual(fakeRequest.withJsonBody(inputRequestData))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"

        verify(mockRegistrationConnector, times(1))
          .registerWithIdIndividual(Matchers.eq(nino), any(), Matchers.eq(inputRequestData))(any(), any(), any())
      }
    }
  }

  "registerWithIdOrganisation" must {

    val inputData = Json.obj("utr" -> "1100000000", "organisationName" -> "Test Ltd", "organisationType" -> "LLP")

    "return OK when request utr and organisation get successful response from connector" in {

      val input = readJsonFromFile("/data/validRegisterWithIdOrganisationResponse.json")
      val successResponse = Json.toJson(readJsonFromFile("/data/validRegisterWithIdOrganisationResponse.json").as[SuccessResponse])

      when(mockRegistrationConnector.registerWithIdOrganisation(Matchers.eq("1100000000"), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Right(input)))

      val result = registrationController(organisationRetrievals).registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))

      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual successResponse
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

          val result = registrationController(organisationRetrievals).registerWithIdOrganisation(fakeRequest.withJsonBody(badRequest))

          ScalaFutures.whenReady(result.failed) { e =>
            e mustBe a[BadRequestException]
            e.getMessage must startWith("Bad Request returned from frontend for Register With Id Organisation")
          }

        }

      }

      "there is no body in the request" in {
        val result = registrationController(organisationRetrievals).registerWithIdOrganisation(fakeRequest)

        ScalaFutures.whenReady(result.failed) { e =>
          e mustBe a[BadRequestException]
          e.getMessage mustEqual "No request body received for Organisation"
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

        when(mockRegistrationConnector.registerWithIdOrganisation(Matchers.eq("1100000000"), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(Left(connectorFailure)))

        val result = registrationController(organisationRetrievals).registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))

        ScalaFutures.whenReady(result) { _ =>
          status(result) mustBe connectorFailure.responseCode
        }

      }

    }

    "throw Exception when authorisation retrievals fails" in {

      val retrievals = InsufficientConfidenceLevel()

      val result = registrationController(Future.failed(retrievals)).registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe retrievals.msg
      }
    }

    "throw Upstream4xxResponse when auth all retrievals are not present" in {

      val retrievalsGen = Gen.oneOf(Seq(
        new ~(None, None),
        new ~(None, Some(AffinityGroup.Organisation)),
        new ~(Some(""), None)
      ))

      forAll(retrievalsGen) { retrievals =>

        val result = registrationController(Future.successful(retrievals)).registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))

        ScalaFutures.whenReady(result.failed) { e =>
          e mustBe a[Upstream4xxResponse]
          e.getMessage mustBe "Not authorized"
        }

      }

    }

    "throw Upstream5xxResponse when given Upstream5xxResponse from connector" in {

      val failureResponse = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "DES is currently experiencing problems that require live service intervention."
      )

      when(mockRegistrationConnector.registerWithIdOrganisation(Matchers.eq("1100000000"), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse(failureResponse.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = registrationController(organisationRetrievals).registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream5xxResponse]
        e.getMessage mustBe failureResponse.toString()

        verify(mockRegistrationConnector, times(1))
          .registerWithIdOrganisation(Matchers.eq("1100000000"), any(), any())(any(), any(), any())
      }
    }

  }

  "registrationNoIdOrganisation" must {

    def fakeRequest(data: JsValue): FakeRequest[JsValue] = FakeRequest("POST", "/").withBody(data)

    "return OK with successful response from connector" in {

      val successResponse: JsObject = Json.obj(
        "processingDate" -> LocalDate.now,
        "sapNumber" -> "1234567890",
        "safeId" -> "XE0001234567890"
      )

      when(mockRegistrationConnector.registrationNoIdOrganisation(any(), Matchers.eq(dataToEmtp))(any(), any(), any()))
        .thenReturn(Future.successful(Right(successResponse)))

      val result = call(registrationController(organisationRetrievals).registrationNoIdOrganisation, fakeRequest(dataFromFrontend))

      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        verify(mockRegistrationConnector, times(1)).registrationNoIdOrganisation(any(), Matchers.eq(dataToEmtp))(any(), any(), any())
      }
    }

    "return result from registration when connector returns failure" in {

      val connectorFailureGen: Gen[HttpException] = Gen.oneOf(Seq(
        new BadRequestException("INVALID_PAYLOAD"),
        new NotFoundException("NOT FOUND"),
        new ConflictException("CONFLICT")
      ))

      forAll(connectorFailureGen) { connectorFailure =>

        when(mockRegistrationConnector.registrationNoIdOrganisation(any(), Matchers.eq(dataToEmtp))(any(), any(), any()))
          .thenReturn(Future.successful(Left(connectorFailure)))

        val result = call(registrationController(organisationRetrievals).registrationNoIdOrganisation, fakeRequest(dataFromFrontend))

        ScalaFutures.whenReady(result) { _ =>
          status(result) mustBe connectorFailure.responseCode
        }

      }

    }

    "throw Exception when authorisation retrievals fails" in {

      val retrievals = InsufficientConfidenceLevel()

      val result = call(registrationController(Future.failed(retrievals)).registrationNoIdOrganisation, fakeRequest(dataFromFrontend))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe retrievals.msg
      }
    }

    "throw Upstream4xxResponse when auth all retrievals are not present" in {

      val retrievalsGen = Gen.oneOf(Seq(
        new ~(None, None),
        new ~(None, Some(AffinityGroup.Organisation)),
        new ~(Some(""), None)
      ))

      forAll(retrievalsGen) { retrievals =>

        val result = call(registrationController(Future.successful(retrievals)).registrationNoIdOrganisation, fakeRequest(dataFromFrontend))

        ScalaFutures.whenReady(result.failed) { e =>
          e mustBe a[Upstream4xxResponse]
          e.getMessage mustBe "Not authorized"
        }

      }

    }

    "throw Upstream5xxResponse when given Upstream5xxResponse from connector" in {

      val failureResponse = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "DES is currently experiencing problems that require live service intervention."
      )

      when(mockRegistrationConnector.registrationNoIdOrganisation(any(), Matchers.eq(dataToEmtp))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse(failureResponse.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = call(registrationController(organisationRetrievals).registrationNoIdOrganisation, fakeRequest(dataFromFrontend))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream5xxResponse]
        e.getMessage mustBe failureResponse.toString()

        verify(mockRegistrationConnector, times(1))
          .registrationNoIdOrganisation(any(), Matchers.eq(dataToEmtp))(any(), any(), any())
      }
    }
  }

  "registrationNoIdIndividual" must {

    def fakeRequest(data: JsValue): FakeRequest[JsValue] = FakeRequest("POST", "/").withBody(data)

    "return OK with successful response from connector" in {
      val successResponse: RegisterWithoutIdResponse = RegisterWithoutIdResponse("XE0001234567890", "1234567890")

      when(mockRegistrationConnector.registrationNoIdIndividual(any(), Matchers.eq(individualNoIdToConnector))(any(), any(), any()))
        .thenReturn(Future.successful(Right(successResponse)))

      val result = call(registrationController(individualNoIdRetrievals).registrationNoIdIndividual, fakeRequest(individualNoIdFrontend))

      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        verify(mockRegistrationConnector, times(1)).registrationNoIdIndividual(any(), Matchers.eq(individualNoIdToConnector))(any(), any(), any())
      }
    }

    "return result from registration when connector returns failure" in {

      val connectorFailureGen: Gen[HttpException] = Gen.oneOf(Seq(
        new BadRequestException("INVALID_PAYLOAD"),
        new NotFoundException("NOT FOUND"),
        new ConflictException("CONFLICT")
      ))

      forAll(connectorFailureGen) { connectorFailure =>

        when(mockRegistrationConnector.registrationNoIdIndividual(any(), Matchers.eq(individualNoIdToConnector))(any(), any(), any()))
          .thenReturn(Future.successful(Left(connectorFailure)))

        val result = call(registrationController(individualNoIdRetrievals).registrationNoIdIndividual, fakeRequest(individualNoIdFrontend))

        ScalaFutures.whenReady(result) { _ =>
          status(result) mustBe connectorFailure.responseCode
        }

      }

    }

    "throw Upstream4xxResponse when auth all retrievals are not present" in {

      val retrievals = new ~(None, Some(AffinityGroup.Organisation))

      val result = call(registrationController(Future.successful(retrievals)).registrationNoIdIndividual, fakeRequest(individualNoIdFrontend))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream4xxResponse]
        e.getMessage mustBe "Not authorized"
      }
    }


    "throw Upstream5xxResponse when given Upstream5xxResponse from connector" in {

      val failureResponse = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "DES is currently experiencing problems that require live service intervention."
      )

      when(mockRegistrationConnector.registrationNoIdIndividual(any(), Matchers.eq(individualNoIdToConnector))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse(failureResponse.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = call(registrationController(individualNoIdRetrievals).registrationNoIdIndividual, fakeRequest(individualNoIdFrontend))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream5xxResponse]
        e.getMessage mustBe failureResponse.toString()

        verify(mockRegistrationConnector, times(1))
          .registrationNoIdIndividual(any(), Matchers.eq(individualNoIdToConnector))(any(), any(), any())
      }
    }
  }



}

object RegistrationControllerSpec {

  private val nino = "test-nino"
  private val externalId = "test-external-id"
  private val fakeRequest = FakeRequest("POST", "/")

  private val individualRetrievals =
    Future.successful(
      new ~(
        new ~(
          Some(nino),
          Some(externalId)
        ),
        Some(AffinityGroup.Individual)
      )
    )

  private val organisationRetrievals =
    Future.successful(
      new ~(
        Some(externalId),
        Some(AffinityGroup.Organisation)
      )
    )

  private val individualNoIdRetrievals =
    Future.successful(
      new ~(
        Some(externalId),
        Some(AffinityGroup.Individual)
      )
    )

}
