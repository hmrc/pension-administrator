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

package models.registrationnoid

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class OrganisationRegistrantWritesSpec extends AnyWordSpec with Matchers {

  "An OrganisationRegistrant" must {
    "parse correctly to a valid DES format" when {
      "we have a correct organisation registrant " in {
        val orgRegistrant = OrganisationRegistrant(OrganisationName("test company name"),
          Address("line 1", "line 2", Some("line 3"), Some("line4"), None, "DE"))
        val expectedJsValue = Json.parse(
          """{
            |  "regime": "PODA",
            |  "acknowledgementReference": "test-correlation-id",
            |  "isAnAgent": false,
            |  "isAGroup": false,
            |  "contactDetails": {
            |    "phoneNumber": null,
            |    "mobileNumber": null,
            |    "faxNumber": null,
            |    "emailAddress": null
            |  },
            |  "organisation": {
            |    "organisationName": "test company name"
            |  },
            |  "address": {
            |    "addressLine1": "line 1",
            |    "addressLine2": "line 2",
            |    "addressLine3": "line 3",
            |    "addressLine4": "line4",
            |    "countryCode": "DE"
            |  }
            |}
          """.stripMargin
        )

        val result = Json.toJson(orgRegistrant)(using OrganisationRegistrant.writesOrganisationRegistrantRequest("test-correlation-id"))

        result mustBe expectedJsValue
      }
    }
  }

}
