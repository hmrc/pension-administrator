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

package controllers

import com.google.inject.Inject
import connectors.UpdateClientReferenceConnector
import controllers.actions.PsaPspEnrolmentAuthAction
import models.UpdateClientReferenceRequest
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.http.{BadRequestException, HttpException}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class UpdateClientReferenceController @Inject()(
                                                 updateClientReferenceConnector: UpdateClientReferenceConnector,
                                                 cc: ControllerComponents,
                                                 authAction: PsaPspEnrolmentAuthAction
                                               )(implicit val ec: ExecutionContext) extends BackendController(cc) with ErrorHandler {

  def updateClientReference: Action[AnyContent] = authAction.async {
    implicit request => {
      request.body.asJson match {
        case Some(jsValue) =>
          jsValue.validate[UpdateClientReferenceRequest] match {
            case JsSuccess(updateClientRef, _) =>
              val userAction = request.headers.get("userAction")
              updateClientReferenceConnector.updateClientReference(updateClientRef, userAction) map handleResponse
            case JsError(_) =>
              val error = new BadRequestException(s"Invalid request received from frontend for update Client Reference")
              Future.failed(error)
          }
        case _ =>
          Future.failed(new BadRequestException("No request body received for update Client Reference"))
      }
    }
  }

  private def handleResponse: PartialFunction[Either[HttpException, JsValue], Result] = {
    case Right(successResponse) => Ok(Json.toJson(successResponse))
    case Left(e: HttpException) => result(e)
  }
}
