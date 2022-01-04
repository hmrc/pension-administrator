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

package models.Writes

import models.{PensionSchemeAdministratorDeclarationType, Samples}
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class DeclarationWritesSpec extends AnyWordSpec with Matchers with OptionValues with Samples {
  "An updated declaration" should {
    "Map correctly to a valid des payload" when {
      val declaration = PensionSchemeAdministratorDeclarationType(true,true,true,true,Some(true),Some(true),true, Some(pensionAdviserSample))

      val result = Json.toJson(declaration)(PensionSchemeAdministratorDeclarationType.psaUpdateWrites)

      "we have box1" in {
        (result \ "box1").as[Boolean] mustBe declaration.box1
      }

      "we have box2" in {
        (result \ "box2").as[Boolean] mustBe declaration.box2
      }

      "we have box3" in {
        (result \ "box3").as[Boolean] mustBe declaration.box3
      }

      "we have box4" in {
        (result \ "box4").as[Boolean] mustBe declaration.box4
      }

      "we have box5" in {
        (result \ "box5").asOpt[Boolean] mustBe declaration.box5
      }

      "we have box6" in {
        (result \ "box6").asOpt[Boolean] mustBe declaration.box6
      }

      "we have box7" in {
        (result \ "box7").as[Boolean] mustBe declaration.box7
      }

      "we have a pension advisor" in {
        (result \ "pensionAdvisorDetails" \ "name").as[String] mustBe declaration.pensionAdvisorDetail.value.name
      }

      "we have an isChanged flag" in {
        val declaration = PensionSchemeAdministratorDeclarationType(true,true,true,true,Some(true),Some(true),true, Some(pensionAdviserSample),Some(true))

        val result = Json.toJson(declaration)(PensionSchemeAdministratorDeclarationType.psaUpdateWrites)

        (result \ "changeFlag").asOpt[Boolean] mustBe declaration.isChanged
      }

      "we don't have an isChanged flag so we default it to false" in {
        (result \ "changeFlag").as[Boolean] mustBe false
      }
    }
  }
}
