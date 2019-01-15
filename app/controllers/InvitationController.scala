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

package controllers

import com.google.inject.Inject
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import service.InvitationService
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import utils.ErrorHandler
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future}

class InvitationController @Inject()(invitationService: InvitationService,
                                     cc: ControllerComponents
                                    )extends BackendController(cc) with ErrorHandler {

  def invite(): Action[AnyContent] = Action.async {
    implicit request =>
      val invitationDetails = request.body.asJson

      invitationDetails match {
        case Some(jsValue) =>
          invitationService.invitePSA(jsValue).map {
            case Right(_) => Created
            case Left(e) => result(e)
          }
        case _ => Future.failed(new BadRequestException("Bad Request with no request body returned for invite PSA"))
      }

  }

}
