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

package models.Reads.PsaSubscriptionDetails

import models._
import models.PsaSubscription._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{MustMatchers, OptionValues, WordSpec}


class PsaSubscriptionReadsSpec extends WordSpec with MustMatchers with OptionValues with PsaSubscriptionDetailsGenerators with PropertyChecks {
  "A payload containing details for a PSA subscription" should {
    "map correctly to a PsaSubscription object" when {
      "we have a isSuspended flag " in {
        forAll(psaDetailsGenerator) {
          psa => psa.as[PsaSubscription].isSuspended mustBe (psa \ "psaSubscriptionDetails" \ "isPSASuspension").as[Boolean]
        }
      }

      "we have customer identification details" in {
        forAll(psaDetailsGenerator) {
          psa => psa.as[PsaSubscription].customerIdentification mustBe (psa \ "psaSubscriptionDetails" \ "customerIdentificationDetails").as[CustomerIdentification]
        }
      }

      "we have an optional organisation or partner details" in {
        forAll(psaDetailsGenerator) {
          psa => psa.as[PsaSubscription].organisationOrPartner mustBe (psa \ "psaSubscriptionDetails" \ "organisationOrPartnerDetails").asOpt[OrganisationOrPartner]
        }
      }

      "we have optional individual details" in {
        forAll(psaDetailsGenerator) {
          psa => psa.as[PsaSubscription].individual mustBe (psa \ "psaSubscriptionDetails" \ "individualDetails").asOpt[IndividualDetailType]
        }
      }

      "we have a correspondence address" in {
        forAll(psaDetailsGenerator) {
          psa => psa.as[PsaSubscription].address mustBe (psa \ "psaSubscriptionDetails" \ "correspondenceAddressDetails").as[CorrespondenceAddress]
        }
      }

      "we have a correspondence contact details" in {
        forAll(psaDetailsGenerator) {
          psa => psa.as[PsaSubscription].contact mustBe (psa \ "psaSubscriptionDetails" \ "correspondenceContactDetails").as[PsaContactDetails]
        }
      }

      "we have a flag that tells us if they have lived in the same address for last 12 months" in {
        forAll(psaDetailsGenerator) {
          psa => psa.as[PsaSubscription].isSameAddressForLast12Months mustBe (psa \ "psaSubscriptionDetails" \ "previousAddressDetails" \ "isPreviousAddressLast12Month").as[Boolean]
        }
      }

      "we have an optional previous address" in {
        forAll(psaDetailsGenerator) {
          psa => psa.as[PsaSubscription].previousAddress mustBe (psa \ "psaSubscriptionDetails" \ "previousAddressDetails" \ "previousAddress").asOpt[CorrespondenceAddress]
        }
      }

      "we have an optional list of directors or partners" in {
        forAll(psaDetailsGenerator) {
          psa => psa.as[PsaSubscription].directorsOrPartners mustBe (psa \ "psaSubscriptionDetails" \ "directorOrPartnerDetails").asOpt[Seq[DirectorOrPartner]]
        }
      }

      "we have an option Pension Advisor" in {
        forAll(psaDetailsGenerator) {
          psa => psa.as[PsaSubscription].pensionAdvisor mustBe (psa \ "psaSubscriptionDetails" \ "declarationDetails" \ "pensionAdvisorDetails").asOpt[PensionAdvisor]
        }
      }
    }
  }
}

