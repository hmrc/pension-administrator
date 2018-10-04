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

import models.PsaSubscription._
import models.{CorrespondenceAddress, IndividualDetailType}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class PsaSubscriptionReadsSpec extends WordSpec with MustMatchers with OptionValues with PsaSubscriptionDetailsGenerators with PropertyChecks {
  "A payload containing details for a PSA subscription" should {
    "map correctly to a PsaSubscription object" when {

      val output = psaSubscriptionDetailsGenerator.as[PsaSubscription]

      "we have a isSuspended flag" in {
        output.isSuspended mustBe (psaSubscriptionDetailsGenerator \ "psaSubscriptionDetails" \ "isPSASuspension").as[Boolean]
      }

      "we have customer identification details" in {
        output.customerIdentification mustBe (psaSubscriptionDetailsGenerator \ "psaSubscriptionDetails" \ "customerIdentificationDetails").as[CustomerIdentification]
      }

      "we have an optional organisation or partner details" in {
        output.organisationOrPartner mustBe (psaSubscriptionDetailsGenerator \ "psaSubscriptionDetails" \ "organisationOrPartnerDetails").asOpt[OrganisationOrPartner]
      }

      "we have optional individual details" in {
        output.individual mustBe (psaSubscriptionDetailsGenerator \ "psaSubscriptionDetails" \ "individualDetails").asOpt[IndividualDetailType]
      }

      "we have a correspondence address" in {
        output.address mustBe (psaSubscriptionDetailsGenerator \ "psaSubscriptionDetails"  \ "correspondenceAddressDetails").as[CorrespondenceAddress]
      }

      "we have a correspondence contact details" in {
        output.contact mustBe (psaSubscriptionDetailsGenerator \ "psaSubscriptionDetails" \ "correspondenceContactDetails").as[PsaContactDetails]
      }

      "we have a flag that tells us if they have lived in the same address for last 12 months" in {
        output.isSameAddressForLast12Months mustBe (psaSubscriptionDetailsGenerator \ "psaSubscriptionDetails" \ "previousAddressDetails" \ "isPreviousAddressLast12Month").as[Boolean]
      }

      "we have an optional previous address" in {
        output.previousAddress mustBe (psaSubscriptionDetailsGenerator \ "psaSubscriptionDetails" \ "previousAddressDetails" \ "previousAddress").asOpt[CorrespondenceAddress]
      }

      "we have an optional list of directors or partners" in {
        output.directorsOrPartners mustBe (psaSubscriptionDetailsGenerator \ "psaSubscriptionDetails" \ "directorOrPartnerDetails").asOpt[Seq[DirectorOrPartner]]
      }

      "we have an option Pension Advisor" in {
        output.pensionAdvisor mustBe (psaSubscriptionDetailsGenerator \ "psaSubscriptionDetails" \ "declarationDetails" \ "pensionAdvisorDetails").asOpt[PensionAdvisor]
      }
    }
  }
}

