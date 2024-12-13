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
import connectors.{SchemeConnector, UpdateClientReferenceConnector}
import controllers.actions.{PsaEnrolmentAuthAction, PsaSchemeAuthAction}
import models.{SchemeReferenceNumber, UpdateClientReferenceRequest}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.domain.PspId
import uk.gov.hmrc.http.{BadRequestException, HttpException}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class UpdateClientReferenceController @Inject()(
                                                 updateClientReferenceConnector: UpdateClientReferenceConnector,
                                                 cc: ControllerComponents,
                                                 authAction: PsaEnrolmentAuthAction,
                                                 psaSchemeAuthAction: PsaSchemeAuthAction,
                                                 schemeConnector: SchemeConnector
                                               )(implicit val ec: ExecutionContext) extends BackendController(cc) with ErrorHandler {

  def updateClientReferenceOld: Action[AnyContent] = authAction.async {
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

  def updateClientReference(srn: SchemeReferenceNumber): Action[AnyContent] = (authAction andThen psaSchemeAuthAction(srn)).async { implicit request =>
    def header(name: String) = request.headers.get(name)

    val requiredHeaders = (header("pspId"), header("pstr"))
    requiredHeaders match {
      case (Some(pspId), Some(pstr)) =>
        val clientReference = header("clientReference")
        val userAction = header("userAction")
        val updateClientRef = UpdateClientReferenceRequest(pstr, request.psaId.id, pspId, clientReference)
        schemeConnector.checkForAssociation(Right(PspId(pspId)), srn).flatMap {
          case Right(true) => updateClientReferenceConnector.updateClientReference(updateClientRef, userAction) map handleResponse
          case Right(false) => Future.successful(Forbidden("PspId is not associated with scheme"))
          case Left(e) => throw e
        }
      case (pspId, pstr) =>
        def getMissingHeaderName(name: String, header: Option[_]) = if (header.isEmpty) name else ""

        val missingHeadersMsg = "Required headers missing: " + Seq("pspId" -> pspId, "pstr" -> pstr)
          .map(x => getMissingHeaderName(x._1, x._2)).mkString(" ")
        Future.successful(BadRequest(missingHeadersMsg))
    }
  }

  private def handleResponse: PartialFunction[Either[HttpException, JsValue], Result] = {
    case Right(successResponse) => Ok(Json.toJson(successResponse))
    case Left(e: HttpException) => result(e)
  }
}
