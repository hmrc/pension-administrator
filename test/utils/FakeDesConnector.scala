/*
 * Copyright 2023 HM Revenue & Customs
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

package utils

import connectors.DesConnector
import models.PsaToBeRemovedFromScheme
import org.joda.time.LocalDate
import play.api.libs.json.JodaWrites._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import utils.testhelpers.PsaSubscriptionBuilder.psaSubscription

import scala.concurrent.{ExecutionContext, Future}

class FakeDesConnector extends DesConnector {

  import FakeDesConnector._

  private var registerPsaResponse: Future[Either[HttpException, JsValue]] = Future.successful(Right(registerPsaResponseJson))
  private var getPsaResponse: Future[Either[HttpException, JsValue]] = Future.successful(Right(Json.toJson(psaSubscription)))
  private var removePsaResponse: Future[Either[HttpException, JsValue]] = Future.successful(Right(removePsaResponseJson))
  private var deregisterPsaResponse: Future[Either[HttpException, JsValue]] = Future.successful(Right(deregisterPsaResponseJson))
  private var updatePsaResponse: Future[Either[HttpException, JsValue]] = Future.successful(Right(updatePsaResponseJson))

  def setRegisterPsaResponse(response: Future[Either[HttpException, JsValue]]): Unit = this.registerPsaResponse = response

  def setPsaDetailsResponse(response: Future[Either[HttpException, JsValue]]): Unit = this.getPsaResponse = response

  def setRemovePsaResponse(response: Future[Either[HttpException, JsValue]]): Unit = this.removePsaResponse = response

  def setDeregisterPsaResponse(response: Future[Either[HttpException, JsValue]]): Unit = this.deregisterPsaResponse = response

  def setUpdatePsaResponse(response: Future[Either[HttpException, JsValue]]): Unit = this.updatePsaResponse = response

  override def registerPSA(registerData: JsValue)(implicit
                                                  headerCarrier: HeaderCarrier,
                                                  ec: ExecutionContext,
                                                  request: RequestHeader): Future[Either[HttpException, JsValue]] = registerPsaResponse

  override def getPSASubscriptionDetails(psaId: String)(implicit
                                                        headerCarrier: HeaderCarrier,
                                                        ec: ExecutionContext,
                                                        request: RequestHeader): Future[Either[HttpException, JsValue]] = getPsaResponse

  override def removePSA(psaToBeRemoved: PsaToBeRemovedFromScheme)(implicit
                                                                   headerCarrier: HeaderCarrier,
                                                                   ec: ExecutionContext,
                                                                   request: RequestHeader): Future[Either[HttpException, JsValue]] = removePsaResponse

  override def deregisterPSA(psaId: String)(implicit headerCarrier: HeaderCarrier,
                                            ec: ExecutionContext,
                                            request: RequestHeader): Future[Either[HttpException, JsValue]] = deregisterPsaResponse

  override def updatePSA(psaId: String, data: JsValue)(implicit headerCarrier: HeaderCarrier,
                                                       ec: ExecutionContext,
                                                       request: RequestHeader): Future[Either[HttpException, JsValue]] = updatePsaResponse
}

object FakeDesConnector {

  val registerPsaResponseJson: JsValue =
    Json.obj(
      "processingDate" -> LocalDate.now,
      "formBundle" -> "1121313",
      "psaId" -> "A21999999"
    )

  val updatePsaResponseJson = Json.obj(
    "processingDate" -> "2001-12-17T09:30:47Z",
    "formBundleNumber" -> "12345678912"
  )

  val removePsaResponseJson: JsValue = Json.obj("processingDate" -> LocalDate.now)
  val deregisterPsaResponseJson: JsValue = Json.obj("processingDate" -> LocalDate.now)
}
