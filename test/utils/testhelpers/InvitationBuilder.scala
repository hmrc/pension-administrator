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

package utils.testhelpers

import models.Invitation
import org.joda.time.DateTime

object InvitationBuilder {

  val srn = "test-srn"
  val pstr1 = "S12345"
  val schemeName1 = "Test scheme1 name"
  val inviterPsaId1 = "I12345"
  val inviteePsaId1 = "P12345"
  val inviteeName1 = "Test Invitee1 Name"
  val expiryDate1 = new DateTime("2018-11-10")

  val pstr2 = "D1234"
  val schemeName2 = "Test scheme2 name"
  val inviterPsaId2 = "Q12345"
  val inviteePsaId2 = "T12345"
  val inviteeName2 = "Test Invitee2 Name"
  val expiryDate2 = new DateTime("2018-11-11")

  val invitation1 = Invitation(
    srn,
    pstr = pstr1,
    schemeName = schemeName1,
    inviterPsaId = inviterPsaId1,
    inviteePsaId = inviteePsaId1,
    inviteeName = inviteeName1,
    expireAt = expiryDate1)

  val invitation2 = Invitation(srn,
    pstr = pstr2,
    schemeName = schemeName2,
    inviterPsaId = inviterPsaId2,
    inviteePsaId = inviteePsaId2,
    inviteeName = inviteeName2,
    expireAt = expiryDate2)

  val invitationList = List(invitation1, invitation2)

  val mapBothKeys = Map("pstr" -> pstr1, "inviteePsaId" -> inviteePsaId1)
  val mapPstr = Map("pstr" -> pstr1)
  val mapInviteePsaId = Map("inviteePsaId" -> inviteePsaId1)

}