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

package models.Reads

import models.{Samples, Reads as _, *}
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

class PensionAdvisorDetailsReadsSpec extends AnyWordSpec with Matchers with OptionValues with Samples {

  "A JSON Payload containing a pension adviser detail" should {
    "Map correctly to a valid representation of a PensionAdvisorDetail" when {
      val input = Json.obj("adviserName" -> JsString("John"), "adviserPhone" -> "07592113", "adviserEmail" -> "test@test.com",
        "adviserAddress" -> Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"),
          "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"),
          "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB")))

      "We have a name" in {
        val result = input.as[Option[PensionAdvisorDetail]](using PensionAdvisorDetail.apiReads)

        result.value.name.mustBe(pensionAdviserSample.name)
      }

      "We have an address" in {
        val result = input.as[Option[PensionAdvisorDetail]](using PensionAdvisorDetail.apiReads)

        result.value.addressDetail.mustBe(pensionAdviserSample.addressDetail)
      }

      "We have adviser contact details" in {
        val result = input.as[Option[PensionAdvisorDetail]](using PensionAdvisorDetail.apiReads)

        result.value.contactDetail.mustBe(pensionAdviserSample.contactDetail)
      }
    }
  }
}
