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

import models.CorrespondenceAddress
import models.PsaSubscription.{CorrespondenceDetails, DirectorOrPartner}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class DirectorOrPartnerReadsSpec extends WordSpec with MustMatchers with OptionValues with PsaSubscriptionDetailsGenerators {
  "A payload containing details for a Director or a Partner" should {
    "parse correctly to a PsaDirectorOrPartnerDetails object" when {
      val output = psaDirectorOrPartnerDetailsGenerator.as[DirectorOrPartner]

      "we have an entity type" in {
        output.isDirectorOrPartner mustBe (psaDirectorOrPartnerDetailsGenerator \ "entityType").as[String]
      }

      "we have an optional title" in {
        output.title mustBe (psaDirectorOrPartnerDetailsGenerator \ "title").asOpt[String]
      }

      "we have a first name" in {
        output.firstName mustBe (psaDirectorOrPartnerDetailsGenerator \ "firstName").as[String]
      }

      "we have an optional middle name" in {
        output.middleName mustBe (psaDirectorOrPartnerDetailsGenerator \ "middleName").asOpt[String]
      }

      "we have a surname" in {
        output.lastName mustBe (psaDirectorOrPartnerDetailsGenerator \ "lastName").as[String]
      }

      "we have a dob" in {
        output.dateOfBirth.toString() mustBe (psaDirectorOrPartnerDetailsGenerator \ "dateOfBirth").as[String]
      }

      "we have an optional nino" in {
        output.nino mustBe (psaDirectorOrPartnerDetailsGenerator \ "nino").asOpt[String]
      }

      "we have an optional utr" in {
        output.utr mustBe (psaDirectorOrPartnerDetailsGenerator \ "utr").asOpt[String]
      }

      "we have a flag to say whether if they have been in the same prevoius address in last 12 months" in {
        output.isSameAddressForLast12Months mustBe (psaDirectorOrPartnerDetailsGenerator \ "previousAddressDetails" \ "isPreviousAddressLast12Month").as[Boolean]
      }

      "we have an optional previous address" in {
        output.previousAddress mustBe (psaDirectorOrPartnerDetailsGenerator \ "previousAddressDetails" \ "previousAddress").asOpt[CorrespondenceAddress]
      }

      "we have an optional correspondence common details" in {
        output.correspondenceDetails mustBe (psaDirectorOrPartnerDetailsGenerator \ "correspondenceCommonDetails").asOpt[CorrespondenceDetails]
      }
    }
  }
}


