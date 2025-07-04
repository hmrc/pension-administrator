/*
 * Copyright 2025 HM Revenue & Customs
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

import base.CommonHelper
import models.{Samples, Reads as _, *}
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class PensionAdvisorDetailsWritesSpec extends AnyWordSpec with Matchers with OptionValues with Samples with CommonHelper {

  "A pension advisor details object" should {
    "Map pension advisor" when {
      val result = Json.toJson(pensionAdvisorDetail)(using PensionAdvisorDetail.psaUpdateWrites)

      Seq(
        ("addressDetails", Json.toJson(nonUkAddressSample)(using InternationalAddress.updatePreviousAddressWrites)),
        ("contactDetails", Json.toJson(contactDetailsSample))
      ).foreach { testElement =>

        s"testing for element ${testElement._1} having value ${testElement._2}" in {

          testElementValue(result, elementName = testElement._1, expectedValue = testElement._2)
        }
      }

      s"testing for element `name` having value `xyz`" in {

        testElementValue(result, elementName = "name", expectedValue = "xyz")
      }
    }
  }
}
