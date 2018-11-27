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

package audit

import models.{AcceptedInvitation, PSAMinimalDetails}
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpException

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait AssociationAuditService {

  def sendGetMinimalPSADetailsEvent(psaId: String)(sendEvent: MinimalPSADetails => Unit)
                                   (implicit rh: RequestHeader, ec: ExecutionContext):
  PartialFunction[Try[Either[HttpException, PSAMinimalDetails]], Unit] = {

    case Success(Right(psaMinimalDetails)) =>
      sendEvent(
        MinimalPSADetails(
          psaId = psaId,
          psaName = psaMinimalDetails.name,
          isPsaSuspended = Some(psaMinimalDetails.isPsaSuspended),
          status = Status.OK,
          response = Some(Json.toJson(psaMinimalDetails))
        )
      )
    case Success(Left(e)) =>
      sendEvent(
        MinimalPSADetails(
          psaId = psaId,
          psaName = None,
          isPsaSuspended = None,
          status = e.responseCode,
          response = None
        )
      )
    case Failure(t) =>
      Logger.error("Error in AssociationConnector connector", t)
  }

  def sendAcceptInvitationAuditEvent(acceptedInvitation: AcceptedInvitation, status: Int,
                                     response: Option[JsValue])(sendEvent: InvitationAcceptanceAuditEvent => Unit)
                                   (implicit rh: RequestHeader, ec: ExecutionContext): Unit = {

    sendEvent(InvitationAcceptanceAuditEvent(acceptedInvitation, status, response))

  }

}