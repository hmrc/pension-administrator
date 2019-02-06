/*
 * Copyright 2019 HM Revenue & Customs
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

import models.NumberOfDirectorOrPartnersType
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json

class NumberOfDirectorOrPartnersTypeWriteSpec extends WordSpec with MustMatchers with OptionValues {
  "A NumberOfDirectorOrPartnersType object" should {
    "Correclty serialize to a valid DES payload" when {
      val numberOfDirectors = NumberOfDirectorOrPartnersType(Some(true),Some(false))

      val result = Json.toJson(numberOfDirectors)(NumberOfDirectorOrPartnersType.psaUpdateWrites)
      "we have isMorethanTenDirectors flag" in {
        (result \ "isMoreThanTenDirectors").asOpt[Boolean] mustBe numberOfDirectors.isMorethanTenDirectors
      }

      "we have isMoreThanTenPartners flag" in {
        (result \ "isMoreThanTenPartners").asOpt[Boolean] mustBe numberOfDirectors.isMorethanTenPartners
      }

      "we have isChanged flag" in {
        val numberOfDirectors = NumberOfDirectorOrPartnersType(Some(true),Some(false),Some(true))

        val result = Json.toJson(numberOfDirectors)(NumberOfDirectorOrPartnersType.psaUpdateWrites)

        (result \ "changeFlag").asOpt[Boolean] mustBe numberOfDirectors.isChanged
      }

      "we don't have the isChanged flag so we set it to false" in {
        (result \ "changeFlag").asOpt[Boolean] mustBe Some(false)
      }
    }
  }
}
