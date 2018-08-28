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

package models.Reads.Writes

import models.{Address, InternationalAddress, UkAddress}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json

class AddressWritesSpec extends WordSpec with MustMatchers with OptionValues {

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

    "Replace any ampersands with `and`" when {
      val address = InternationalAddress("line1 & test", Some("line2 & test"), Some("line3 & test"), Some("line4 & test"), "IT", Some("test"))
      val result = Json.toJson(address.asInstanceOf[Address])

      "addressLine1 contains an ampersand" in {
        val line1 = (result \ "line1").as[String]

        line1 mustNot include("&")
        line1 must include("and")
      }

      "addressLine2 contains an ampersand" in {
        val line2 = (result \ "line2").as[String]

        line2 mustNot include("&")
        line2 must include("and")
      }

      "addressLine3 contains an ampersand" in {
        val line3 = (result \ "line3").as[String]

        line3 mustNot include("&")
        line3 must include("and")
      }

      "addressLine4 contains an ampersand" in {
        val line4 = (result \ "line4").as[String]

        line4 mustNot include("&")
        line4 must include("and")
      }
    }
  }
}
