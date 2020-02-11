/*
 * Copyright 2020 HM Revenue & Customs
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

package audit

import models.registrationnoid.RegisterWithoutIdResponse
import models.{SuccessResponse, UkAddress, User}
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpException

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait RegistrationAuditService {

  def organisationPsaType(registerData: JsValue): String = {
    (registerData \ "organisation" \ "organisationType").validate[String].fold(
      _ => "Unknown",
      organisationType => organisationType
    )
  }

  def withIdIsUk(response: SuccessResponse): Option[Boolean] = {

    response.address match {
      case _: UkAddress => Some(true)
      case _ => Some(false)
    }
  }

  def sendPSARegistrationEvent(withId: Boolean, user: User, psaType: String, registerData: JsValue, isUk: SuccessResponse => Option[Boolean])
                              (sendEvent: PSARegistration => Unit)
                              (implicit request: RequestHeader, ec: ExecutionContext): PartialFunction[Try[Either[HttpException, SuccessResponse]], Unit] = {

    case Success(Right(successResponse)) =>
      sendAuditEvent(withId, user.externalId, psaType, true, isUk(successResponse), Status.OK, registerData, Some(Json.toJson(successResponse)))(sendEvent)

    case Success(Left(e)) =>
      sendAuditEvent(withId, user.externalId, psaType, false, None, e.responseCode, registerData, None)(sendEvent)

    case Failure(t) =>
      Logger.error("Error in registration connector", t)

  }


  def sendPSARegWithoutIdEvent(withId: Boolean, user: User, psaType: String, registerData: JsValue, isUk: JsValue => Option[Boolean])
                              (sendEvent: PSARegistration => Unit)
                              (implicit request: RequestHeader, ec: ExecutionContext): PartialFunction[Try[Either[HttpException, RegisterWithoutIdResponse]], Unit] = {

    case Success(Right(registerWithoutIdResponse)) =>
      sendAuditEvent(withId, user.externalId, psaType, true, isUk(Json.toJson(registerWithoutIdResponse)),
        Status.OK, registerData, Some(Json.toJson(registerWithoutIdResponse)))(sendEvent)

    case Success(Left(e)) =>
      sendAuditEvent(withId, user.externalId, psaType, false, None, e.responseCode, registerData, None)(sendEvent)

    case Failure(t) =>
      Logger.error("Error in registration connector", t)

  }

  private def sendAuditEvent(withId: Boolean, externalId: String, psaType: String, found: Boolean, isUk: Option[Boolean],
                             status: Int, request: JsValue, response: Option[JsValue])(sendEvent: PSARegistration => Unit): Unit

  = {
    sendEvent(
      PSARegistration(
        withId = withId,
        externalId = externalId,
        psaType = psaType,
        found = found,
        isUk = isUk,
        status = status,
        request = request,
        response = response
      )
    )
  }

}
