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

import models.IndividualDetailType
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec


class IndividualDetailsReadsSpec extends AnyWordSpec with Matchers with OptionValues with PsaSubscriptionDetailsGenerators {
  "A psa subscription details payload containing individual details" should {
    "parse the individual details into an Individual Details object" when {

      "we have an optional title" in {
        forAll(individualGenerator) {
          individual => individual.as[IndividualDetailType].title mustBe (individual \ "title").asOpt[String]
        }
      }

      "we have a first name" in {
        forAll(individualGenerator) {
          individual => individual.as[IndividualDetailType].firstName mustBe (individual \ "firstName").as[String]
        }
      }

      "we have an optional middle name" in {
        forAll(individualGenerator) {
          individual => individual.as[IndividualDetailType].middleName mustBe (individual \ "middleName").asOpt[String]
        }
      }

      "we have a last name" in {
        forAll(individualGenerator) {
          individual => individual.as[IndividualDetailType].lastName mustBe (individual \ "lastName").as[String]
        }
      }

      "we have a date of birth" in {
        forAll(individualGenerator) {
          individual => individual.as[IndividualDetailType].dateOfBirth.toString mustBe (individual \ "dateOfBirth").as[String]
        }
      }
    }
  }

}
