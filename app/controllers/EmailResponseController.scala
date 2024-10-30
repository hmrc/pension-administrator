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

import audit.{AuditService, EmailAuditEvent}
import com.google.inject.Inject
import controllers.actions.AuthAction
import models.enumeration.JourneyType
import models.{EmailEvents, Opened}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc._
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class EmailResponseController @Inject()(
                                         auditService: AuditService,
                                         crypto: ApplicationCrypto,
                                         cc: ControllerComponents,
                                         authAction: AuthAction,
                                         parser: PlayBodyParsers
                                       )(implicit val ec: ExecutionContext) extends BackendController(cc) {

  private val logger = Logger(classOf[EmailResponseController])

  def retrieveStatus(journeyType: JourneyType.Name, id: String): Action[JsValue] = authAction.async(parser.tolerantJson) {
    implicit request =>
      validatePsaId(id) match {
        case Right(psaId) =>
          request.body.validate[EmailEvents].fold(
            _ => Future.successful(BadRequest("Bad request received for email call back event")),
            valid => {
              valid.events.filterNot(
                _.event == Opened
              ).foreach { event =>
                logger.debug(s"Email Audit event coming from $journeyType is $event")
                auditService.sendEvent(EmailAuditEvent(psaId, event.event, journeyType))
              }
              Future.successful(Ok)
            }
          )

        case Left(result) => Future.successful(result)
      }
  }

  private def validatePsaId(id: String): Either[Result, PsaId] =
    try {
      Right(PsaId {
        crypto.QueryParameterCrypto.decrypt(Crypted(id)).value
      })
    } catch {
      case _: IllegalArgumentException | _: SecurityException => Left(Forbidden("Malformed PSAID"))
    }
}
