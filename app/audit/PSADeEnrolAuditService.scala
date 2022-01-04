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

import play.api.Logger
import play.api.http.Status
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpException

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait PSADeEnrolAuditService {

  private val logger = Logger(classOf[PSADeEnrolAuditService])

  def sendPSADeEnrolEvent(psaId: String)
                         (sendEvent: PSADeEnrol => Unit)
                         (implicit request: RequestHeader, ec: ExecutionContext): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {

    case Success(Right(response)) =>
      sendAuditEvent(psaId, Status.OK, Some(response))(sendEvent)

    case Success(Left(e)) =>
      sendAuditEvent(psaId, e.responseCode, None)(sendEvent)

    case Failure(t) =>
      logger.error("Error in sending audit event for PSA de-enrolment", t)
  }

  private def sendAuditEvent(psaId: String, status: Int, response: Option[JsValue])
                            (sendEvent: PSADeEnrol => Unit): Unit =
    sendEvent(
      PSADeEnrol(
        psaId = psaId,
        status = status,
        response = response)
    )
}
