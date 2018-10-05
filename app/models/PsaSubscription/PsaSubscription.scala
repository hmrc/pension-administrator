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

import models.{CorrespondenceAddress, IndividualDetailType}
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads, Writes}

case class PsaSubscription(isSuspended: Boolean, customerIdentification: CustomerIdentification,
                           organisationOrPartner: Option[OrganisationOrPartner], individual: Option[IndividualDetailType], address: CorrespondenceAddress,
                           contact: PsaContactDetails, isSameAddressForLast12Months: Boolean, previousAddress: Option[CorrespondenceAddress],
                           directorsOrPartners: Option[Seq[DirectorOrPartner]], pensionAdvisor: Option[PensionAdvisor])

object PsaSubscription {
  implicit val writes : Writes[PsaSubscription] = Json.writes[PsaSubscription]
  implicit val reads : Reads[PsaSubscription] = (
    (JsPath \ "isPSASuspension").read[Boolean] and
      (JsPath \ "customerIdentificationDetails").read[CustomerIdentification] and
      (JsPath \ "organisationOrPartnerDetails").readNullable[OrganisationOrPartner] and
      (JsPath \ "individualDetails").readNullable[IndividualDetailType] and
      (JsPath \ "correspondenceAddressDetails").read[CorrespondenceAddress] and
      (JsPath \ "correspondenceContactDetails").read[PsaContactDetails] and
      (JsPath \ "previousAddressDetails" \ "isPreviousAddressLast12Month").read[Boolean] and
      (JsPath \ "previousAddressDetails" \ "previousAddress").readNullable[CorrespondenceAddress] and
      (JsPath \ "directorOrPartnerDetails").readNullable[Seq[DirectorOrPartner]] and
      (JsPath \ "declarationDetails" \ "pensionAdvisorDetails").readNullable[PensionAdvisor]
    )((isSuspended,customerIdentification, organisationOrPartnerDetails, individual, address, contactDetails,
       isSameAddress, prevAddress, directorOrPartner, advisor) =>
    PsaSubscription(isSuspended,customerIdentification,organisationOrPartnerDetails, individual, address,
      contactDetails, isSameAddress, prevAddress, directorOrPartner, advisor))
}