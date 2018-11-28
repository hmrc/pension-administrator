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

package audit

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

class MinimalPSADetailsEventSpec extends FlatSpec with Matchers {

  "MinimalPSADetails.details" should "output the correct map of data" in {

    val response = Json.obj("name" -> "response")

    val event = MinimalPSADetailsEvent(
      psaId = "A2500001",
      psaName = Some("John Doe"),
      isPsaSuspended = Some(false),
      status = 200,
      response = Some(response)
    )

    val expected: Map[String, String] = Map(
      "PSAID" -> "A2500001",
      "PSAName" -> "John Doe",
      "isPsaSuspended" -> "false",
      "status" -> "200",
      "response" -> Json.stringify(response)
    )

    event.auditType shouldBe "GetMinPSADetails"

    event.details shouldBe expected
  }
}
