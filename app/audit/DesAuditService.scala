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

import models.PsaSubscription
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpException

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait DesAuditService {

  def sendPSADetailsEvent(psaId: String)(sendEvent: PSADetails => Unit)
                         (implicit rh: RequestHeader, ec: ExecutionContext): PartialFunction[Try[Either[HttpException, PsaSubscription]], Unit] = {

    case Success(Right(psaSubscription)) =>
      sendEvent(
        PSADetails(
          psaId = psaId,
          psaName = psaSubscription.name,
          status = Status.OK,
          response = Some(Json.toJson(psaSubscription))
        )
      )
    case Success(Left(e)) =>
      sendEvent(
        PSADetails(
          psaId = psaId,
          psaName = None,
          status = e.responseCode,
          response = None
        )
      )
    case Failure(t) =>
      Logger.error("Error in DesConnector", t)

  }
}
