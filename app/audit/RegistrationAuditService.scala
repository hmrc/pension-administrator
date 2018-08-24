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

package audit

import models.{OrganisationRegistrant, SuccessResponse, UkAddress, User}
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.JsValue
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

  def noIdIsUk(organisation: OrganisationRegistrant)(response: JsValue): Option[Boolean] = {
    organisation.address match {
      case _: UkAddress => Some(true)
      case _ => Some(false)
    }
  }

  def withIdIsUk(response: JsValue): Option[Boolean] = {

    response.validate[SuccessResponse].fold(
      _ => None,
      success => success.address match {
        case _: UkAddress => Some(true)
        case _ => Some(false)
      }
    )

  }

  def sendPSARegistrationEvent(withId: Boolean, user: User, psaType: String, registerData: JsValue, isUk: JsValue => Option[Boolean])
                              (sendEvent: PSARegistration => Unit)
                              (implicit request: RequestHeader, ec: ExecutionContext): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {

    case Success(Right(json)) =>
      sendEvent(
        PSARegistration(
          withId = withId,
          externalId = user.externalId,
          psaType = psaType,
          found = true,
          isUk = isUk(json),
          status = Status.OK,
          request = registerData,
          response = Some(json)
        )
      )
    case Success(Left(e)) =>
      sendEvent(
        PSARegistration(
          withId = withId,
          externalId = user.externalId,
          psaType = psaType,
          found = false,
          isUk = None,
          status = e.responseCode,
          request = registerData,
          response = None
        )
      )
    case Failure(t) =>
      Logger.error("Error in registration connector", t)

  }

}
