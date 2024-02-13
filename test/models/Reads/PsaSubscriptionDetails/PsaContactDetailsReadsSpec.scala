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

package models.Reads.PsaSubscriptionDetails

import models.PsaContactDetails
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PsaContactDetailsReadsSpec extends AnyWordSpec with Matchers with OptionValues with PsaSubscriptionDetailsGenerators {
  "A payload containing psa contact details" should {
    "parse to a valid tactDetails object" when {
      "we have a telephone number" in {
        forAll(psaContactDetailsGenerator){
          contact => contact.as[PsaContactDetails].telephone mustBe (contact \ "telephone").as[String]
        }
      }

      "we have an optional email" in {
        forAll(psaContactDetailsGenerator){
          contact => contact.as[PsaContactDetails].email mustBe (contact \ "email").asOpt[String]
        }
      }
    }
  }
}
