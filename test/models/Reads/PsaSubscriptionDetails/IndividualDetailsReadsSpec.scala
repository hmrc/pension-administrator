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


class IndividualDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "A psa subscription details payload containing individual details" should {
    "parse the individual details into an Individual Details object" when {
      
      val date: Gen[LocalDate] = for {
        day <- Gen.choose(1,28)
        month <-Gen.choose(1,12)
        year <-Gen.choose(2000,2018)
      } yield new LocalDate(year,month,day)


      val titles = Gen.oneOf("Mr","Mrs","Miss","Ms","Dr","Sir","Professor","Lord")
      val individual = Json.obj("title" -> Gen.option(titles).sample,
      "firstName" -> Gen.alphaStr.sample,
      "middleName" -> Gen.option(Gen.alphaStr).sample,
      "lastName" -> Gen.alphaStr.sample,
      "dateOfBirth" -> date.sample)

      val result = individual.as[IndividualDetailType]

      "we have an optional title" in {
        result.title mustBe (individual \ "title").asOpt[String]
      }

      "we have a first name" in {
        result.firstName mustBe (individual \ "firstName").as[String]
      }

      "we have an optional middle name" in {
        result.middleName mustBe (individual \ "middleName").asOpt[String]
      }

      "we have a last name" in {
        result.lastName mustBe (individual \ "lastName").as[String]
      }

      "we have a date of birth" in {
        result.dateOfBirth.toString mustBe (individual \ "dateOfBirth").as[String]
      }
    }
  }

}
