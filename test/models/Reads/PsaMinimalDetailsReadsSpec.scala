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

package models.Reads

import models.{PSAMinimalDetails, Samples}
import java.time.LocalDate
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class PsaMinimalDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {

  "A payload containing psa minimal details" should {

    "transform to a valid individual details" when {
      /*"we have firstName" in {
        val individualDetailsPayload = Json.parse(
          """
            |"individualDetails": {
            |			"firstName": "abcdefghjffgfg",
            |			"middleName": "dfgfdgdfgfdgd",
            |			"lastName": "sfdsfsdgdfgdfg"
            |		}
          """.stripMargin
        )

        val output = individualDetailsPayload.as[PSAIndividualDetails]
        output.firstName mustBe ""
      }*/
    }

    "transform to a valid PSA Minimal Details Model" when {

      "we have a processing date" in {
        val output = outputPayload.as[PSAMinimalDetails](PSAMinimalDetails.customReads)
        output.processingDate mustBe LocalDate.parse("2001-12-17")
      }

      "we have an email" in {
        val output = outputPayload.as[PSAMinimalDetails](PSAMinimalDetails.customReads)
        output.email mustBe "aaa@email.com"
      }

      "we have psaSuspensionFlag" in {
        val output = outputPayload.as[PSAMinimalDetails](PSAMinimalDetails.customReads)
        output.isPsaSuspended mustBe true
      }
    }
  }


  val outputPayload =Json.parse(
    """{
      |	"processingDate": "2001-12-17T09:30:47Z",
      |	"psaMinimalDetails": {
      |		"individualDetails": {
      |			"firstName": "abcdefghjffgfg",
      |			"middleName": "dfgfdgdfgfdgd",
      |			"lastName": "sfdsfsdgdfgdfg"
      |		}
      |	},
      |	"email": "aaa@email.com",
      |	"psaSuspensionFlag": true
      |}""".stripMargin)
}

