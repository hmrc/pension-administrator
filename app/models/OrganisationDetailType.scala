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
                                  vatRegistrationNumber: Option[String] = None, payeReference: Option[String] = None,
                                  isUpdated: Option[Boolean] = None) extends PSADetail

object OrganisationDetailType {
  implicit val formats: OFormat[OrganisationDetailType] = Json.format[OrganisationDetailType]

  val companyDetailsReads: Reads[Option[(Option[String], Option[String])]] = (
    (JsPath \ "vatRegistrationNumber").readNullable[String] and
      (JsPath \ "payeEmployerReferenceNumber").readNullable[String]
    ) ((vatRegistrationNumber, payeEmployerReferenceNumber) => {
    (vatRegistrationNumber, payeEmployerReferenceNumber) match {
      case (None, None) => None
      case _ => Some((vatRegistrationNumber, payeEmployerReferenceNumber))
    }
  })

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
    (JsPath \ "businessDetails" \ "companyName").read[String] and
      (JsPath \ "companyDetails").readNullable(companyDetailsReads) and
      (JsPath \ "companyRegistrationNumber").readNullable[String]
    ) ((name, companyDetails: Option[Option[(Option[String], Option[String])]], crnNumber) =>
    OrganisationDetailType(name, vatRegistrationNumber = companyDetails.flatMap(c => c.flatMap(c => c._1)),
      payeReference = companyDetails.flatMap(c => c.flatMap(c => c._2)),
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
