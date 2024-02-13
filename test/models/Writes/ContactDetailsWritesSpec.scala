/*
 * Copyright 2024 HM Revenue & Customs
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

import models.ContactDetails
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class ContactDetailsWritesSpec extends AnyWordSpec with Matchers with OptionValues {
  "A contact details object" should {
    "serialize correctly to a valid des payload" when {
      val contactDetails = ContactDetails("16342346",None,None,"test@test.com")
      val result = Json.toJson(contactDetails)(ContactDetails.updateWrites)

      "we have a phone" in {
        (result \ "telephone").as[String] mustBe contactDetails.telephone
      }

      "we have an email" in {
        (result \ "email").as[String] mustBe contactDetails.email
      }

      "we don't have a change flag so we set to false" in {
        (result \ "changeFlag").as[Boolean] mustBe false
      }

      "we have a change flag" in {
        val contactDetails = ContactDetails("16342346",None,None,"test@test.com",Some(true))
        val result = Json.toJson(contactDetails)(ContactDetails.updateWrites)

        (result \ "changeFlag").as[Boolean] mustBe contactDetails.isChanged.value
      }
    }
  }
}
