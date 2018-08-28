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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads, __}

case class PensionAdvisorDetail(name: String, addressDetail: Address, contactDetail: ContactDetails)

object PensionAdvisorDetail {
  implicit val formats = Json.format[PensionAdvisorDetail]

  val apiReads: Reads[Option[PensionAdvisorDetail]] = (
    (JsPath \ "adviserDetails").readNullable((__ \ "name").readNullable[String]) and
      (JsPath \ "adviserAddress").readNullable[Address] and
      (JsPath \ "adviserDetails").readNullable(ContactDetails.apiReads)
    ) ((name, address, contactDetails) => {
    (name, address, contactDetails) match {
      case (Some(name), Some(address), Some(contactDetails)) => Some(PensionAdvisorDetail(name.get, address, contactDetails))
      case _ => None
    }
  })
}
