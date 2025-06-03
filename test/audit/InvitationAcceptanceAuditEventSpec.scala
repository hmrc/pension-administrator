/*
 * Copyright 2025 HM Revenue & Customs
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

import models.{AcceptedInvitation, PensionAdviserDetails, UkAddress}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.domain.PsaId

class InvitationAcceptanceAuditEventSpec extends AnyFlatSpec with Matchers {

  "InvitationAcceptanceAuditEvent" should "output the correct map of data" in {

    val email = "aaa@aaa.com"
    val ukAddress = UkAddress(
      addressLine1 = "address line 1",
      addressLine2 = Some("address line 2"),
      addressLine3 = Some("address line 3"),
      addressLine4 = Some("address line 4"), countryCode = "GB", postalCode = "ZZ11ZZ"
    )
    val pensionAdvisorName = "pension advisor 1"
    val pensionAdviserDetailUK = PensionAdviserDetails(name = pensionAdvisorName, addressDetail = ukAddress, email = email)
    val psaId = PsaId("A2123456")
    val psaIdInvitee = PsaId("A2123456")
    val pstr = "scheme"
    val acceptedInvitation = AcceptedInvitation(pstr = pstr, inviteePsaId = psaIdInvitee, declaration = true, declarationDuties = false, inviterPsaId = psaId,
      pensionAdviserDetails = Some(pensionAdviserDetailUK))

    val responseJson = Json.obj("abc" -> "def")

    val event = InvitationAcceptanceAuditEvent(
      acceptedInvitation = acceptedInvitation,
      status = 200,
      response = Some(responseJson)
    )

    val expected: Map[String, String] = Map(
      "pstr" -> acceptedInvitation.pstr,
      "inviteePsaId" -> acceptedInvitation.inviteePsaId.id,
      "inviterPsaId" -> acceptedInvitation.inviterPsaId.id,
      "declaration" -> acceptedInvitation.declaration.toString,
      "declarationDuties" -> acceptedInvitation.declarationDuties.toString,
      "response" -> Json.stringify(responseJson)
    )

    event.auditType.shouldBe("PSAInvitationAccepted")
    event.details.shouldBe(expected)
  }
}
