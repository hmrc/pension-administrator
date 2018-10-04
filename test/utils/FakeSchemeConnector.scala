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

package utils

import connectors.SchemeConnector
import models.PsaSubscription.PsaSubscription
import org.joda.time.LocalDate
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import utils.testhelpers.PsaSubscriptionBuilder.psaSubscription

import scala.concurrent.{ExecutionContext, Future}

class FakeSchemeConnector extends SchemeConnector {

  import FakeSchemeConnector._

  private var registerPsaResponse: Future[Either[HttpException, JsValue]] = Future.successful(Right(registerPsaResponseJson))
  private var getPsaResponse: Future[Either[HttpException, PsaSubscription]] = Future.successful(Right(psaSubscription))

  def setRegisterPsaResponse(response: Future[Either[HttpException, JsValue]]): Unit = this.registerPsaResponse = response
  def setPsaDetailsResponse(response: Future[Either[HttpException, PsaSubscription]]): Unit = this.getPsaResponse = response

  override def registerPSA(registerData: JsValue)(implicit
                                                  headerCarrier: HeaderCarrier,
                                                  ec: ExecutionContext,
                                                  request: RequestHeader): Future[Either[HttpException, JsValue]] = registerPsaResponse

  override def getPSASubscriptionDetails(psaId: String)(implicit
                                                        headerCarrier: HeaderCarrier,
                                                        ec: ExecutionContext,
                                                        request: RequestHeader): Future[Either[HttpException, PsaSubscription]] = getPsaResponse
}

object FakeSchemeConnector {

  val registerPsaResponseJson: JsValue =
    Json.obj(
      "processingDate" -> LocalDate.now,
      "formBundle" -> "1121313",
      "psaId" -> "A21999999"
    )

}
