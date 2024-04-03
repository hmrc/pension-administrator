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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId}

case class Invitation(srn: SchemeReferenceNumber,
                      pstr: String,
                      schemeName: String,
                      inviterPsaId: PsaId,
                      inviteePsaId: PsaId,
                      inviteeName: String,
                      expireAt: Instant
                     )

object Invitation {
  implicit val formats: Format[Invitation] = new Format[Invitation] {
    override def writes(o: Invitation): JsValue = Json.writes[Invitation].writes(o)

    private val instantReads = MongoJavatimeFormats.instantReads

    override def reads(json: JsValue): JsResult[Invitation] = (
      (JsPath \ "srn").read[SchemeReferenceNumber] and
        (JsPath \ "pstr").read[String] and
        (JsPath \ "schemeName").read[String] and
        (JsPath \ "inviterPsaId").read[PsaId] and
        (JsPath \ "inviteePsaId").read[PsaId] and
        (JsPath \ "inviteeName").read[String] and
        (JsPath \ "expireAt").read(instantReads).orElse(Reads.pure(Instant.now()))
      )((srn, pstr, schemeName, inviterPsaId, inviteePsaId, inviteeName, expireAt) =>
      Invitation(srn, pstr, schemeName, inviterPsaId, inviteePsaId, inviteeName, expireAt)
    ).reads(json)
  }
}
