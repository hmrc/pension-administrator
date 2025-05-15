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

package models

import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.format.DateTimeFormatter
import java.time.{Instant, OffsetDateTime}

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

    def parseTimestamp: Reads[Instant] = Reads[Instant] { json =>
      val instantResult = instantReads.reads(json)
      instantResult match {
        case JsSuccess(instant, _) =>
          JsSuccess(instant)
        case JsError(_) =>
          val timestamp = json.as[String]
          val stringInstant = try {
            OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          } catch {
             case _: Exception =>
              OffsetDateTime.parse(timestamp + "Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME)
           }
          JsSuccess(stringInstant.toInstant)
      }
    }

    override def reads(json: JsValue): JsResult[Invitation] = (
      (JsPath \ "srn").read[SchemeReferenceNumber] and
        (JsPath \ "pstr").read[String] and
        (JsPath \ "schemeName").read[String] and
        (JsPath \ "inviterPsaId").read[PsaId] and
        (JsPath \ "inviteePsaId").read[PsaId] and
        (JsPath \ "inviteeName").read[String] and
        (JsPath \ "expireAt").read(using parseTimestamp)
      )((srn, pstr, schemeName, inviterPsaId, inviteePsaId, inviteeName, expireAt) =>
      Invitation(srn, pstr, schemeName, inviterPsaId, inviteePsaId, inviteeName, expireAt)
    ).reads(json)

  }
}
