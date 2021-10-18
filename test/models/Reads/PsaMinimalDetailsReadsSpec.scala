/*
 * Copyright 2021 HM Revenue & Customs
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

import models.{MinimalDetails, IndividualDetails}
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._

class PsaMinimalDetailsReadsSpec extends AnyWordSpec with Matchers with OptionValues {

  import PsaMinimalDetailsReadsSpec._

  "A payload containing psa minimal details in IF format" should {

    "transform to a valid PSA Minimal Details model" when {

      "we have an email" in {
        val output = individualDetailIFPayload.as[MinimalDetails](MinimalDetails.minimalDetailsIFReads)
        output.email mustBe psaMinimalDetailsUser.email
      }

      "we have psaSuspensionFlag" in {
        val output = individualDetailIFPayload.as[MinimalDetails](MinimalDetails.minimalDetailsIFReads)
        output.isPsaSuspended mustBe psaMinimalDetailsUser.isPsaSuspended
      }

      "we have minimal detail object with individual data" in {
        val output = individualDetailIFPayload.as[MinimalDetails](MinimalDetails.minimalDetailsIFReads)
        output.individualDetails mustBe psaMinimalDetailsUser.individualDetails
      }

      "we have minimal detail object with organisation data" in {
        val output = orgIFPayload.as[MinimalDetails](MinimalDetails.minimalDetailsIFReads)
        output.organisationName mustBe psaMinimalDetailsUser.organisationName
      }

      "we have rlsFlag" in {
        val output = individualDetailIFPayload.as[MinimalDetails](MinimalDetails.minimalDetailsIFReads)
        output.rlsFlag mustBe psaMinimalDetailsUser.rlsFlag
      }

      "we have deceasedFlag" in {
        val output = individualDetailIFPayload.as[MinimalDetails](MinimalDetails.minimalDetailsIFReads)
        output.deceasedFlag mustBe psaMinimalDetailsUser.deceasedFlag
      }
    }
  }

}

object PsaMinimalDetailsReadsSpec {
  private val individualDetailIFPayload = Json.parse(
    """{
      |	"processingDate": "2001-12-17T09:30:47Z",
      |	"minimalDetails": {
      |		"individualDetails": {
      |			"firstName": "testFirst",
      |			"middleName": "testMiddle",
      |			"lastName": "testLast"
      |		}
      |	},
      |	"email": "test@email.com",
      |	"psaSuspensionFlag": true,
      |	"rlsFlag": true,
      |	"deceasedFlag": true
      |}""".stripMargin)

  private val orgIFPayload = Json.parse(
    """
      |{
      |	"processingDate": "2009-12-17T09:30:47Z",
      |	"minimalDetails": {
      |		"organisationOrPartnershipName": "test org name"
      |	},
      |	"email": "test@email.com",
      |	"psaSuspensionFlag": true,
      |	"rlsFlag": true,
      |	"deceasedFlag": true
      |}
      |
    """.stripMargin

  )

  val psaMinimalDetailsUser = MinimalDetails(
    "test@email.com",
    isPsaSuspended = true,
    Some("test org name"),
    Some(IndividualDetails(
      "testFirst",
      Some("testMiddle"),
      "testLast"
    )),
    rlsFlag = true,
    deceasedFlag = true
  )
}

