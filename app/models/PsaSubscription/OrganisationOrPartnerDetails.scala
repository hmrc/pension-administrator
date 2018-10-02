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

case class OrganisationOrPartnerDetails(name: String, crn: Option[String], vatRegistration: Option[String], paye: Option[String])

object OrganisationOrPartnerDetails {
  implicit val writes : Writes[OrganisationOrPartnerDetails] = Json.writes[OrganisationOrPartnerDetails]
  implicit val reads : Reads[OrganisationOrPartnerDetails] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "crnNumber").readNullable[String] and
      (JsPath \ "vatRegistrationNumber").readNullable[String] and
      (JsPath \ "payeReference").readNullable[String]
    )(OrganisationOrPartnerDetails.apply _)
}