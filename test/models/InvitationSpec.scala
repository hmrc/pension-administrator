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

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.domain.PsaId

import java.time.Instant

class InvitationSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures {

  import models._

  "Invitation" should {

    val invitation = Invitation(
      srn = SchemeReferenceNumber("S2400000041"),
      pstr = "24000041IN",
      schemeName = "Open Scheme Overview API Test 2",
      inviterPsaId = PsaId("A2100005"),
      inviteePsaId = PsaId("A2100006"),
      inviteeName = "Richard Clarkson",
      expireAt = Instant.parse("2024-06-03T00:00:00Z")
    )

    "serialize correctly" in {

      val json = Json.toJson(invitation)
      val result = json.validate[Invitation]

      result mustBe a[JsSuccess[Invitation]]
      result.get mustBe invitation
    }

    "de-serialize correctly" in {

        val json = """{"srn":{"id":"S2400000041"},"pstr":"24000041IN","schemeName":"Open Scheme Overview API Test 2",
                  |"inviterPsaId":"A2100005","inviteePsaId":"A2100006",
                  |"inviteeName":"Richard Clarkson","expireAt":"2024-06-03T00:00:00"}""".stripMargin

      val invitationFromJson = Json.parse(json).as[Invitation]

      invitationFromJson mustBe invitation

    }


  }
}

