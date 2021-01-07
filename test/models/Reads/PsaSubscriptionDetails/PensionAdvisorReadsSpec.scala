/*
 * Copyright 2021 HM Revenue & Customs
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

import models.{CorrespondenceAddress, PensionAdvisor, PsaContactDetails}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._
import org.scalatest.{MustMatchers, OptionValues, WordSpec}


class PensionAdvisorReadsSpec extends WordSpec with MustMatchers with OptionValues with PsaSubscriptionDetailsGenerators {
  "A payload containing details for a pension advisor" should {
    "Map correctly to a Pension Advisor object" when {
      "We have a name" in {
        forAll(pensionAdvisorGenerator){
          advisor => advisor.as[PensionAdvisor].name mustBe (advisor \ "name").as[String]
        }
      }

      "we have an address" in {
        forAll(pensionAdvisorGenerator){
          advisor => advisor.as[PensionAdvisor].address mustBe (advisor \ "addressDetails").as[CorrespondenceAddress]
        }
      }

      "we have an optional contact details" in {
        forAll(pensionAdvisorGenerator){
          advisor => advisor.as[PensionAdvisor].contactDetails mustBe (advisor \ "contactDetails").asOpt[PsaContactDetails]
        }
      }
    }
  }
}
