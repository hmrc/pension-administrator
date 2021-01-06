/*
 * Copyright 2021 HM Revenue & Customs
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

import models.AcceptedInvitation
import play.api.libs.json.{Format, JsValue, Json}

case class InvitationAcceptanceAuditEvent(acceptedInvitation: AcceptedInvitation,
                                          status: Int,
                                          response: Option[JsValue]
                                         ) extends AuditEvent {

  override def auditType: String = "PSAInvitationAccepted"

  override def details: Map[String, String] =
    Map(
      "pstr" -> acceptedInvitation.pstr,
      "inviteePsaId" -> acceptedInvitation.inviteePsaId.id,
      "inviterPsaId" -> acceptedInvitation.inviterPsaId.id,
      "declaration" -> acceptedInvitation.declaration.toString,
      "declarationDuties" -> acceptedInvitation.declarationDuties.toString,
      "response" -> {
        response match {
          case Some(json) => Json.stringify(json)
          case _ => ""
        }
      }
    )
}


