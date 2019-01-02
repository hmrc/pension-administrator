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

package models.Reads

import models.Samples
import models.{Reads => _, _}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{Json, _}


class DeclarationTypeReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {

  "A JSON Payload containing a declaration" should {
    "Map correctly a Pension Scheme Administrator Declaration Type" when {

      val declaration = Json.obj("declaration" -> JsBoolean(true), "declarationFitAndProper" -> JsBoolean(true),
        "declarationWorkingKnowledge" -> JsString("workingKnowledge"))

      "We have a declaration field" when {
        "It is true then boxes 1,2,3 and 4 are true" in {
          val result = declaration.as[PensionSchemeAdministratorDeclarationType](PensionSchemeAdministratorDeclarationType.apiReads)

          result.box1 mustBe true
          result.box2 mustBe true
          result.box3 mustBe true
          result.box4 mustBe true
        }

        "It is false then boxes 1,2,3, and 4 will be false" in {
          val result = (declaration + ("declaration" ->
            JsBoolean(false))).as[PensionSchemeAdministratorDeclarationType](PensionSchemeAdministratorDeclarationType.apiReads)

          result.box1 mustBe false
          result.box2 mustBe false
          result.box3 mustBe false
          result.box4 mustBe false
        }
      }

      "We have a fitAndProper declaration field" in {
        val result = declaration.as[PensionSchemeAdministratorDeclarationType](PensionSchemeAdministratorDeclarationType.apiReads)

        result.box7 mustBe true
      }

      "We have a declarationWorkingKnowledge field" when {
        "set as 'workingKnowledge'" in {
          val result = declaration.as[PensionSchemeAdministratorDeclarationType](PensionSchemeAdministratorDeclarationType.apiReads)

          result.box5.value mustBe true
        }

        "set as 'adviser'" in {
          val adviserDeclaration = "declarationWorkingKnowledge" -> JsString("adviser")

          val result = (declaration + adviserDeclaration).as[PensionSchemeAdministratorDeclarationType](PensionSchemeAdministratorDeclarationType.apiReads)

          result.box6.value mustBe true
        }

        "set as 'adviser' containing adviser details" in {
          val adviserDetails = "adviserDetails" -> Json.obj("name" -> JsString("John"), "phone" -> "07592113", "email" -> "test@test.com")

          val adviserAddress = "adviserAddress" -> Json.obj("addressLine1" -> JsString("line1"),
            "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"),
            "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))

          val workingKnowledge = "declarationWorkingKnowledge" -> JsString("adviser")
          val result = (declaration + workingKnowledge + adviserDetails + adviserAddress).as[PensionSchemeAdministratorDeclarationType](
            PensionSchemeAdministratorDeclarationType.apiReads)

          result.box6.value mustBe true
          result.pensionAdvisorDetail.value mustBe pensionAdviserSample
        }
      }
    }
  }
}
