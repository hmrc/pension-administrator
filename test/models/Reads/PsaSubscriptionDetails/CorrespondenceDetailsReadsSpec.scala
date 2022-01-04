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

package models.Reads.PsaSubscriptionDetails

import models.{CorrespondenceAddress, CorrespondenceDetails, PsaContactDetails}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec


class CorrespondenceDetailsReadsSpec extends AnyWordSpec with Matchers with OptionValues with PsaSubscriptionDetailsGenerators {

  "A payload containing correspondence details" should {
    "map correctly to a CorrespondenceDetails object" when {
      "we have an address" in {
        forAll(correspondenceDetailsGenerator) {
          correspondence =>
            correspondence.as[CorrespondenceDetails].address mustBe
              (correspondence \ "addressDetails").as[CorrespondenceAddress]
        }
      }

      "we have an optional contact details" in {
        forAll(correspondenceDetailsGenerator) {
          correspondence =>
            correspondence.as[CorrespondenceDetails].contactDetails mustBe
              (correspondence \ "contactDetails").asOpt[PsaContactDetails]
        }
      }
    }
  }
}


