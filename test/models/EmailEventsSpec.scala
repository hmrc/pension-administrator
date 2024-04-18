/*
 * Copyright 2024 HM Revenue & Customs
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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

import java.time.{LocalDateTime, ZoneId}

class EmailEventsSpec extends AnyFreeSpec with Matchers {
  "Should parse email body" in {
    val emailBody =
      """{
        |            "event": "Opened",
        |            "detected": "2015-07-02T08:26:39.035Z"
        |        }""".stripMargin


    val timestampAsInstant = LocalDateTime.parse("2015-07-02T08:26:39.035").atZone(ZoneId.of("UTC")).toInstant

    Json.parse(emailBody).as[EmailEvent] shouldBe EmailEvent(Opened, timestampAsInstant)
  }
}
