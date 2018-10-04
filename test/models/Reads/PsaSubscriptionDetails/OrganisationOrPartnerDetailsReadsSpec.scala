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

import models.PsaSubscription.OrganisationOrPartnerDetails
import models.Samples
import org.scalacheck.Gen
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class OrganisationOrPartnerDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with PsaSubscriptionDetailsGenerators {
  "A valid payload with Organisation or Partner Details" should {
    "validate to a Organisation or Partner Details object" when {
      val result = orgOrPartnerDetailsGenerator.as[OrganisationOrPartnerDetails]

      "we have a name" in {
        result.name mustBe (orgOrPartnerDetailsGenerator \ "name").as[String]
      }

      "we have an optional crnNumber" in {
        result.crn mustBe (orgOrPartnerDetailsGenerator \ "crnNumber").asOpt[String]
      }

      "we have an optional vatRegistrationNumber" in {
        result.vatRegistration mustBe (orgOrPartnerDetailsGenerator \ "vatRegistrationNumber").asOpt[String]
      }

      "we have an optional payeReference" in {
        result.paye mustBe (orgOrPartnerDetailsGenerator \ "payeReference").asOpt[String]
      }
    }
  }
}