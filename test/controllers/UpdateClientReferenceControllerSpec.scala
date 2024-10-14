/*
 * Copyright 2024 HM Revenue & Customs
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

import connectors.UpdateClientReferenceConnector
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{BadRequestException, _}
import utils.FakeAuthConnector

import java.time.LocalDateTime
import scala.concurrent.Future

class UpdateClientReferenceControllerSpec extends AnyWordSpec with MockitoSugar with BeforeAndAfter
  with ScalaCheckDrivenPropertyChecks with Matchers {

  private val externalId = "test-external-id"
  private val individualRetrievals =
    Future.successful(
      new ~(
        Some(externalId),
        Some(AffinityGroup.Individual)
      )
    )

  private val mockUpdateClientReferenceConnector = mock[UpdateClientReferenceConnector]

  def modules: Seq[GuiceableModule] = Seq(
    bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
    bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
    bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
    bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
    bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository]),
    bind[AdminDataRepository].toInstance(mock[AdminDataRepository]),
    bind[UpdateClientReferenceConnector].toInstance(mockUpdateClientReferenceConnector)
  )

  def updateClientReferenceController(app: Application): UpdateClientReferenceController = app.injector.instanceOf[UpdateClientReferenceController]

  before(reset(mockUpdateClientReferenceConnector))

  "updateClientReference " must {

    val requestBody = Json.obj("pstr" -> "pstr", "psaId" -> "psaId", "pspId" -> "pspId", "clientReference" -> "clientReference")

    "return OK for successful" in {
      val bindings: Seq[GuiceableModule] = modules :+ bind[AuthConnector].toInstance(new FakeAuthConnector(individualRetrievals))
      running(_.overrides(
        bindings: _*
      )) { app =>

        val successResponse: JsValue = Json.obj(
          "status" -> "OK",
          "statusText" -> "Hello there!",
          "processingDate" -> LocalDateTime.now().toString()
        )

        when(mockUpdateClientReferenceConnector.updateClientReference(any(), any())
        (any(), any(), any()))
          .thenReturn(Future.successful(Right(successResponse)))

        val result = updateClientReferenceController(app).updateClientReference(fakeRequest.withJsonBody(requestBody))

        ScalaFutures.whenReady(result) { _ =>
          status(result) mustBe OK
          contentAsJson(result) mustEqual Json.toJson(successResponse)
        }
      }
    }

    "throw BadRequestException" when {
      "nino cannot be read from request" in {
        val bindings: Seq[GuiceableModule] = modules :+ bind[AuthConnector].toInstance(new FakeAuthConnector(individualRetrievals))
        running(_.overrides(
          bindings: _*
        )) { app =>

          val badRequestGen: Gen[JsObject] = Gen.oneOf(Seq(
            Json.obj(),
            Json.obj("bad" -> "request")
          ))

          forAll(badRequestGen) { badRequest =>
            val result = updateClientReferenceController(app).updateClientReference(fakeRequest.withJsonBody(badRequest))

            ScalaFutures.whenReady(result.failed) { e =>
              e mustBe a[BadRequestException]
              e.getMessage must startWith("Invalid request received from frontend for update Client Reference")
            }
          }
        }
      }

      "there is no body in the request" in {
        val bindings: Seq[GuiceableModule] = modules :+ bind[AuthConnector].toInstance(new FakeAuthConnector(individualRetrievals))
        running(_.overrides(
          bindings: _*
        )) { app =>
          val result = updateClientReferenceController(app).updateClientReference(fakeRequest)

          ScalaFutures.whenReady(result.failed) { e =>
            e mustBe a[BadRequestException]
            e.getMessage mustEqual "No request body received for update Client Reference"
          }
        }
      }
    }

    "return result from registration when connector returns failure" in {
      val bindings: Seq[GuiceableModule] = modules :+ bind[AuthConnector].toInstance(new FakeAuthConnector(individualRetrievals))
      running(_.overrides(
        bindings: _*
      )) { app =>
        val connectorFailureGen: Gen[HttpException] = Gen.oneOf(Seq(
          new BadRequestException("INVALID_PAYLOAD"),
          new NotFoundException("NOT FOUND"),
          new ConflictException("CONFLICT")
        ))

        forAll(connectorFailureGen) { connectorFailure =>

          when(mockUpdateClientReferenceConnector.updateClientReference(any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(Left(connectorFailure)))

          val result = updateClientReferenceController(app).updateClientReference(fakeRequest.withJsonBody(requestBody))

          ScalaFutures.whenReady(result) { _ =>
            status(result) mustBe connectorFailure.responseCode
          }
        }
      }
    }

    "throw Exception when authorisation retrievals fails" in {
      val retrievals = InsufficientConfidenceLevel()
      val bindings: Seq[GuiceableModule] = modules :+ bind[AuthConnector].toInstance(new FakeAuthConnector(Future.failed(retrievals)))
      running(_.overrides(
        bindings: _*
      )) { app =>

        val result = updateClientReferenceController(app).updateClientReference(fakeRequest.withJsonBody(requestBody))

        ScalaFutures.whenReady(result.failed) { e =>
          e mustBe a[Exception]
          e.getMessage mustBe retrievals.msg
        }
      }
    }

    "throw UpstreamErrorResponse when auth all retrievals are not present" in {

      val retrievalsGen = Gen.oneOf(Seq(
        new ~(None, None),
        new ~(None, Some(AffinityGroup.Individual)),
        new ~(Some(""), None)
      ))

      forAll(retrievalsGen) { retrievals =>
        val bindings: Seq[GuiceableModule] = modules :+ bind[AuthConnector].toInstance(new FakeAuthConnector(Future.successful(retrievals)))
        running(_.overrides(
          bindings: _*
        )) { app =>
          val result = updateClientReferenceController(app).updateClientReference(fakeRequest.withJsonBody(requestBody))

          ScalaFutures.whenReady(result.failed) { e =>
            e mustBe a[UpstreamErrorResponse]
            e.getMessage mustBe "Not authorized"
          }
        }
      }
    }

    "throw UpstreamErrorResponse when given UpstreamErrorResponse from connector" in {

      val bindings: Seq[GuiceableModule] = modules :+ bind[AuthConnector].toInstance(new FakeAuthConnector(individualRetrievals))
      running(_.overrides(
        bindings: _*
      )) { app =>

        val failureResponse = Json.obj(
          "code" -> "SERVER_ERROR",
          "reason" -> "DES is currently experiencing problems that require live service intervention."
        )

        when(mockUpdateClientReferenceConnector.updateClientReference(any(), any())(any(), any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse(failureResponse.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))
        val result = updateClientReferenceController(app).updateClientReference(fakeRequest.withJsonBody(requestBody))

        ScalaFutures.whenReady(result.failed) { e =>
          e mustBe a[UpstreamErrorResponse]
          e.getMessage mustBe failureResponse.toString()

          verify(mockUpdateClientReferenceConnector, times(1))
            .updateClientReference(any(), any())(any(), any(), any())
        }
      }
    }

    "throw Exception when any other exception returned from connector" in {
      val bindings: Seq[GuiceableModule] = modules :+ bind[AuthConnector].toInstance(new FakeAuthConnector(individualRetrievals))
      running(_.overrides(
        bindings: _*
      )) { app =>
        when(mockUpdateClientReferenceConnector.updateClientReference(any(), any())(any(), any(), any()))
          .thenReturn(Future.failed(new Exception("Generic Exception")))
        val result = updateClientReferenceController(app).updateClientReference(fakeRequest.withJsonBody(requestBody))

        ScalaFutures.whenReady(result.failed) { e =>
          e mustBe a[Exception]
          e.getMessage mustBe "Generic Exception"

          verify(mockUpdateClientReferenceConnector, times(1))
            .updateClientReference(any(), any())(any(), any(), any())
        }
      }
    }
  }

  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")
}


