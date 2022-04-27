/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.UpdateClientReferenceConnector
import org.joda.time.LocalDateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json._
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{BadRequestException, _}
import utils.FakeAuthConnector

import scala.concurrent.Future

class UpdateClientReferenceControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with ScalaCheckDrivenPropertyChecks {

  private val externalId = "test-external-id"
  private val individualRetrievals =
    Future.successful(
      new ~(
        Some(externalId),
        Some(AffinityGroup.Individual)
      )
    )

  private val mockUpdateClientReferenceConnector = mock[UpdateClientReferenceConnector]

  implicit val mat: Materializer = app.materializer

  private def updateClientReferenceController(retrievals: Future[_]): UpdateClientReferenceController =
    new UpdateClientReferenceController(
      new FakeAuthConnector(retrievals),
      mockUpdateClientReferenceConnector,
      controllerComponents
    )

  before(reset(mockUpdateClientReferenceConnector))

  "updateClientReference " must {

    val requestBody = Json.obj("pstr" -> "pstr", "psaId" -> "psaId", "pspId" -> "pspId", "clientReference" -> "clientReference")

    "return OK for successful" in {

      val successResponse: JsValue = Json.obj(
        "status" -> "OK",
        "statusText" -> "Hello there!",
        "processingDate" -> LocalDateTime.now().toString()
      )

      when(mockUpdateClientReferenceConnector.updateClientReference(any())
      (any(), any(), any()))
        .thenReturn(Future.successful(Right(successResponse)))

      val result = updateClientReferenceController(individualRetrievals).updateClientReference(fakeRequest.withJsonBody(requestBody))

      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual Json.toJson(successResponse)
      }
    }

    "throw BadRequestException" when {
      "nino cannot be read from request" in {

        val badRequestGen: Gen[JsObject] = Gen.oneOf(Seq(
          Json.obj(),
          Json.obj("bad" -> "request")
        ))

        forAll(badRequestGen) { badRequest =>

          val result = updateClientReferenceController(individualRetrievals).updateClientReference(fakeRequest.withJsonBody(badRequest))

          ScalaFutures.whenReady(result.failed) { e =>
            e mustBe a[BadRequestException]
            e.getMessage must startWith("Invalid request received from frontend for update Client Reference")
          }

        }

      }

      "there is no body in the request" in {
        val result = updateClientReferenceController(individualRetrievals).updateClientReference(fakeRequest)

        ScalaFutures.whenReady(result.failed) { e =>
          e mustBe a[BadRequestException]
          e.getMessage mustEqual "No request body received for update Client Reference"
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

        when(mockUpdateClientReferenceConnector.updateClientReference(any())(any(), any(), any()))
          .thenReturn(Future.successful(Left(connectorFailure)))

        val result = updateClientReferenceController(individualRetrievals).updateClientReference(fakeRequest.withJsonBody(requestBody))

        ScalaFutures.whenReady(result) { _ =>
          status(result) mustBe connectorFailure.responseCode
        }

      }

    }

    "throw Exception when authorisation retrievals fails" in {

      val retrievals = InsufficientConfidenceLevel()

      val result = updateClientReferenceController(Future.failed(retrievals)).updateClientReference(fakeRequest.withJsonBody(requestBody))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe retrievals.msg
      }
    }

    "throw UpstreamErrorResponse when auth all retrievals are not present" in {

      val retrievalsGen = Gen.oneOf(Seq(
        new ~(None, None),
        new ~(None, Some(AffinityGroup.Individual)),
        new ~(Some(""), None)
      ))

      forAll(retrievalsGen) { retrievals =>

        val result = updateClientReferenceController(Future.successful(retrievals)).updateClientReference(fakeRequest.withJsonBody(requestBody))

        ScalaFutures.whenReady(result.failed) { e =>
          e mustBe a[UpstreamErrorResponse]
          e.getMessage mustBe "Not authorized"
        }

      }

    }

    "throw UpstreamErrorResponse when given UpstreamErrorResponse from connector" in {

      val failureResponse = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "DES is currently experiencing problems that require live service intervention."
      )

      when(mockUpdateClientReferenceConnector.updateClientReference(any())(any(), any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(failureResponse.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = updateClientReferenceController(individualRetrievals).updateClientReference(fakeRequest.withJsonBody(requestBody))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe failureResponse.toString()

        verify(mockUpdateClientReferenceConnector, times(1))
          .updateClientReference(any())(any(), any(), any())
      }
    }

    "throw Exception when any other exception returned from connector" in {

      when(mockUpdateClientReferenceConnector.updateClientReference(any())(any(), any(), any()))
        .thenReturn(Future.failed(new Exception("Generic Exception")))

      val result = updateClientReferenceController(individualRetrievals).updateClientReference(fakeRequest.withJsonBody(requestBody))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"

        verify(mockUpdateClientReferenceConnector, times(1))
          .updateClientReference(any())(any(), any(), any())
      }
    }
  }
}


