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

class OrganisationOrPartnerDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "A valid payload with Organisation or Partner Details" should {
    "validate to a Organisation or Partner Details object" when {

      val customerIdentificationDetails  = Json.obj("name" -> Gen.alphaStr.sample,
        "crnNumber" -> Gen.option(Gen.alphaStr).sample,
        "vatRegistrationNumber" -> Gen.option(Gen.alphaStr).sample,
        "payeReference" -> Gen.option(Gen.alphaStr).sample)

      val result = customerIdentificationDetails.as[OrganisationOrPartnerDetails]

      "we have a name" in {
        result.name mustBe (customerIdentificationDetails \ "name").as[String]
      }

      "we have an optional crnNumber" in {
        result.crn mustBe (customerIdentificationDetails \ "crnNumber").asOpt[String]
      }

      "we have an optional vatRegistrationNumber" in {
        result.vatRegistration mustBe (customerIdentificationDetails \ "vatRegistrationNumber").asOpt[String]
      }

      "we have an optional payeReference" in {
        result.paye mustBe (customerIdentificationDetails \ "payeReference").asOpt[String]
      }
    }
  }
}