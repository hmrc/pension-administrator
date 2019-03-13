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

package models.Writes

import models.{Address, PreviousAddressDetails, Samples, UkAddress}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json

class PreviousAddressDetailsWritesSpec extends WordSpec with MustMatchers with OptionValues with Samples {

  "A previous address details object" should {
    "Map previosaddressdetails inner object as `previousaddresdetail`" when {
      "required" in {
        val previousAddress = PreviousAddressDetails(true, Some(ukAddressSample))
        val result = Json.toJson(previousAddress)(PreviousAddressDetails.psaSubmissionWrites)

        result.toString() must include("\"previousAddressDetail\":")
      }
    }
  }

  "An updated previous address details object" should {
    "serialize correctly into a valid DES payload" when {
      val previousAddress = PreviousAddressDetails(isPreviousAddressLast12Month = true, Some(ukAddressSample))
      val result = Json.toJson(previousAddress)(PreviousAddressDetails.psaUpdateWrites)

      "we have an isPreviousAddressLast12Months flag" in {
        (result \ "isPreviousAddressLast12Month").as[Boolean] mustBe previousAddress.isPreviousAddressLast12Month
      }

      "we have a previous address" in {
        (result \ "previousAddressDetails" \ "line1").as[String] mustBe previousAddress.previousAddressDetails.value.asInstanceOf[UkAddress].addressLine1
      }

      "we have an isChanged flag" in {
        val previousAddress = PreviousAddressDetails(isPreviousAddressLast12Month = true, Some(ukAddressSample.copy(isChanged = Some(true))), Some(true))
        val result = Json.toJson(previousAddress)(PreviousAddressDetails.psaUpdateWrites)

        (result \ "changeFlag").asOpt[Boolean] mustBe Some(true)
      }

      "we don't require isChanged flag" in {
        val previousAddress = PreviousAddressDetails(isPreviousAddressLast12Month = true, Some(ukAddressSample), Some(true))
        val result = Json.toJson(previousAddress)(PreviousAddressDetails.psaUpdateWritesWithNoUpdateFlag)

        (result \ "changeFlag").asOpt[Boolean] mustBe None
      }
    }
  }
}
