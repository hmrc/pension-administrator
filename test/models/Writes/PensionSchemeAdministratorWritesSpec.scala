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

import base.JsonFileReader
import models.{PensionSchemeAdministrator, PreviousAddressDetails, Samples}
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class PensionSchemeAdministratorWritesSpec extends AnyWordSpec with Matchers with OptionValues with Samples with JsonFileReader {
  "An object of a Pension Scheme Administrator" should {
    Seq("director", "partner").foreach { personType =>

      s"Map all ${personType}s previous addresses to `previousDetail`" when {

        "parse pension adviser correctly" in {

          val pensionSchemeAdministratorSampleTwo = pensionSchemeAdministratorSample.copy(declaration = declarationSampleTwo)
          val result = Json.toJson(pensionSchemeAdministratorSampleTwo)(using PensionSchemeAdministrator.psaSubmissionWrites)

          result.toString().must(include("\"pensionAdvisorDetail\":"))
        }

        "We are doing a PSA submission containing previous address at root level" in {
          val result = Json.toJson(pensionSchemeAdministratorSample.copy(previousAddressDetail = PreviousAddressDetails(true, Some(ukAddressSample))))(
            using PensionSchemeAdministrator.psaSubmissionWrites)

          result.toString().must(include("true,\"previousAddressDetail\":"))
        }

        s"We are doing a PSA submission with ${personType}s that have previous address" in {
          val directorWithPreviousAddress = directorOrPartnerSample(personType)
            .copy(previousAddressDetail = PreviousAddressDetails(true, Some(ukAddressSample)))
          val result = Json.toJson(pensionSchemeAdministratorSample.copy(directorOrPartnerDetail = Some(List(directorWithPreviousAddress))))(
            using PensionSchemeAdministrator.psaSubmissionWrites)

          result.toString().must(include("true,\"previousAddressDetail\":"))
        }

        s"We are checking the changeOfDirectorOrPartnerDetails flag is not included" in {
          val result = Json.toJson(pensionSchemeAdministratorSample)(using PensionSchemeAdministrator.psaSubmissionWrites)

          (result \ "changeOfDirectorOrPartnerDetails").asOpt[Boolean].mustBe(None)
        }

        //FAIL
        s"We are doing a PSA submission with ${personType}s containing directorOrPartnerDetail at root level for update writes" in {
          val result = Json.toJson(pensionSchemeAdministratorSampleTwo(personType)
            .copy(previousAddressDetail = PreviousAddressDetails(true, Some(ukAddressSample))))(using PensionSchemeAdministrator.psaUpdateWrites)

          result.mustBe(readJsonFromFile(s"/data/validPsaVariationRequest$personType.json"))
        }
      }
    }
  }
}
