/*
 * Copyright 2021 HM Revenue & Customs
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

import models.{MinimalDetails, AcceptedInvitation}
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpException

import scala.concurrent.ExecutionContext
import scala.util.{Try, Success, Failure}

trait AssociationAuditService {

  private val logger = Logger(classOf[AssociationAuditService])

  def sendGetMinimalDetailsEvent(idType: String, idValue: String)(sendEvent: MinimalDetailsEvent => Unit)
    (implicit rh: RequestHeader, ec: ExecutionContext):
  PartialFunction[Try[Either[HttpException, MinimalDetails]], Unit] = {

    case Success(Right(psaMinimalDetails)) =>
      sendEvent(
        MinimalDetailsEvent(
          idType = idType,
          idValue = idValue,
          name = psaMinimalDetails.name,
          isSuspended = Some(psaMinimalDetails.isPsaSuspended),
          rlsFlag = Some(psaMinimalDetails.rlsFlag),
          deceasedFlag = Some(psaMinimalDetails.deceasedFlag),
          status = Status.OK,
          response = Some(Json.toJson(psaMinimalDetails))
        )
      )
    case Success(Left(e)) =>
      sendEvent(
        MinimalDetailsEvent(
          idType = idType,
          idValue = idValue,
          name = None,
          isSuspended = None,
          rlsFlag = None,
          deceasedFlag = None,
          status = e.responseCode,
          response = None
        )
      )
    case Failure(t) =>
      logger.error("Error in AssociationConnector connector", t)
  }

  def sendAcceptInvitationAuditEvent(acceptedInvitation: AcceptedInvitation, status: Int,
                                     response: Option[JsValue], originalData: JsValue)(sendEvent: InvitationAcceptanceAuditEvent => Unit)
                                   (implicit rh: RequestHeader, ec: ExecutionContext): Unit = {

    sendEvent(InvitationAcceptanceAuditEvent(acceptedInvitation, status, response, originalData))

  }

}
