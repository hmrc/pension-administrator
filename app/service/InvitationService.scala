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

package service

import com.google.inject.{ImplementedBy, Inject}
import connectors.AssociationConnector
import models.Invitation
import play.api.Logger
import play.api.libs.json.{JsResultException, _}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[InvitationServiceImpl])
trait InvitationService {

  def invitePSA(jsValue: JsValue)
                 (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, JsValue]]

}

class InvitationServiceImpl @Inject()(associationConnector: AssociationConnector) extends InvitationService {

  override def invitePSA(jsValue: JsValue)
                          (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, rh: RequestHeader): Future[Either[HttpException, JsValue]] = {
    jsValue.validate[Invitation].fold(
      invalid = {
        errors =>
          Logger.warn(s"Json contains bad data $errors")
          Future.failed(new BadRequestException(s"Bad invitation format sent $errors"))
      },
      valid = { invitation =>
        associationConnector.getPSAMinimalDetails(invitation.inviteePsaId)
      }
    )

  }
}
