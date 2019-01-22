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

package models.Reads.getPsaDetails

import models.Reads.getPsaDetails.CustomerIdentificationDetailsTypeTransformationSpec.updateJson
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.JsonTransformations.PSASubscriptionDetailsTransformer


class AddressTransformationSpec extends WordSpec with MustMatchers with OptionValues {
  "A DES payload containing an address" must {
    lazy val transformedJson = desAddress.transform(PSASubscriptionDetailsTransformer.getAddress(__ \ 'testAddress, __ \ 'testAddressFromDes)).asOpt.value

    "Map correctly to a valid address" when {
      "We have line1" in {
        (transformedJson \ "testAddress" \ "addressLine1").as[String] mustBe (expectedAddress \ "testAddress" \ "addressLine1").as[String]
      }

      "We have line2" in {
        (transformedJson \ "testAddress" \ "addressLine2").as[String] mustBe (expectedAddress \ "testAddress" \ "addressLine2").as[String]
      }

      "We have line3" in {
        val inputJson = desAddress.transform(updateJson(__ \ 'testAddressFromDes,"line3","York")).asOpt.value

        val transformedJson = inputJson.transform(PSASubscriptionDetailsTransformer.getAddress(__ \ 'testAddress, __ \ 'testAddressFromDes)).asOpt.value

        (transformedJson \ "testAddress" \ "addressLine3").as[String] mustBe (expectedAddress \ "testAddress" \ "addressLine3").as[String]
      }

      "We don't have address line 3" in {
        (transformedJson \ "testAddress" \ "addressLine3").asOpt[String] mustBe None
      }

      "We have line4" in {
        val inputJson = desAddress.transform(updateJson(__ \ 'testAddressFromDes,"line4","Yorkshire")).asOpt.value

        val transformedJson = inputJson.transform(PSASubscriptionDetailsTransformer.getAddress(__ \ 'testAddress, __ \ 'testAddressFromDes)).asOpt.value

        (transformedJson \ "testAddress" \ "addressLine4").as[String] mustBe (expectedAddress \ "testAddress" \ "addressLine4").as[String]
      }

      "We don't have address line 4" in {
        (transformedJson \ "testAddress" \ "addressLine4").asOpt[String] mustBe None
      }

      "We have postal code" in {
        val inputJson = desAddress.transform(updateJson(__ \ 'testAddressFromDes,"postalCode","YO1 9EX")).asOpt.value

        val transformedJson = inputJson.transform(PSASubscriptionDetailsTransformer.getAddress(__ \ 'testAddress, __ \ 'testAddressFromDes)).asOpt.value

        (transformedJson \ "testAddress" \ "postalCode").as[String] mustBe (expectedAddress \ "testAddress" \ "postalCode").as[String]
      }

      "We don't have a postal code" in {
        (transformedJson \ "testAddress" \ "postalCode").asOpt[String] mustBe None
      }

      "We have a contry code" in {
        (transformedJson \ "testAddress" \ "countryCode").as[String] mustBe (expectedAddress \ "testAddress" \ "countryCode").as[String]
      }
    }
  }


  val expectedAddress = Json.parse("""{
                                                "testAddress" : {
                                                    "addressLine1" : "1 Director Road",
                                                    "addressLine2" : "Clifton",
                                                    "addressLine3" : "York",
                                                    "addressLine4" : "Yorkshire",
                                                    "postalCode" : "YO1 9EX",
                                                    "countryCode" : "ES"
                                                }
                                            }""")

  val desAddress = Json.parse("""{
                                          "testAddressFromDes" : {
                                              "nonUKAddress":false,
                                              "line1":"1 Director Road",
                                              "line2":"Clifton",
                                              "countryCode":"ES"
                                          }
                                        }""")

}
