/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.domain.PsaId

case class PensionAdviserDetails(name: String, addressDetail: Address, email: String)

object PensionAdviserDetails {
  implicit val formats: Format[PensionAdviserDetails] = Json.format[PensionAdviserDetails]
}

case class AcceptedInvitation(
                               pstr: String,
                               inviteePsaId: PsaId,
                               inviterPsaId: PsaId,
                               declaration: Boolean,
                               declarationDuties: Boolean,
                               pensionAdviserDetails: Option[PensionAdviserDetails]
                             )

object AcceptedInvitation {
  implicit val formats: Format[AcceptedInvitation] = Json.format[AcceptedInvitation]
}