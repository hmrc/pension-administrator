/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json
import play.api.test.Helpers._

class PSADeEnrolSpec extends AnyFlatSpec with Matchers {

  private val psaId: String = "psa-id"
  private val status: Int = OK
  private val response = Some(Json.obj("name" -> "response"))

  private val event = PSADeEnrol(
    psaId: String,
    status: Int,
    response
  )

  private val expected: Map[String, String] = Map(
    "psaId" -> psaId,
    "status" -> status.toString,
    "response" -> {
      response match {
        case Some(json) => Json.stringify(json)
        case _ => ""
      }
    }
  )

  "PSADeEnrol" should "return the correct audit data" in {

    event.auditType shouldBe "PSADeEnrol"

    event.details shouldBe expected
  }
}
