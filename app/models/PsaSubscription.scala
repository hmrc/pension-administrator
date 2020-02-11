/*
 * Copyright 2020 HM Revenue & Customs
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

import org.joda.time.LocalDate
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads, Writes}
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

case class CorrespondenceAddress(addressLine1: String, addressLine2: String, addressLine3: Option[String], addressLine4: Option[String], countryCode: String, postalCode: Option[String])

object CorrespondenceAddress {
  implicit val writes: Writes[CorrespondenceAddress] = Json.writes[CorrespondenceAddress]
  implicit val reads: Reads[CorrespondenceAddress] = (
    (JsPath \ "line1").read[String] and
      (JsPath \ "line2").read[String] and
      (JsPath \ "line3").readNullable[String] and
      (JsPath \ "line4").readNullable[String] and
      (JsPath \ "countryCode").read[String] and
      (JsPath \ "postalCode").readNullable[String]
    ) (CorrespondenceAddress.apply _)
}

case class CorrespondenceDetails(address: CorrespondenceAddress, contactDetails: Option[PsaContactDetails])

object CorrespondenceDetails {
  implicit val writes : Writes[CorrespondenceDetails] = Json.writes[CorrespondenceDetails]
  implicit val reads : Reads[CorrespondenceDetails] = (
    (JsPath \ "addressDetails").read[CorrespondenceAddress] and
      (JsPath \ "contactDetails").readNullable[PsaContactDetails]
    )(CorrespondenceDetails.apply _)
}


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


case class DirectorOrPartner(isDirectorOrPartner: String,
                             title: Option[String],
                             firstName: String,
                             middleName: Option[String],
                             lastName: String,
                             dateOfBirth: LocalDate,
                             nino: Option[String],
                             utr: Option[String],
                             isSameAddressForLast12Months: Boolean,
                             previousAddress: Option[CorrespondenceAddress],
                             correspondenceDetails: Option[CorrespondenceDetails])

object DirectorOrPartner {
  implicit val writes : Writes[DirectorOrPartner] = Json.writes[DirectorOrPartner]
  implicit val reads : Reads[DirectorOrPartner] = (
    (JsPath \ "entityType").read[String] and
      (JsPath \ "title").readNullable[String] and
      (JsPath \ "firstName").read[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "dateOfBirth").read[LocalDate] and
      (JsPath \ "nino").readNullable[String] and
      (JsPath \ "utr").readNullable[String] and
      (JsPath \ "previousAddressDetails" \ "isPreviousAddressLast12Month").read[Boolean] and
      (JsPath \ "previousAddressDetails" \ "previousAddress").readNullable[CorrespondenceAddress] and
      (JsPath \ "correspondenceCommonDetails").readNullable[CorrespondenceDetails]
    )((entityType,title,name,middleName,surname,dob,nino,utr,isSameAddress,previousAddress,correspondence) =>
    DirectorOrPartner(entityType,title,name,middleName,surname,dob,nino,utr,isSameAddress,previousAddress,correspondence))
}


case class OrganisationOrPartner(name: String, crn: Option[String], vatRegistration: Option[String], paye: Option[String])

object OrganisationOrPartner {
  implicit val writes : Writes[OrganisationOrPartner] = Json.writes[OrganisationOrPartner]
  implicit val reads : Reads[OrganisationOrPartner] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "crnNumber").readNullable[String] and
      (JsPath \ "vatRegistrationNumber").readNullable[String] and
      (JsPath \ "payeReference").readNullable[String]
    )(OrganisationOrPartner.apply _)
}

case class PensionAdvisor(name: String, address: CorrespondenceAddress, contactDetails: Option[PsaContactDetails])

object PensionAdvisor {
  implicit val writes : Writes[PensionAdvisor] = Json.writes[PensionAdvisor]
  implicit val reads : Reads[PensionAdvisor] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "addressDetails").read[CorrespondenceAddress] and
      (JsPath \ "contactDetails").readNullable[PsaContactDetails]
    )(PensionAdvisor.apply _)
}


case class PsaContactDetails(telephone: String, email: Option[String])

object PsaContactDetails {
  implicit val writes : Writes[PsaContactDetails] = Json.writes[PsaContactDetails]
  implicit val reads : Reads[PsaContactDetails] = (
    (JsPath \ "telephone").read[String] and
      (JsPath \ "email").readNullable[String])(PsaContactDetails.apply _)
}


case class PsaSubscription(isSuspended: Boolean, customerIdentification: CustomerIdentification,
                           organisationOrPartner: Option[OrganisationOrPartner], individual: Option[IndividualDetailType], address: CorrespondenceAddress,
                           contact: PsaContactDetails, isSameAddressForLast12Months: Boolean, previousAddress: Option[CorrespondenceAddress],
                           directorsOrPartners: Option[Seq[DirectorOrPartner]], pensionAdvisor: Option[PensionAdvisor]){


  def name: Option[String] = {
    (individual, organisationOrPartner) match {
      case (Some(ind),None) => Some(Seq(ind.firstName,ind.middleName.getOrElse(),ind.lastName).mkString(" "))
      case (None, Some(org)) => Some(org.name)
      case _ => None
    }
  }
}

object PsaSubscription {
  implicit val writes : Writes[PsaSubscription] = Json.writes[PsaSubscription]
  implicit val reads : Reads[PsaSubscription] = (
    (JsPath \ "psaSubscriptionDetails" \ "isPSASuspension").read[Boolean] and
      (JsPath \ "psaSubscriptionDetails" \ "customerIdentificationDetails").read[CustomerIdentification] and
      (JsPath \ "psaSubscriptionDetails" \ "organisationOrPartnerDetails").readNullable[OrganisationOrPartner] and
      (JsPath \ "psaSubscriptionDetails" \ "individualDetails").readNullable[IndividualDetailType] and
      (JsPath \ "psaSubscriptionDetails" \ "correspondenceAddressDetails").read[CorrespondenceAddress] and
      (JsPath \ "psaSubscriptionDetails" \ "correspondenceContactDetails").read[PsaContactDetails] and
      (JsPath \ "psaSubscriptionDetails" \ "previousAddressDetails" \ "isPreviousAddressLast12Month").read[Boolean] and
      (JsPath \ "psaSubscriptionDetails" \ "previousAddressDetails" \ "previousAddress").readNullable[CorrespondenceAddress] and
      (JsPath \ "psaSubscriptionDetails" \ "directorOrPartnerDetails").readNullable[Seq[DirectorOrPartner]] and
      (JsPath \ "psaSubscriptionDetails" \ "declarationDetails" \ "pensionAdvisorDetails").readNullable[PensionAdvisor]
    )((isSuspended,customerIdentification, organisationOrPartnerDetails, individual, address, contactDetails,
       isSameAddress, prevAddress, directorOrPartner, advisor) =>
    PsaSubscription(isSuspended,customerIdentification,organisationOrPartnerDetails, individual, address,
      contactDetails, isSameAddress, prevAddress, directorOrPartner, advisor))
}