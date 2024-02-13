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
import play.api.libs.json.{JsPath, Json, OFormat, Reads}

case class OrganisationDetailType(name: String,
                                  crnNumber: Option[String] = None,
                                  vatRegistrationNumber: Option[String] = None,
                                  payeReference: Option[String] = None
                                 ) extends PSADetail

object OrganisationDetailType {
  implicit val formats: OFormat[OrganisationDetailType] = Json.format[OrganisationDetailType]

  val partnershipApiReads: Reads[OrganisationDetailType] = (
    (JsPath \ "businessName").read[String] and
      (JsPath \ "vat").readNullable[String] and
      (JsPath \ "paye").readNullable[String]
    ) ((name, vat, partnershipPaye) =>
    OrganisationDetailType(
      name = name,
      crnNumber = None,
      vatRegistrationNumber = vat,
      payeReference = partnershipPaye
    ))

  val companyApiReads: Reads[OrganisationDetailType] = (
    (JsPath \ "businessName").read[String] and
      (JsPath \ "vat").readNullable[String] and
      (JsPath \ "paye").readNullable[String] and
      (JsPath \ "companyRegistrationNumber").readNullable[String]
    ) ((name, vat, paye, crnNumber) =>
    OrganisationDetailType(
      name = name,
      crnNumber = crnNumber,
      vatRegistrationNumber = vat,
      payeReference = paye
    ))
}

case class PartnershipPaye(paye: Option[String])

object PartnershipPaye {
  implicit val formats: OFormat[PartnershipPaye] = Json.format[PartnershipPaye]
}
