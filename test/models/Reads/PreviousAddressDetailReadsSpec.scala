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

package models.Reads

import models.{InternationalAddress, PreviousAddressDetails, Samples, UkAddress}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsString, Json}

class PreviousAddressDetailReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "JSON payload with previous address details" should {
    "Map to a valid previousAddressDetails payload correctly" when {
      "we have a companyAddressYears flag as true" in {
        val input = Json.obj("companyAddressYears" -> JsString("under_a_year"))

        val result = input.as[PreviousAddressDetails](PreviousAddressDetails.apiReads("company"))

        result.isPreviousAddressLast12Month mustBe true
      }

      "we have a companyAddressYears flag as false" in {
        val input = Json.obj("companyAddressYears" -> JsString("over_a_year"))

        val result = input.as[PreviousAddressDetails](PreviousAddressDetails.apiReads("company"))

        result.isPreviousAddressLast12Month mustBe false
      }

      "we have a GB address" in {
        val input = Json.obj("companyAddressYears" -> JsString("under_a_year"),
          "companyPreviousAddress" -> Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"),
          "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"), "postcode" -> JsString("NE1"), "country" -> JsString("GB")))

        val result = input.as[PreviousAddressDetails](PreviousAddressDetails.apiReads("company"))

        result.previousAddressDetails.value.asInstanceOf[UkAddress].countryCode mustBe ukAddressSample.countryCode
      }

      "we have a non UK address with no postcode" in {
        val input = Json.obj("companyAddressYears" -> JsString("under_a_year"),
          "companyPreviousAddress" -> Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"),
          "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"), "country" -> JsString("IT")))

        val result = input.as[PreviousAddressDetails](PreviousAddressDetails.apiReads("company"))

        result.previousAddressDetails.value.asInstanceOf[InternationalAddress].postalCode mustBe None
      }
    }
  }
}
