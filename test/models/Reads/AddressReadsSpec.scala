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

package models.Reads

import models.{Address, InternationalAddress, Samples, UkAddress}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class AddressReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "A JSON Payload with an address" should {
    "Map correctly to an Address type" when {

      val address = Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"),
        "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))

      "We have common address elements" when {
        "with addressLine 1" in {
          val result = address.as[(String, Option[String], Option[String], Option[String], String)](Address.commonAddressElementsReads)

          result._1 mustBe ukAddressSample.addressLine1
        }

        "with addressLine 2" in {
          val result = address.as[(String, Option[String], Option[String], Option[String], String)](Address.commonAddressElementsReads)

          result._2 mustBe ukAddressSample.addressLine2
        }

        "with addressLine 3" in {
          val result = address.as[(String, Option[String], Option[String], Option[String], String)](Address.commonAddressElementsReads)

          result._3 mustBe ukAddressSample.addressLine3
        }

        "with addressLine 4" in {
          val result = address.as[(String, Option[String], Option[String], Option[String], String)](Address.commonAddressElementsReads)

          result._4 mustBe ukAddressSample.addressLine4
        }

        "with countryCode" in {
          val result = address.as[(String, Option[String], Option[String], Option[String], String)](Address.commonAddressElementsReads)

          result._5 mustBe ukAddressSample.countryCode
        }

        "with a countryCode defined as `country`" in {
          val result = (address - "countryCode" + ("country" -> JsString("GB"))).as[(String, Option[String], Option[String], Option[String], String)](Address.commonAddressElementsReads)

          result._5 mustBe ukAddressSample.countryCode
        }
      }

      "we have a UK address" when {
        "with postal code" in {
          val result = address.as[Address]

          result.asInstanceOf[UkAddress].postalCode mustBe ukAddressSample.postalCode
        }

        "with postal code defined as `postcode`" in {
          val result = (address - "postalCode" + ("postcode" -> JsString("NE1"))).as[Address]

          result.asInstanceOf[UkAddress].postalCode mustBe ukAddressSample.postalCode
        }
      }

      "we have a non UK address" when {
        val address = Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"), "countryCode" -> JsString("IT"))

        "with no postal code" in {
          val result = address.as[Address]

          result.asInstanceOf[InternationalAddress].postalCode mustBe None
        }

        "with postal code" in {
          val input = (address + ("postalCode" -> JsString("NE1")))

          val result = input.as[Address]

          result.asInstanceOf[InternationalAddress].postalCode mustBe nonUkAddressSample.postalCode
        }

        "with postal code defined as `postcode`" in {
          val input = (address + ("postcode" -> JsString("NE1")))

          val result = input.as[Address]

          result.asInstanceOf[InternationalAddress].postalCode mustBe nonUkAddressSample.postalCode
        }

        "with territory defined as country code" in {
          val input = (address + ("countryCode" -> JsString("territory:IT")))

          val result = input.as[Address]

          result.asInstanceOf[InternationalAddress].countryCode mustBe nonUkAddressSample.countryCode
        }

        "with territory defined as country code with leading space" in {
          val input = (address + ("countryCode" -> JsString("territory: IT")))

          val result = input.as[Address]

          result.asInstanceOf[InternationalAddress].countryCode mustBe nonUkAddressSample.countryCode
        }
      }
    }
  }
}
