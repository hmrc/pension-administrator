/*
 * Copyright 2022 HM Revenue & Customs
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

import models.CustomerIdentification
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._


class CustomerIdentificationReadsSpec extends AnyWordSpec with Matchers with OptionValues with PsaSubscriptionDetailsGenerators {
  "A valid payload with Customer Identification Details" should {
    "validate to a Customer Identification Details object" when {
      "we have a legal status" in {
        forAll(customerIdentificationDetailsGenerator) {
          customerDetails => customerDetails.as[CustomerIdentification].legalStatus mustBe (customerDetails \ "legalStatus").as[String]
        }
      }

      "we have an optional id type" in {
        forAll(customerIdentificationDetailsGenerator) {
          customerDetails => customerDetails.as[CustomerIdentification].typeOfId mustBe (customerDetails \ "idType").asOpt[String]
        }
      }

      "we have an optional id number" in {
        forAll(customerIdentificationDetailsGenerator) {
          customerDetails => customerDetails.as[CustomerIdentification].number mustBe (customerDetails \ "idNumber").asOpt[String]
        }
      }

      "we have a flag for noIdentifier" in {
        forAll(customerIdentificationDetailsGenerator) {
          customerDetails => customerDetails.as[CustomerIdentification].isOverseasCustomer mustBe (customerDetails \ "noIdentifier").as[Boolean]
        }
      }
    }
  }
}


