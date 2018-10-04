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

import models.{IndividualDetailType, Samples}
import org.joda.time.LocalDate
import org.scalacheck.Gen
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json


class IndividualDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with PsaSubscriptionDetailsGenerators {
  "A psa subscription details payload containing individual details" should {
    "parse the individual details into an Individual Details object" when {
      val result = individualGenerator.as[IndividualDetailType]

      "we have an optional title" in {
        result.title mustBe (individualGenerator \ "title").asOpt[String]
      }

      "we have a first name" in {
        result.firstName mustBe (individualGenerator \ "firstName").as[String]
      }

      "we have an optional middle name" in {
        result.middleName mustBe (individualGenerator \ "middleName").asOpt[String]
      }

      "we have a last name" in {
        result.lastName mustBe (individualGenerator \ "lastName").as[String]
      }

      "we have a date of birth" in {
        result.dateOfBirth.toString mustBe (individualGenerator \ "dateOfBirth").as[String]
      }
    }
  }

}
