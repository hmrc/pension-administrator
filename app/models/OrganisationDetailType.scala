/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.libs.json.{Json, JsPath, OFormat, Reads}
import play.api.libs.json.JsObject

case class OrganisationDetailType(name: String, crnNumber: Option[String] = None,
                                  vatRegistrationNumber: Option[String] = None, payeReference: Option[String] = None) extends PSADetail

object OrganisationDetailType {
  implicit val formats: OFormat[OrganisationDetailType] = Json.format[OrganisationDetailType]

  val partnershipApiReads: Reads[OrganisationDetailType] = (
    (JsPath \ "partnershipDetails" \ "companyName").read[String] and
      (JsPath \ "partnershipVat").readNullable[PartnershipVat] and
      (JsPath \ "partnershipPaye").readNullable[PartnershipPaye]
    ) ((name, partnershipVat, partnershipPaye) =>
    OrganisationDetailType(name,
      None,
      partnershipVat.flatMap(_.vat),
      partnershipPaye.flatMap(_.paye)))

  val CompanyApiReads: Reads[OrganisationDetailType] = (
    (JsPath \ "businessName").read[String] and
      (JsPath \ "vat").readNullable[String] and
      (JsPath \ "paye").readNullable[String] and
      (JsPath \ "companyRegistrationNumber").readNullable[String]
    ) ((name, vat, paye, crnNumber) =>
    OrganisationDetailType(name, vatRegistrationNumber = vat,
      payeReference = paye,
      crnNumber = crnNumber))
}


case class PartnershipVat(vat: Option[String])

object PartnershipVat {
  implicit val formats: OFormat[PartnershipVat] = Json.format[PartnershipVat]
}

case class PartnershipPaye(paye: Option[String])

object PartnershipPaye {
  implicit val formats: OFormat[PartnershipPaye] = Json.format[PartnershipPaye]
}
