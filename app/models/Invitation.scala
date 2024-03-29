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

import org.joda.time.DateTime
import play.api.libs.json.JodaWrites._
import play.api.libs.json._
import uk.gov.hmrc.domain.PsaId

import java.time.LocalDateTime

case class Invitation(srn: SchemeReferenceNumber,
                      pstr: String,
                      schemeName: String,
                      inviterPsaId: PsaId,
                      inviteePsaId: PsaId,
                      inviteeName: String,
                      expireAt: LocalDateTime
                     )

object Invitation {
  implicit val jodaDateFormat: Format[DateTime] = new Format[DateTime] {
    override def reads(json: JsValue): JsResult[DateTime] = JodaReads.DefaultJodaDateTimeReads.reads(json)
    override def writes(o: DateTime): JsValue = JodaDateTimeNumberWrites.writes(o)
  }
  implicit val formats: Format[Invitation] = Json.format[Invitation]
}
