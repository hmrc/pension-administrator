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

import audit.{AuditService, EmailAuditEvent}
import models.enumeration.JourneyType
import models.{EmailEvents, Opened}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.Results.{BadRequest, Forbidden, Ok}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter}
import uk.gov.hmrc.domain.PsaId

import scala.concurrent.ExecutionContext
import scala.util.Try

trait AuditEmailStatus {

  implicit val ec: ExecutionContext
  protected val crypto: Encrypter & Decrypter
  protected val logger: Logger
  protected val auditService: AuditService

  private def validatePsaId(id: String): Option[PsaId] =
    Try(
      PsaId(crypto.decrypt(Crypted(id)).value)
    ).toOption

  protected def auditEmailStatus(id: String,
                                 journeyType: JourneyType.Name
                                )(implicit request: Request[JsValue]): Result =
    validatePsaId(id).fold(
      Forbidden("Malformed PSAID")
    )(psaId =>
      request.body.validate[EmailEvents] match {
        case JsSuccess(valid, _) =>
          valid.events.filterNot(
            _.event == Opened
          ).foreach { event =>
            logger.debug(s"Email Audit event coming from $journeyType is $event")
            auditService.sendEvent(EmailAuditEvent(psaId, event.event, journeyType))
          }
          Ok
        case JsError(_) =>
          BadRequest("Bad request received for email call back event")
      }
    )
}
