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

package audit

import models._
import org.joda.time.DateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.domain.PsaId

class InvitationAuditEventSpec extends AnyFlatSpec with Matchers {

  "InvitationAuditEvent" should "output the correct map of data" in {

    val psaId = PsaId("A2123456")
    val psaIdInvitee = PsaId("A2123456")
    val pstr = "scheme"
    val schemeName = "ssss"
    val inviteeName = "www"
    val expireAt = DateTime.now
    val srn = "srn"
    val invitation = Invitation(
      srn = SchemeReferenceNumber(srn),
      pstr = pstr,
      schemeName = schemeName,
      inviterPsaId = psaId,
      inviteePsaId = psaIdInvitee,
      inviteeName = inviteeName,
      expireAt: DateTime)


    val event = InvitationAuditEvent(
      invitation = invitation
    )

    val expected: Map[String, String] = Map(
      "inviteeName" -> invitation.inviteeName,
      "inviteePsaId" -> invitation.inviteePsaId.value,
      "inviterPsaId" -> invitation.inviterPsaId.value,
      "pstr" -> invitation.pstr,
      "schemeName" -> invitation.schemeName
    )

    event.auditType shouldBe "PSAInvitation"
    event.details shouldBe expected
  }
}
