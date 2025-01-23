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
import connectors.DesConnector
import models.{PsaToBeRemovedFromScheme, PsaToBeRemovedFromSchemeSelf}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import service.SchemeService
import uk.gov.hmrc.http.{BadRequestException, _}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class SchemeController @Inject()(
                                  schemeService: SchemeService,
                                  desConnector: DesConnector,
                                  cc: ControllerComponents,
                                  authAction: actions.PsaPspEnrolmentAuthAction,
                                  noEnrolmentAuthAction: actions.NoEnrolmentAuthAction,
                                  psaAuthAction: actions.PsaEnrolmentAuthAction,
                                  psaSchemeAuthAction: actions.PsaSchemeAuthAction
                                )(implicit val ec: ExecutionContext)
  extends BackendController(cc) with ErrorHandler {

  private val logger = Logger(classOf[SchemeController])

  def registerPSA: Action[AnyContent] = noEnrolmentAuthAction.async {
    implicit request => {

      val feJson = request.body.asJson
      logger.debug(s"[PSA-Registration-Incoming-Payload]$feJson")

      feJson match {
        case Some(jsValue) =>
          schemeService.registerPSA(jsValue).map {
            case Right(json) => Ok(json)
            case Left(e: ForbiddenException) if e.getMessage.contains("INVALID_PSAID") => Forbidden("INVALID_PSAID")
            case Left(e) => result(e)
          }
        case _ => Future.failed(new BadRequestException("Bad Request with no request body returned for register PSA"))
      }
    } recoverWith recoverFromError
  }

  def getPsaDetailsSelf: Action[AnyContent] = psaAuthAction.async {
    implicit request =>
      desConnector.getPSASubscriptionDetails(request.psaId.id).map {
        case Right(psaDetails) => Ok(Json.toJson(psaDetails))
        case Left(e) => result(e)
      }
  }

  def removePsa(srn: String): Action[PsaToBeRemovedFromSchemeSelf] =
    (psaAuthAction andThen psaSchemeAuthAction(srn)).async(parse.json[PsaToBeRemovedFromSchemeSelf]) { implicit request =>
      val body = request.body
      desConnector.removePSA(PsaToBeRemovedFromScheme(request.psaId.id, body.pstr, body.removalDate)) map {
        case Right(_) =>
          NoContent
        case Left(e) => result(e)
      }
    }

  def deregisterPsa(psaId: String): Action[AnyContent] = authAction.async {
    implicit request =>
      desConnector.deregisterPSA(psaId).map {
        case Right(_) => NoContent
        case Left(e) => result(e)
      }
  }

  def deregisterPsaSelf: Action[AnyContent] = psaAuthAction.async {
    implicit request =>
      desConnector.deregisterPSA(request.psaId.id).map {
        case Right(_) => NoContent
        case Left(e) => result(e)
      }
  }

  def updatePsaSelf: Action[AnyContent] = psaAuthAction.async {
    implicit request =>

      request.body.asJson match {
        case Some(jsValue) =>
          schemeService.updatePSA(request.psaId.id, jsValue).map {
            case Right(_) => Ok
            case Left(e) => result(e)
          }
        case _ => Future.failed(new BadRequestException("No PSA variation details in the header for psa update/variatiosn"))
      }
  }
}
