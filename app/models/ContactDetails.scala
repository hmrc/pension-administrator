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
import play.api.libs.json.Reads._
import play.api.libs.json._

case class ContactDetails(telephone: String, mobileNumber: Option[String] = None, fax: Option[String] = None, email: String, isChanged: Option[Boolean] = None)

object ContactDetails {
  implicit val formats: OFormat[ContactDetails] = Json.format[ContactDetails]
  val apiReads: Reads[ContactDetails] = (
    (JsPath \ "phone").read[String] and
      (JsPath \ "email").read[String] and
      (JsPath \ "isChanged").readNullable[Boolean]
    ) ((phone, email, isChanged) => ContactDetails(phone, email = email, isChanged = isChanged)
  )

  val updateWrites: Writes[ContactDetails] = (
    (JsPath \ "telephone").write[String] and
      (JsPath \ "email").write[String] and
      (JsPath \ "changeFlag").write[Boolean]
    ) (contactDetails => (contactDetails.telephone, contactDetails.email, contactDetails.isChanged.fold(false)(identity)))
}
