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

class PSARegistrationSpec extends AnyFlatSpec with Matchers {

  private val status = 0
  private val externalId = "12131"
  private val found = "true"
  private val isUk = Some("true")
  private val request = Json.obj("name" -> "request")
  private val response = Some(Json.obj("name" -> "response"))

  private val eventUK = PSARegistration(
    withId = true,
    externalId = externalId,
    psaType = "individual",
    found = true,
    isUk= Some(true),
    status = status,
    request = request,
    response = response
  )

  private val eventUKOrg = eventUK.copy(psaType="organisation")
  private val eventNonUK = eventUK.copy(withId=false)
  private val eventNonUKOrg = eventUK.copy(withId=false, psaType="organisation")

  private def expected(withId: Boolean, psaType: String): Map[String, String] = Map(
    "withId" -> withId.toString,
    "externalId" -> externalId,
    "psaType" -> psaType,
    "found" -> found.toString,
    "isUk" -> isUk.map(_.toString).getOrElse(""),
    "status" -> status.toString,
    "request" -> Json.stringify(request),
    "response" -> {
      response match {
        case Some(json) => Json.stringify(json)
        case _ => ""
      }
    }
  )

  "PSARegistration.details" should "output the correct map of data for individual" in {

    eventUK.auditType shouldBe "PSARegistration"

    eventUK.details shouldBe expected(withId = true, "individual")

  }

  it should "output the correct map of data for organisation" in {

    eventUKOrg.auditType shouldBe "PSARegistration"

    eventUKOrg.details shouldBe expected(withId = true, "organisation")

  }

  "PSARegWithoutId.details" should "output the correct map of data for individual" in {

    eventNonUK.auditType shouldBe "PSARegWithoutId"

    eventNonUK.details shouldBe expected(withId = false, "individual")

  }

  it should "output the correct map of data for organisation" in {

    eventNonUKOrg.auditType shouldBe "PSARegWithoutId"

    eventNonUKOrg.details shouldBe expected(withId = false, "organisation")

  }

}
