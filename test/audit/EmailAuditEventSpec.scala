/*
 * Copyright 2022 HM Revenue & Customs
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

import models.Sent
import models.enumeration.JourneyType
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.domain.PsaId

class EmailAuditEventSpec extends AnyFlatSpec with Matchers {

  "EmailAuditEvent" should "output the correct map of data" in {

    val event = EmailAuditEvent(
      psaId = PsaId("A2500001"),
      event = Sent,
      journeyType = JourneyType.PSA

    )

    val expected: Map[String, String] = Map(
      "psaId" -> "A2500001",
      "event" -> Sent.toString
    )

    event.auditType shouldBe s"${JourneyType.PSA}EmailEvent"
    event.details shouldBe expected
  }
}
