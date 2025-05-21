/*
 * Copyright 2025 HM Revenue & Customs
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

import models.{Address, InternationalAddress, UkAddress}
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class AddressWritesSpec extends AnyWordSpec with Matchers with OptionValues {

  "An updated address using updateWrites" should {
    "parse correctly to a valid DES format" when {
      "we have a UK address" when {
        val address = UkAddress("line1", Some("line2"), Some("line3"), Some("line4"), "GB", "Test")
        val result = Json.toJson(address.asInstanceOf[Address])(using Address.updateWrites)

        "with address line 1 " in {
          result.toString() must include("line1")
        }

        "with address line 2" in {
          result.toString() must include("line2")
        }

        "with address line 3" in {
          result.toString() must include("line3")
        }

        "with address line 4" in {
          result.toString() must include("line4")
        }

        "with countrycode" in {
          result.toString() must include("countryCode")
        }

        "with postalcode" in {
          result.toString() must include("postalCode")
        }

        "we have a nonUkAddress flag" in {
          (result \ "nonUKAddress").as[Boolean] mustBe false
        }

        "with an isUpdated flag" in {
          val address = UkAddress("line1", Some("line2"), Some("line3"), Some("line4"), "GB", "Test", isChanged = Some(true))

          val result = Json.toJson(address.asInstanceOf[Address])(using Address.updateWrites)

          (result \ "changeFlag").asOpt[Boolean].value mustBe true
        }

        "without an isUpdated flag" in {
          val address = UkAddress("line1", Some("line2"), Some("line3"), Some("line4"), "GB", "Test", isChanged = None)

          val result = Json.toJson(address.asInstanceOf[Address])(using Address.updateWrites)

          (result \ "changeFlag").asOpt[Boolean] mustBe None
        }
      }

      "we have an International address" when {
        val address = InternationalAddress("line1", Some("line2"), Some("line3"), Some("line4"), "IT", Some("test"))
        val result = Json.toJson(address.asInstanceOf[Address])(using Address.updateWrites)

        "with address line 1" in {
          result.toString() must include("line1")
        }

        "with address line 2" in {
          result.toString() must include("line2")
        }

        "with address line 3" in {
          result.toString() must include("line3")
        }

        "with address line 4" in {
          result.toString() must include("line4")
        }

        "with countrycode" in {
          result.toString() must include("countryCode")
        }

        "with postalcode" in {
          result.toString() must include("postalCode")
        }

        "we have a nonUkAddress flag" in {
          (result \ "nonUKAddress").as[Boolean] mustBe true
        }

        "with an isUpdated flag" in {
          val address = InternationalAddress("line1", Some("line2"), Some("line3"), Some("line4"), "IT", Some("test"), isChanged = Some(true))

          val result = Json.toJson(address.asInstanceOf[Address])(using Address.updateWrites)

          (result \ "changeFlag").asOpt[Boolean].value mustBe true
        }

        "without an isUpdated flag" in {
          val address = InternationalAddress("line1", Some("line2"), Some("line3"), Some("line4"), "IT", Some("test"), isChanged = None)

          val result = Json.toJson(address.asInstanceOf[Address])(using Address.updateWrites)

          (result \ "changeFlag").asOpt[Boolean] mustBe None
        }
      }
    }
  }

  "An updated address" should {
    "parse correctly to a valid DES format using updatePreviousAddressWrites" when {
      "we have a UK address" when {
        val address = UkAddress("line1", Some("line2"), Some("line3"), Some("line4"), "GB", "Test")
        val result = Json.toJson(address.asInstanceOf[Address])(using Address.updatePreviousAddressWrites)

        "with address line 1 " in {
          result.toString() must include("line1")
        }

        "with address line 2" in {
          result.toString() must include("line2")
        }

        "with address line 3" in {
          result.toString() must include("line3")
        }

        "with address line 4" in {
          result.toString() must include("line4")
        }

        "with countrycode" in {
          result.toString() must include("countryCode")
        }

        "with postalcode" in {
          result.toString() must include("postalCode")
        }

        "we have a nonUkAddress flag" in {
          (result \ "nonUKAddress").as[Boolean] mustBe false
        }

        "we have no isUpdated flag" in {
          val address = UkAddress("line1", Some("line2"), Some("line3"), Some("line4"), "GB", "Test", isChanged = Some(true))

          val result = Json.toJson(address.asInstanceOf[Address])(using Address.updatePreviousAddressWrites)

          (result \ "changeFlag").isDefined mustBe false
        }
      }

      "we have an International address" when {
        val address = InternationalAddress("line1", Some("line2"), Some("line3"), Some("line4"), "IT", Some("test"))
        val result = Json.toJson(address.asInstanceOf[Address])(using Address.updatePreviousAddressWrites)

        "with address line 1" in {
          result.toString() must include("line1")
        }

        "with address line 2" in {
          result.toString() must include("line2")
        }

        "with address line 3" in {
          result.toString() must include("line3")
        }

        "with address line 4" in {
          result.toString() must include("line4")
        }

        "with countrycode" in {
          result.toString() must include("countryCode")
        }

        "with postalcode" in {
          result.toString() must include("postalCode")
        }

        "we have a nonUkAddress flag" in {
          (result \ "nonUKAddress").as[Boolean] mustBe true
        }

        "we have no isUpdated flag" in {
          val address = InternationalAddress("line1", Some("line2"), Some("line3"), Some("line4"), "GB", Some("Test"), isChanged = Some(true))

          val result = Json.toJson(address.asInstanceOf[Address])(using Address.updatePreviousAddressWrites)

          (result \ "changeFlag").isDefined mustBe false
        }
      }
    }
  }

  "An address" should {
    "parse correctly to a valid DES format" when {
      "we have a UK address" when {
        val address = UkAddress("line1", Some("line2"), Some("line3"), Some("line4"), "GB", "Test")
        val result = Json.toJson(address.asInstanceOf[Address])

        "with address line 1" in {
          result.toString() must include("line1")
        }

        "with address line 2" in {
          result.toString() must include("line2")
        }

        "with address line 3" in {
          result.toString() must include("line3")
        }

        "with address line 4" in {
          result.toString() must include("line4")
        }

        "with countrycode" in {
          result.toString() must include("countryCode")
        }

        "with postalcode" in {
          result.toString() must include("postalCode")
        }

        "with an address type of UK" in {
          result.toString() must include("\"addressType\":\"UK\"")
        }
      }

      "we have an International address" when {
        val address = InternationalAddress("line1", Some("line2"), Some("line3"), Some("line4"), "IT", Some("test"))
        val result = Json.toJson(address.asInstanceOf[Address])

        "with address line 1" in {
          result.toString() must include("line1")
        }

        "with address line 2" in {
          result.toString() must include("line2")
        }

        "with address line 3" in {
          result.toString() must include("line3")
        }

        "with address line 4" in {
          result.toString() must include("line4")
        }

        "with countrycode" in {
          result.toString() must include("countryCode")
        }

        "with postalcode" in {
          result.toString() must include("postalCode")
        }

        "with an address type of Non-UK" in {
          result.toString() must include("\"addressType\":\"NON-UK\"")
        }
      }
    }
  }
}
