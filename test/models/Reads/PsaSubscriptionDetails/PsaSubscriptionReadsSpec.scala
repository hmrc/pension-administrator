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

package models.Reads.PsaSubscriptionDetails

import models.{ContactDetails, CorrespondenceAddress, IndividualDetailType}
import models.PsaSubscription._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsPath, Json, Reads, Writes}
import play.api.libs.functional.syntax._

class PsaSubscriptionReadsSpec extends WordSpec with MustMatchers with OptionValues with PsaSubscriptionDetailsGenerators with PropertyChecks {
  "A payload containing details for a PSA subscription" should {
    "map correctly to a PsaSubscription object" when {

      val output = psaSubscriptionDetailsGenerator.as[PsaSubscription]

      "we have a isSuspended flag" in {
        output.isSuspended mustBe (psaSubscriptionDetailsGenerator \ "isPSASuspension").as[Boolean]
      }

      "we have customer identification details" in {
        output.customerIdentification mustBe (psaSubscriptionDetailsGenerator \ "customerIdentificationDetails").as[CustomerIdentification]
      }

      "we have an optional organisation or partner details" in {
        output.organisationOrPartner mustBe (psaSubscriptionDetailsGenerator \ "organisationOrPartnerDetails").asOpt[OrganisationOrPartner]
      }

      "we have optional individual details" in {
        output.individual mustBe (psaSubscriptionDetailsGenerator \ "individualDetails").asOpt[IndividualDetailType]
      }

      "we have a correspondence address" in {
        output.address mustBe (psaSubscriptionDetailsGenerator \ "correspondenceAddressDetails").as[CorrespondenceAddress]
      }

      "we have a correspondence contact details" in {
        output.contact mustBe (psaSubscriptionDetailsGenerator \ "correspondenceContactDetails").as[PsaContactDetails]
      }

      "we have a flag that tells us if they have lived in the same address for last 12 months" in {
        output.isSameAddressForLast12Months mustBe (psaSubscriptionDetailsGenerator \ "previousAddressDetails" \ "isPreviousAddressLast12Month").as[Boolean]
      }

      "we have an optional previous address" in {
        output.previousAddress mustBe (psaSubscriptionDetailsGenerator \ "previousAddressDetails" \ "previousAddress").asOpt[CorrespondenceAddress]
      }

      "we have an optional list of directors or partners" in {
        output.directorsOrPartners mustBe (psaSubscriptionDetailsGenerator \ "directorOrPartnerDetails").asOpt[Seq[DirectorOrPartner]]
      }

      "we have an option Pension Advisor" in {
        output.pensionAdvisor mustBe (psaSubscriptionDetailsGenerator \ "declarationDetails" \ "pensionAdvisorDetails").asOpt[PensionAdvisor]
      }
    }
  }
}

case class PsaSubscription(isSuspended: Boolean, customerIdentification: CustomerIdentification,
                           organisationOrPartner: Option[OrganisationOrPartner], individual: Option[IndividualDetailType], address: CorrespondenceAddress, contact: PsaContactDetails,
                           isSameAddressForLast12Months: Boolean, previousAddress: Option[CorrespondenceAddress], directorsOrPartners: Option[Seq[DirectorOrPartner]],
                           pensionAdvisor: Option[PensionAdvisor])

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
  )((isSuspended,customerIdentification, organisationOrPartnerDetails, individual, address, contactDetails, isSameAddress, prevAddress, directorOrPartner, advisor) =>
    PsaSubscription(isSuspended,customerIdentification,organisationOrPartnerDetails, individual, address, contactDetails, isSameAddress, prevAddress, directorOrPartner, advisor))
}
