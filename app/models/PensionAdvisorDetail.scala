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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class PensionAdvisorDetail(name: String, addressDetail: Address, contactDetail: ContactDetails)

object PensionAdvisorDetail {
  implicit val formats = Json.format[PensionAdvisorDetail]

  val apiReads: Reads[Option[PensionAdvisorDetail]] = (
    (JsPath \ "adviserName").readNullable[String] and
      (JsPath \ "adviserAddress").readNullable[Address] and
      (JsPath \ "adviserEmail").readNullable[String] and
      (JsPath \ "adviserPhone").readNullable[String]
    ) ((name, address, email, phone) => {
    (name, address, email, phone) match {
      case (Some(adviserName), Some(address), Some(email), Some(phone)) =>
        Some(PensionAdvisorDetail(adviserName, address, ContactDetails(telephone = phone, email = email)))
      case _ => None
    }
  })

  val psaUpdateWrites: Writes[PensionAdvisorDetail] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "addressDetails").write[Address](Address.updateWrites) and
      (JsPath \ "contactDetails").write[ContactDetails]
    ) (
    details =>
      (details.name, details.addressDetail, details.contactDetail)
  )
}
