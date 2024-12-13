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

package models.Reads

import models.{Samples, Reads => _, _}
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._


class DeclarationTypeReadsSpec extends AnyWordSpec with Matchers with OptionValues with Samples {

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

        "It is false then boxes 1,2,3, and 4 will be false " in {
          val result = (declaration + ("declaration" ->
            JsBoolean(false))).as[PensionSchemeAdministratorDeclarationType](PensionSchemeAdministratorDeclarationType.apiReads)

          result.box1 mustBe false
          result.box2 mustBe false
          result.box3 mustBe false
          result.box4 mustBe false
        }
      }

      "We have an isChanged flag" in {
        val result = (declaration + ("isChanged" -> JsBoolean(true)))
          .as[PensionSchemeAdministratorDeclarationType](PensionSchemeAdministratorDeclarationType.apiReads)

        result.isChanged.value mustBe true
      }

      "We have a fitAndProper declaration field" in {
        val result = declaration.as[PensionSchemeAdministratorDeclarationType](PensionSchemeAdministratorDeclarationType.apiReads)

        result.box7 mustBe true
      }

      "We have a declarationWorkingKnowledge field" when {
        "set as 'workingKnowledge'" in {
          val result = declaration.as[PensionSchemeAdministratorDeclarationType](PensionSchemeAdministratorDeclarationType.apiReads)

          result.box5 mustBe Some(true)
          result.box6 mustBe None
        }

        "set as 'taskList' (have working knowledge)" in {
          val declaration = Json.obj("declaration" -> JsBoolean(true), "declarationFitAndProper" -> JsBoolean(true),
            "declarationWorkingKnowledge" -> JsString("taskList"))
          val result = declaration.as[PensionSchemeAdministratorDeclarationType](PensionSchemeAdministratorDeclarationType.apiReads)

          result.box5 mustBe Some(true)
          result.box6 mustBe None
        }

        "set as 'adviser'" in {
          val adviserDeclaration = "declarationWorkingKnowledge" -> JsString("adviser")

          val result = (declaration + adviserDeclaration).as[PensionSchemeAdministratorDeclarationType](PensionSchemeAdministratorDeclarationType.apiReads)

          result.box5 mustBe None
          result.box6 mustBe Some(true)
        }

        "set as 'whatyouWillNeed' (I don't have working knowledge)" in {
          val adviserDeclaration = "declarationWorkingKnowledge" -> JsString("whatyouWillNeed")

          val result = (declaration + adviserDeclaration).as[PensionSchemeAdministratorDeclarationType](PensionSchemeAdministratorDeclarationType.apiReads)

          result.box5 mustBe None
          result.box6 mustBe Some(true)
        }

        "set as 'adviser' containing adviser details" in {
          val adviserName = "adviserName" -> JsString("John")
          val adviserPhone = "adviserPhone" -> JsString("07592113")
          val adviserEmail = "adviserEmail" -> JsString("test@test.com")

          val adviserAddress = "adviserAddress" -> Json.obj("addressLine1" -> JsString("line1"),
            "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"),
            "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))

          val workingKnowledge = "declarationWorkingKnowledge" -> JsString("adviser")
          val result = (declaration + workingKnowledge + adviserName + adviserEmail + adviserPhone + adviserAddress).as[PensionSchemeAdministratorDeclarationType](
            PensionSchemeAdministratorDeclarationType.apiReads)

          result.box5 mustBe None
          result.box6 mustBe Some(true)
          result.pensionAdvisorDetail.value mustBe pensionAdviserSample
        }

        "set as 'whatyouWillNeed' (I don't have working knowledge) containing adviser details" in {
          val adviserName = "adviserName" -> JsString("John")
          val adviserPhone = "adviserPhone" -> JsString("07592113")
          val adviserEmail = "adviserEmail" -> JsString("test@test.com")

          val adviserAddress = "adviserAddress" -> Json.obj("addressLine1" -> JsString("line1"),
            "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"),
            "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))

          val workingKnowledge = "declarationWorkingKnowledge" -> JsString("whatyouWillNeed")
          val result = (declaration + workingKnowledge + adviserName + adviserEmail + adviserPhone + adviserAddress)
            .as[PensionSchemeAdministratorDeclarationType](PensionSchemeAdministratorDeclarationType.apiReads)

          result.box5 mustBe None
          result.box6 mustBe Some(true)
          result.pensionAdvisorDetail.value mustBe pensionAdviserSample
        }
      }
    }
  }
}
