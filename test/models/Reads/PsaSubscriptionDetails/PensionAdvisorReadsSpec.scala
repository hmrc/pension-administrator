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
import models.PsaSubscription.{PensionAdvisor, PsaContactDetails}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsPath, Json, Reads, Writes}
import play.api.libs.functional.syntax._

class PensionAdvisorReadsSpec extends WordSpec with MustMatchers with OptionValues with PsaSubscriptionDetailsGenerators {
  "A payload containing details for a pension advisor" should {
    "Map correctly to a Pension Advisor object" when {
      val output = pensionAdvisorGenerator.as[PensionAdvisor]
      
      "We have a name" in {
        output.name mustBe (pensionAdvisorGenerator \ "name").as[String]
      }

      "we have an address" in {
        output.address mustBe (pensionAdvisorGenerator \ "addressDetails").as[CorrespondenceAddress]
      }

      "we have an optional contact details" in {
        output.contactDetails mustBe (pensionAdvisorGenerator \ "contactDetails").asOpt[PsaContactDetails]
      }
    }
  }
}
