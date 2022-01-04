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

package audit

import base.SpecBase
import play.api.libs.json.Json


class SchemeAuditServiceSpec extends SpecBase {

  "getName" should {
    "work for partnerships" in {
      val json = Json.obj(
        "registrationInfo" -> Json.obj(
          "legalStatus" -> "Partnership"
        ),
        "partnershipDetails" -> Json.obj(
          "companyName" -> "bla"
        )
      )
      SchemeAuditService.getName(json) mustBe Some("bla")
    }

    "work for ltd companies" in {
      val json = Json.obj(
        "registrationInfo" -> Json.obj(
          "legalStatus" -> "Limited Company"
        ),
        "businessDetails" -> Json.obj(
          "companyName" -> "bla"
        )
      )
      SchemeAuditService.getName(json) mustBe Some("bla")
    }
  }
}
