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

import models.PsaSubscription.CustomerIdentificationDetails
import models.Samples
import org.scalacheck.Gen
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class CustomerIdentificationDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "A valid payload with Customer Identification Details" should {
    "validate to a Customer Identification Details object" when {

      val legalStatus = Gen.oneOf("Individual","Partnership","Limited Company")
      val idType = Gen.option(Gen.oneOf("NINO","UTR"))

      val customerIdentificationDetails  = Json.obj("legalStatus" -> legalStatus.sample,
        "idType" -> idType.sample,
        "idNumber" -> Gen.option(Gen.alphaStr).sample,
        "noIdentifier" -> Gen.oneOf(true,false).sample)

      val result = customerIdentificationDetails.as[CustomerIdentificationDetails]

      "we have a legal status" in {
        result.legalStatus mustBe (customerIdentificationDetails \ "legalStatus").as[String]
      }

      "we have an optional id type" in {
        result.typeOfId mustBe (customerIdentificationDetails \ "idType").asOpt[String]
      }

      "we have an optional id number" in {
        result.number mustBe (customerIdentificationDetails \ "idNumber").asOpt[String]
      }

      "we have a flag for noIdentifier" in {
        result.isOverseasCustomer mustBe (customerIdentificationDetails \ "noIdentifier").as[Boolean]
      }
    }
  }
}


