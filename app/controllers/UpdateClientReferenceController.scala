/*
 * Copyright 2023 HM Revenue & Customs
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
import models.UpdateClientReferenceRequest
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandler

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class UpdateClientReferenceController @Inject()(
                                        override val authConnector: AuthConnector,
                                        updateClientReferenceConnector: UpdateClientReferenceConnector,
                                        cc: ControllerComponents
                                      )(implicit val ec: ExecutionContext) extends BackendController(cc) with ErrorHandler with AuthorisedFunctions {

  def updateClientReference: Action[AnyContent] = Action.async {
    implicit request => {
      retrieveUser { _ =>
        request.body.asJson match {
          case Some(jsValue) =>
            jsValue.validate[UpdateClientReferenceRequest] match {
              case JsSuccess(updateClientRef, _) =>
               val userAction= request.headers.get("userAction")
                updateClientReferenceConnector.updateClientReference(updateClientRef,userAction) map handleResponse
              case JsError(_) =>
                val error = new BadRequestException(s"Invalid request received from frontend for update Client Reference" )
                Future.failed(error)
            }
          case _ =>
            Future.failed(new BadRequestException("No request body received for update Client Reference"))
        }
      }
    }
  }

  private def handleResponse: PartialFunction[Either[HttpException, JsValue], Result] = {
    case Right(successResponse) => Ok(Json.toJson(successResponse))
    case Left(e: HttpException) => result(e)
  }

  private def retrieveUser(fn: models.User => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    authorised().retrieve(v2.Retrievals.externalId and v2.Retrievals.affinityGroup) {
      case Some(externalId) ~ Some(affinityGroup) =>
        fn(models.User(externalId, affinityGroup))
      case _ =>
        Future.failed(UpstreamErrorResponse("Not authorized", UNAUTHORIZED, UNAUTHORIZED))
    }
  }
}
