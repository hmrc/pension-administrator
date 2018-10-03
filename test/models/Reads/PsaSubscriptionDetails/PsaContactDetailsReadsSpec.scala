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

import models.PsaSubscription.PsaContactDetails
import models.Samples
import org.scalacheck.Gen
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json

class PsaContactDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "A payload containing psa contact details" should {
    "parse to a valid tactDetails object" when {

      val input = Json.obj("telephone" -> Gen.numStr.sample,
      "email" -> Gen.option(Gen.alphaStr).sample)

      val output = input.as[PsaContactDetails]

      "we have a telephone number" in {
        output.telephone mustBe (input \ "telephone").as[String]
      }
      
      "we have an optional email" in {
        output.email mustBe (input \ "email").asOpt[String]
      }
    }
  }
}
