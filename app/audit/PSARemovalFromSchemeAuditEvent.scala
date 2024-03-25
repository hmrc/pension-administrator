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

import models.PsaToBeRemovedFromScheme
import org.joda.time.format.DateTimeFormat

import java.time.format.DateTimeFormatter

case class PSARemovalFromSchemeAuditEvent(psaToBeRemovedFromScheme: PsaToBeRemovedFromScheme) extends AuditEvent {
  override def auditType: String = "PSARemoveFromScheme"

  def formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM YYYY")

  override def details: Map[String, String] =
    Map(
      "psaId" -> psaToBeRemovedFromScheme.psaId,
      "pstr" -> psaToBeRemovedFromScheme.pstr,
      "removalDate" -> formatter.format(psaToBeRemovedFromScheme.removalDate)
    )
}
