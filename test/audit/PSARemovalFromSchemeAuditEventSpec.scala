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

import models.PsaToBeRemovedFromScheme
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PSARemovalFromSchemeAuditEventSpec extends AnyFlatSpec with Matchers {

  private val psaId: String = "psa-id"
  private val pstr = "scheme"
  private val removalDate = LocalDate.now()

  private val psaToBeRemoved = PsaToBeRemovedFromScheme(psaId = psaId,
    pstr = pstr, removalDate = removalDate)

  private val event = PSARemovalFromSchemeAuditEvent(
    psaToBeRemovedFromScheme = psaToBeRemoved
  )

  private def formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM YYYY")
  private val formattedDate = formatter.format(psaToBeRemoved.removalDate)

  private val expected = Map(
    "psaId" -> psaToBeRemoved.psaId,
    "pstr" -> psaToBeRemoved.pstr,
    "removalDate" -> formattedDate
  )

  "PSARemovalFromSchemeAuditEvent" should "return the correct audit data" in {

    event.auditType.shouldBe("PSARemoveFromScheme")

    event.details.shouldBe(expected)
  }
}
