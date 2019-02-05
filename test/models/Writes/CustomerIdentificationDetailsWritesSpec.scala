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

import models.{PensionSchemeAdministrator, Samples}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json


class CustomerIdentificationDetailsWritesSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "A Pension Scheme Administrator" when {
    "Containing customer identification details" when {
      "Parse correctly to a valid customer identification details payload" when {
        val result =  Json.toJson(pensionSchemeAdministratorSample)(PensionSchemeAdministrator.psaUpdateWrites)

        "we have a legalStatus" in {
          (result \ "customerIdentificationDetails" \ "legalStatus").as[String] mustBe pensionSchemeAdministratorSample.legalStatus
        }

        "we have an idType" in {
          (result \ "customerIdentificationDetails" \ "idType").asOpt[String].value mustBe pensionSchemeAdministratorSample.idType.value
        }

        "we have an idNumber" in {
          (result \ "customerIdentificationDetails" \ "idNumber").asOpt[String].value mustBe pensionSchemeAdministratorSample.idNumber.value
        }

        "we have a noIdentifier" in {
          (result \ "customerIdentificationDetails" \ "noIdentifier").as[Boolean] mustBe pensionSchemeAdministratorSample.noIdentifier
        }
      }
    }
  }
}
