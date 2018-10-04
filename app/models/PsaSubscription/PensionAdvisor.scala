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

import models.CorrespondenceAddress
import play.api.libs.json.{JsPath, Json, Reads, Writes}
import play.api.libs.functional.syntax._

case class PensionAdvisor(name: String, address: CorrespondenceAddress, contactDetails: Option[PsaContactDetails])

object PensionAdvisor {
  implicit val writes : Writes[PensionAdvisor] = Json.writes[PensionAdvisor]
  implicit val reads : Reads[PensionAdvisor] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "addressDetails").read[CorrespondenceAddress] and
      (JsPath \ "contactDetails").readNullable[PsaContactDetails]
    )(PensionAdvisor.apply _)
}
