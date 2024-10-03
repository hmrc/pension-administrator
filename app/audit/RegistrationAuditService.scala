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

package audit

import models.{Address, UkAddress, User}
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpException

import scala.util.{Failure, Success, Try}

trait RegistrationAuditService {

  private val logger = Logger(classOf[RegistrationAuditService])

  def organisationPsaType(registerData: JsValue): String = {
    (registerData \ "organisation" \ "organisationType").validate[String].fold(
      _ => "Unknown",
      organisationType => organisationType
    )
  }

  def withIdIsUk(response: JsValue): Option[Boolean] = {

    (response \ "address").as[Address] match {
      case _: UkAddress => Some(true)
      case _ => Some(false)
    }
  }

  def sendPSARegistrationEvent(
                                withId: Boolean,
                                user: User,
                                psaType: String,
                                registerData: JsValue,
                                isUk: JsValue => Option[Boolean]
                              )(
                                sendEvent: PSARegistration => Unit
                              ): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {

    case Success(Right(jsValue)) =>
      sendAuditEvent(
        withId = withId,
        externalId = user.externalId,
        psaType = psaType,
        found = true,
        isUk = isUk(jsValue),
        status = Status.OK,
        request = registerData,
        response = Some(jsValue)
      )(sendEvent)

    case Success(Left(e)) =>
      sendAuditEvent(
        withId = withId,
        externalId = user.externalId,
        psaType = psaType,
        found = false,
        isUk = None,
        status = e.responseCode,
        request = registerData,
        response = None
      )(sendEvent)

    case Failure(t) =>
      logger.error("Error sendPSARegistrationEvent", t)
      sendAuditEvent(
        withId = withId,
        externalId = user.externalId,
        psaType = psaType,
        found = false,
        isUk = None,
        status = 0,
        request = registerData,
        response = Some(Json.obj("error" -> "Error sendPSARegistrationEvent", "message" -> t.getMessage))
      )(sendEvent)

  }


  def sendPSARegWithoutIdEvent(
                                withId: Boolean,
                                user: User,
                                psaType: String,
                                registerData: JsValue,
                                isUk: JsValue => Option[Boolean]
                              )(
                                sendEvent: PSARegistration => Unit
                              ): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {

    case Success(Right(jsValue)) =>
      sendAuditEvent(
        withId = withId,
        externalId = user.externalId,
        psaType = psaType,
        found = true,
        isUk = isUk(jsValue),
        status = Status.OK,
        request = registerData,
        response = Some(Json.toJson(jsValue))
      )(sendEvent)

    case Success(Left(e)) =>
      sendAuditEvent(
        withId = withId,
        externalId = user.externalId,
        psaType = psaType,
        found = false,
        isUk = None,
        status = e.responseCode,
        request = registerData,
        response = None
      )(sendEvent)

    case Failure(t) =>
      logger.error("Error sendPSARegWithoutIdEvent", t)
      sendAuditEvent(
        withId = withId,
        externalId = user.externalId,
        psaType = psaType,
        found = false,
        isUk = None,
        status = 0,
        request = registerData,
        response = Some(Json.obj("error" -> "Error sendPSARegWithoutIdEvent", "message" -> t.getMessage))
      )(sendEvent)

  }

  private def sendAuditEvent(
                              withId: Boolean,
                              externalId: String,
                              psaType: String,
                              found: Boolean,
                              isUk: Option[Boolean],
                              status: Int,
                              request: JsValue,
                              response: Option[JsValue]
                            )(
                              sendEvent: PSARegistration => Unit
                            ): Unit =
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
