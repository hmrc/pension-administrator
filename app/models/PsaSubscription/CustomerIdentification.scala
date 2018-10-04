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

package models.PsaSubscription

import play.api.libs.json.{JsPath, Json, Reads, Writes}
import play.api.libs.functional.syntax._

case class CustomerIdentification(legalStatus: String, typeOfId: Option[String], number: Option[String], isOverseasCustomer: Boolean)

object CustomerIdentification {
  implicit val reads : Reads[CustomerIdentification] = (
    (JsPath \ "legalStatus").read[String] and
      (JsPath \ "idType").readNullable[String] and
      (JsPath \ "idNumber").readNullable[String] and
      (JsPath \ "noIdentifier").read[Boolean]
    )(CustomerIdentification.apply _)
  implicit val writes : Writes[CustomerIdentification] = Json.writes[CustomerIdentification]
}