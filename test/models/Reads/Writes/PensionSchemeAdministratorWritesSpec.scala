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

package models.Reads.Writes

import models.{PensionSchemeAdministrator, PreviousAddressDetails, Samples}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json

class PensionSchemeAdministratorWritesSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "An object of a Pension Scheme Administrator" should {
    Seq("director", "partner").foreach { personType =>

      s"Map all ${personType}s previous addresses to `previousDetail`" when {

        "We are doing a PSA submission containing previous address at rool level" in {
          val result = Json.toJson(pensionSchemeAdministratorSample.copy(previousAddressDetail = PreviousAddressDetails(true, Some(ukAddressSample))))(
            PensionSchemeAdministrator.psaSubmissionWrites)

          result.toString() must include("true,\"previousAddressDetail\":")
        }

        s"We are doing a PSA submission with ${personType}s that have previous address" in {
          val directorWithPreviousAddress = directorOrPartnerSample(personType).copy(previousAddressDetail = PreviousAddressDetails(true, Some(ukAddressSample)))
          val result = Json.toJson(pensionSchemeAdministratorSample.copy(directorOrPartnerDetail = Some(List(directorWithPreviousAddress))))(
            PensionSchemeAdministrator.psaSubmissionWrites)

          result.toString() must include("true,\"previousAddressDetail\":")
        }
      }
    }
  }
}
