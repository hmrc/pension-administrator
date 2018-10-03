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

import models.PsaSubscription.{CorrespondenceDetails, PsaContactDetails}
import models.{CorrespondenceAddress, Samples}
import org.scalacheck.Gen
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._
import play.api.libs.functional.syntax._

class CorrespondenceDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {

  "A payload containing correspondence details" should {
    "map correclty to a CorrespondenceDetails object" when {

      val contactDetails = Json.obj("telephone" -> Gen.numStr.sample,
        "email" -> Gen.option(Gen.alphaStr).sample)

      val address = Json.obj("nonUKAddress" -> Gen.oneOf(JsBoolean(true),JsBoolean(false)).sample,
        "line1" -> Gen.alphaStr.sample,
        "line2" -> Gen.alphaStr.sample,
        "line3" -> Gen.option(Gen.alphaStr.sample).sample,
        "line4" -> Gen.option(Gen.alphaStr.sample).sample,
        "postalCode" -> Gen.option(Gen.alphaStr.sample).sample,
        "countryCode" -> Gen.alphaStr.sample)

      val input = Json.obj("addressDetails" -> address,
      "contactDetails" -> Gen.option(contactDetails).sample)

      val output = input.as[CorrespondenceDetails]

      "we have an address" in {
        output.address mustBe (input \ "addressDetails").as[CorrespondenceAddress]
      }

      "we have an optional contact details" in {
        output.contactDetails mustBe (input \ "contactDetails").asOpt[PsaContactDetails]
      }
    }
  }
}


