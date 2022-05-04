/*
 * Copyright 2022 HM Revenue & Customs
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

import models.{IdentifierDetails, UpdateClientReferenceRequest}
import play.api.libs.json.{JsValue, Json}

case class UpdateClientReferenceAuditEvent(updateClientReferenceRequest: UpdateClientReferenceRequest, response: Option[JsValue]) extends AuditEvent {
  override def auditType: String = "UpdateClientReferenceAudit"

  override def details: Map[String, String] =
    Map(
      "pstr" -> updateClientReferenceRequest.pstr,
      "psaId" -> updateClientReferenceRequest.psaId,
      "pspId" -> updateClientReferenceRequest.pspId,
      "clientReference" -> updateClientReferenceRequest.clientReference.getOrElse(""),
      "userAction" -> "",
      "response" -> {
        response match {
          case Some(json) => Json.stringify(json)
          case _ => ""
        }
      }
    )
}


