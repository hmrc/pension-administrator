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
import connectors.RegistrationConnector
import controllers.actions.NoEnrolmentAuthAction
import models.Organisation
import models.registrationnoid.{OrganisationRegistrant, RegistrationNoIdIndividualRequest}
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.http.{BadRequestException, HttpException}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandler
import utils.UtrHelper.stripUtr
import utils.ValidationUtils._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class RegistrationController @Inject()(
                                        registerConnector: RegistrationConnector,
                                        cc: ControllerComponents,
                                        authAction: NoEnrolmentAuthAction
                                      )(implicit val ec: ExecutionContext) extends BackendController(cc) with ErrorHandler {

  private val logger = Logger(classOf[RegistrationController])

  def registerWithIdIndividual: Action[AnyContent] = authAction.async {
    implicit request => {
      request.body.asJson match {
        case Some(jsBody) =>
          Try((jsBody \ "nino").convertTo[String]) match {
            case Success(nino) =>
              registerConnector.registerWithIdIndividual(nino, request.externalId, mandatoryPODSData()) map handleResponse
            case Failure(e) =>
              logger.warn(s"Bad Request returned from frontend for Register With Id Individual $e")
              Future.failed(new BadRequestException(s"Bad Request returned from frontend for Register With Id Individual $e"))
          }
        case _ =>
          Future.failed(new BadRequestException("No request body received for register with Id Individual"))
      }
    }
  }

  def registerWithIdOrganisation: Action[AnyContent] = authAction.async {
    implicit request => {
      request.body.asJson match {
        case Some(jsBody) =>
          Try((stripUtr(Some("UTR"), Some((jsBody \ "utr").convertTo[String])), jsBody.convertTo[Organisation])) match {
            case Success((Some(utr), org)) =>
              val orgWithInvalidCharactersRemoved = org.copy(organisationName =
                org.organisationName.replaceAll("""[^a-zA-Z0-9- '&()\/]+""", ""))
              val registerWithIdData = mandatoryPODSData(true).as[JsObject] ++
                Json.obj("organisation" -> Json.toJson(orgWithInvalidCharactersRemoved))
              registerConnector.registerWithIdOrganisation(utr, request.externalId, registerWithIdData) map handleResponse
            case Success((None, _)) =>
              logger.warn("Bad Request returned for Register With Id Organisation - no UTR found")
              Future.failed(new BadRequestException("Bad Request returned for Register With Id Organisation - no UTR found"))
            case Failure(e) =>
              logger.warn(s"Bad Request returned for Register With Id Organisation $e")
              Future.failed(new BadRequestException(s"Bad Request returned for Register With Id Organisation $e"))
          }
        case _ =>
          Future.failed(new BadRequestException("No request body received for Organisation"))
      }
    }
  }

  private def handleResponse: PartialFunction[Either[HttpException, JsValue], Result] = {
    case Right(jsValue) => Ok(jsValue)
    case Left(e: HttpException) => result(e)
  }

  private def mandatoryPODSData(requiresNameMatch: Boolean = false): JsValue = {
    Json.obj("regime" -> "PODA", "requiresNameMatch" -> requiresNameMatch, "isAnAgent" -> false)
  }

  def registrationNoIdOrganisation: Action[OrganisationRegistrant] = authAction.async(parse.json[OrganisationRegistrant]) {
    implicit request => {
      registerConnector.registrationNoIdOrganisation(request.externalId, request.body) map {
        case Right(response) => Ok(Json.toJson(response))
        case Left(e) => result(e)
      }
    }
  }

  def registrationNoIdIndividual: Action[RegistrationNoIdIndividualRequest] = authAction.async(parse.json[RegistrationNoIdIndividualRequest]) {
    implicit request => {
      registerConnector.registrationNoIdIndividual(request.externalId, request.body) map {
        case Right(response) => Ok(Json.toJson(response))
        case Left(e) => result(e)
      }
    }
  }

}
