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

package audit

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

class PSAChangesSpec extends FlatSpec with Matchers {

  private val legalStatus = "aa"
  private val requestJson = Json.obj("abc" -> "def")
  private val responseJson = Json.obj("xyz" -> "pqr")


  private val event = PSAChanges(
    legalStatus = legalStatus,
    status = 200,
    request = requestJson,
    response = Some(responseJson)
  )

  private val expected = Map(
    "legalStatus" -> legalStatus,
    "status" -> "200",
    "request" -> Json.stringify(requestJson),
    "response" -> Json.stringify(responseJson)
  )

  "PSAChanges" should "return the correct audit data" in {

    event.auditType shouldBe "PSAVariation"

    event.details shouldBe expected
  }
}
