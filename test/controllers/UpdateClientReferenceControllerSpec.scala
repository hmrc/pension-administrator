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

import connectors.{SchemeConnector, UpdateClientReferenceConnector}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
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
import play.api.libs.json.*
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.domain.PspId
import uk.gov.hmrc.http.{BadRequestException, *}
import utils.AuthUtils.FakeFailingAuthConnector
import utils.{AuthUtils, FakeAuthConnector, FakePsaSchemeAuthAction}

import java.time.LocalDateTime
import scala.concurrent.Future

class UpdateClientReferenceControllerSpec extends AnyWordSpec
                                            with MockitoSugar
                                            with BeforeAndAfter
                                            with ScalaCheckDrivenPropertyChecks
                                            with Matchers {

  private val individualRetrievals = Future.successful(AuthUtils.authResponse)
  private val mockUpdateClientReferenceConnector = mock[UpdateClientReferenceConnector]
  private val mockSchemeConnector = mock[SchemeConnector]

  private def modules: Seq[GuiceableModule] = Seq(
    bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
    bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
    bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
    bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
    bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository]),
    bind[UpdateClientReferenceConnector].toInstance(mockUpdateClientReferenceConnector),
    bind[actions.PsaSchemeAuthAction].toInstance(new FakePsaSchemeAuthAction),
    bind[SchemeConnector].toInstance(mockSchemeConnector)
  )

  private def updateClientReferenceController(app: Application): UpdateClientReferenceController = app.injector.instanceOf[UpdateClientReferenceController]

  before {
    reset(mockUpdateClientReferenceConnector)
    when(mockSchemeConnector.checkForAssociation(ArgumentMatchers.eq(Right(PspId(AuthUtils.pspId))),
      ArgumentMatchers.eq(AuthUtils.srn))(using any(), any()))
      .thenReturn(Future.successful(Right(true)))
  }

  private val requestHeaders = Seq("pstr" -> "pstr", "pspId" -> AuthUtils.pspId, "clientReference" -> "clientReference")

  "updateClientReference" must {
    "return OK for successful" in {
      val bindings: Seq[GuiceableModule] = modules :+ bind[AuthConnector].toInstance(new FakeAuthConnector(individualRetrievals))
      running(_.overrides(
        bindings *
      )) { app =>

        val successResponse: JsValue = Json.obj(
          "status" -> "OK",
          "statusText" -> "Hello there!",
          "processingDate" -> LocalDateTime.now().toString()
        )

        when(mockUpdateClientReferenceConnector.updateClientReference(any(), any())(using any(), any(), any()))
          .thenReturn(Future.successful(Right(successResponse)))

        val result = updateClientReferenceController(app).updateClientReference(AuthUtils.srn)(fakeRequest.withHeaders(requestHeaders *))

        ScalaFutures.whenReady(result) { _ =>
          status(result).mustBe(OK)
          contentAsJson(result) mustEqual Json.toJson(successResponse)
        }
      }
    }

    "return Forbidden if psp is not associated with scheme" in {
      val bindings: Seq[GuiceableModule] = modules :+ bind[AuthConnector].toInstance(new FakeAuthConnector(individualRetrievals))
      running(_.overrides(
        bindings *
      )) { app =>
        when(mockSchemeConnector.checkForAssociation(ArgumentMatchers.eq(Right(PspId(AuthUtils.pspId))),
          ArgumentMatchers.eq(AuthUtils.srn))(using any(), any()))
          .thenReturn(Future.successful(Right(false)))
        val result = updateClientReferenceController(app).updateClientReference(AuthUtils.srn)(fakeRequest.withHeaders(requestHeaders *))

        status(result).mustBe(FORBIDDEN)
        contentAsString(result).mustBe("PspId is not associated with scheme")
      }
    }
  }

    "throw BadRequestException" when {
      "Required headers are missing" in {
        val bindings: Seq[GuiceableModule] = modules :+ bind[AuthConnector].toInstance(new FakeAuthConnector(individualRetrievals))
        running(_.overrides(
          bindings *
        )) { app =>

          val badRequestGen = Gen.oneOf(Seq(
            Seq(),
            Seq("bad" -> "request")
          ))

          forAll(badRequestGen) { badRequest =>
            val result = updateClientReferenceController(app).updateClientReference(AuthUtils.srn)(fakeRequest.withHeaders(badRequest *))

            status(result).mustBe(BAD_REQUEST)
            contentAsString(result).mustBe("Required headers missing: pspId pstr")
          }
        }
      }

      "return result from registration when connector returns failure" in {
        val bindings: Seq[GuiceableModule] = modules :+ bind[AuthConnector].toInstance(new FakeAuthConnector(individualRetrievals))
        running(_.overrides(
          bindings *
        )) { app =>
          val connectorFailureGen: Gen[HttpException] = Gen.oneOf(Seq(
            new BadRequestException("INVALID_PAYLOAD"),
            new NotFoundException("NOT FOUND"),
            new ConflictException("CONFLICT")
          ))

          forAll(connectorFailureGen) { connectorFailure =>

            when(mockUpdateClientReferenceConnector.updateClientReference(any(), any())(using any(), any(), any()))
              .thenReturn(Future.successful(Left(connectorFailure)))

            val result = updateClientReferenceController(app).updateClientReference(AuthUtils.srn)(fakeRequest.withHeaders(requestHeaders *))

            ScalaFutures.whenReady(result) { _ =>
              status(result).mustBe(connectorFailure.responseCode)
            }
          }
        }
      }

      "throw Exception when authorisation retrievals fails" in {
        val retrievals = InsufficientEnrolments()
        val bindings: Seq[GuiceableModule] = modules :+ bind[AuthConnector].toInstance(new FakeFailingAuthConnector(retrievals))
        running(_.overrides(
          bindings *
        )) { app =>

          val result = updateClientReferenceController(app).updateClientReference(AuthUtils.srn)(fakeRequest.withHeaders(requestHeaders *))
          status(result).mustBe(FORBIDDEN)
        }
      }

      "throw UpstreamErrorResponse when given UpstreamErrorResponse from connector" in {

        val bindings: Seq[GuiceableModule] = modules :+ bind[AuthConnector].toInstance(new FakeAuthConnector(individualRetrievals))
        running(_.overrides(
          bindings *
        )) { app =>

          val failureResponse = Json.obj(
            "code" -> "SERVER_ERROR",
            "reason" -> "DES is currently experiencing problems that require live service intervention."
          )

          when(mockUpdateClientReferenceConnector.updateClientReference(any(), any())(using any(), any(), any()))
            .thenReturn(Future.failed(UpstreamErrorResponse(failureResponse.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))
          val result = updateClientReferenceController(app).updateClientReference(AuthUtils.srn)(fakeRequest.withHeaders(requestHeaders *))

          ScalaFutures.whenReady(result.failed) { e =>
            e `mustBe` a[UpstreamErrorResponse]
            e.getMessage.mustBe(failureResponse.toString())

            verify(mockUpdateClientReferenceConnector, times(1))
              .updateClientReference(any(), any())(using any(), any(), any())
          }
        }
      }

      "throw Exception when any other exception returned from connector" in {
        val bindings: Seq[GuiceableModule] = modules :+ bind[AuthConnector].toInstance(new FakeAuthConnector(individualRetrievals))
        running(_.overrides(
          bindings *
        )) { app =>
          when(mockUpdateClientReferenceConnector.updateClientReference(any(), any())(using any(), any(), any()))
            .thenReturn(Future.failed(new Exception("Generic Exception")))
          val result = updateClientReferenceController(app).updateClientReference(AuthUtils.srn)(fakeRequest.withHeaders(requestHeaders *))

          ScalaFutures.whenReady(result.failed) { e =>
            e `mustBe` a[Exception]
            e.getMessage.mustBe("Generic Exception")

            verify(mockUpdateClientReferenceConnector, times(1))
              .updateClientReference(any(), any())(using any(), any(), any())
          }
        }
      }
    }

  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")
}
