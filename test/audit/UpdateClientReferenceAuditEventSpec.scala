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

import models.UpdateClientReferenceRequest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.http.Status.OK
import play.api.libs.json.Json

import java.time.LocalDate

class UpdateClientReferenceAuditEventSpec extends AnyFlatSpec with Matchers {

  "UpdateClientReferenceAuditEvent" should "output the correct map of data" in {

    val psaId = "A2123456"
    val pspId = "21000005"
    val pstr = "pstr"
    val clientReference = Some("abc123")
    val updateClientReferenceRequest = UpdateClientReferenceRequest(pstr,psaId,pspId,clientReference)
    val response = Some(Json.obj(
      "status" -> "OK",
      "statusText" -> "Hello there!",
      "processingDate" -> LocalDate.now().toString()
    ))

    val event = UpdateClientReferenceAuditEvent(
      updateClientReferenceRequest,response,OK,Some("Added")
    )

    val expected: Map[String, String] = Map(
      "pstr" -> pstr,
      "psaId" -> psaId,
      "pspId" -> pspId,
      "clientReference" -> clientReference.getOrElse(""),
      "status" -> "200",
      "userAction" -> "Added",
      "response" -> Json.stringify(response.get)
    )

    event.auditType.shouldBe("UpdateClientReferenceAudit")
    event.details.shouldBe(expected)
  }

  "UpdateClientReferenceAuditEvent" should "output the correct map of data when response None" in {

    val psaId = "A2123456"
    val pspId = "21000005"
    val pstr = "pstr"
    val clientReference = Some("abc123")
    val updateClientReferenceRequest = UpdateClientReferenceRequest(pstr,psaId,pspId,clientReference)

    val event = UpdateClientReferenceAuditEvent(
      updateClientReferenceRequest,None,OK,Some("Added")
    )

    val expected: Map[String, String] = Map(
      "pstr" -> pstr,
      "psaId" -> psaId,
      "pspId" -> pspId,
      "clientReference" -> clientReference.getOrElse(""),
      "status" -> "200",
      "userAction" -> "Added",
      "response" -> ""
    )

    event.auditType.shouldBe("UpdateClientReferenceAudit")
    event.details.shouldBe(expected)
  }
}
