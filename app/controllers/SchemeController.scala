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

package controllers

import com.google.inject.Inject
import connectors.DesConnector
import models.PsaToBeRemovedFromScheme
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import service.SchemeService
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandler

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class SchemeController @Inject()(schemeService: SchemeService,
                                 schemeConnector: DesConnector,
                                 cc: ControllerComponents
                                )(implicit val ec: ExecutionContext) extends BackendController(cc) with ErrorHandler {

  private val logger = Logger(classOf[SchemeController])

  def registerPSA: Action[AnyContent] = Action.async {
    implicit request => {
      val feJson = request.body.asJson
      logger.debug(s"[PSA-Registration-Incoming-Payload]$feJson")

      feJson match {
        case Some(jsValue) =>
          schemeService.registerPSA(jsValue).map {
            case Right(json) => Ok(json)
            case Left(e) => result(e)
          }
        case _ => Future.failed(new BadRequestException("Bad Request with no request body returned for register PSA"))
      }
    } recoverWith recoverFromError
  }

  def getPsaDetails: Action[AnyContent] = Action.async {
    implicit request =>
      val psaId = request.headers.get("psaId")
      psaId match {
        case Some(id) =>
          schemeConnector.getPSASubscriptionDetails(id).map {
            case Right(psaDetails) => Ok(Json.toJson(psaDetails))
            case Left(e) => result(e)
          }
        case _ => Future.failed(new BadRequestException("No PSA Id in the header for get minimal details"))
      }

  }

  def removePsa: Action[PsaToBeRemovedFromScheme] = Action.async(parse.json[PsaToBeRemovedFromScheme]) {
    implicit request =>
      schemeConnector.removePSA(request.body) map {
        case Right(_) =>
          NoContent
        case Left(e) => result(e)
      }
  }

  def deregisterPsa(psaId: String): Action[AnyContent] = Action.async {
    implicit request =>
      schemeConnector.deregisterPSA(psaId).map {
        case Right(_) => NoContent
        case Left(e) => result(e)
      }
  }

  def updatePSA(psaId: String): Action[AnyContent] = Action.async {
    implicit request =>

       request.body.asJson match {
        case Some(jsValue) =>
          schemeService.updatePSA(psaId, jsValue).map {
            case Right(_) => Ok
            case Left(e) => result(e)
          }
        case _ => Future.failed(new BadRequestException("No PSA variation details in the header for psa update/variatiosn"))
      }
  }
}
