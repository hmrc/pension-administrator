/*
 * Copyright 2022 HM Revenue & Customs
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

import models.ContactDetails
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsBoolean, Json}

class ContactDetailsReadsSpec extends AnyWordSpec with Matchers with OptionValues {

  "A JSON payload containing contact details" should {
    "Map to a valid ContactDetails object" when {
      val input = Json.obj("phone" -> "0758237281", "email" -> "test@test.com")
      "We have a telephone number" in {
        val result: ContactDetails = input.as[ContactDetails](ContactDetails.apiReads)
        result.telephone mustBe "0758237281"
      }

      "We have an email address" in {
        val result: ContactDetails = input.as[ContactDetails](ContactDetails.apiReads)
        result.email mustBe "test@test.com"
      }

      "We have an isChanged flag" in {
        val modifiedContactDetails = input + ("isChanged" -> JsBoolean(true))

        val result = modifiedContactDetails.as[ContactDetails](ContactDetails.apiReads)

        result.isChanged.value mustBe true
      }
    }
  }
}
