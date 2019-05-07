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
import config.FeatureSwitchManagementService
import connectors.RegistrationConnector
import models.registrationnoid.{OrganisationRegistrant, RegistrationNoIdIndividualRequest}
import models.{Organisation, SuccessResponse}
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException, Upstream4xxResponse}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import utils.ErrorHandler
import utils.validationUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class RegistrationController @Inject()(
                                        override val authConnector: AuthConnector,
                                        registerConnector: RegistrationConnector,
                                        cc: ControllerComponents,
                                        fs: FeatureSwitchManagementService
                                      ) extends BackendController(cc) with ErrorHandler with AuthorisedFunctions {

  def registerWithIdIndividual: Action[AnyContent] = Action.async {
    implicit request => {
      retrieveUser { user =>
        request.body.asJson match {
          case Some(jsBody) =>
            Try((jsBody \ "nino").convertTo[String]) match {
              case Success(nino) =>
                registerConnector.registerWithIdIndividual(nino, user, mandatoryPODSData()) map handleResponse
              case Failure(e) =>
                Logger.warn(s"Bad Request returned from frontend for Register With Id Individual $e")
                Future.failed(new BadRequestException(s"Bad Request returned from frontend for Register With Id Individual $e"))
            }
          case _ =>
            Future.failed(new BadRequestException("No request body received for register with Id Individual"))
        }
      }
    }
  }

  def registerWithIdOrganisation: Action[AnyContent] = Action.async {
    implicit request => {
      retrieveUser { user =>
        request.body.asJson match {
          case Some(jsBody) =>
            Try(((jsBody \ "utr").convertTo[String], jsBody.convertTo[Organisation])) match {
              case Success((utr, org)) =>
                val registerWithIdData = mandatoryPODSData(true).as[JsObject] ++ Json.obj("organisation" -> Json.toJson(org))

                registerConnector.registerWithIdOrganisation(utr, user, registerWithIdData) map handleResponse

              case Failure(e) =>
                Logger.warn(s"Bad Request returned from frontend for Register With Id Organisation $e")
                Future.failed(new BadRequestException(s"Bad Request returned from frontend for Register With Id Organisation $e"))
            }
          case _ =>
            Future.failed(new BadRequestException("No request body received for Organisation"))
        }
      }
    }
  }

  private def handleResponse: PartialFunction[Either[HttpException, SuccessResponse], Result] = {
    case Right(successResponse) => Ok(Json.toJson(successResponse))
    case Left(e: HttpException) => result(e)
  }

  private def mandatoryPODSData(requiresNameMatch: Boolean = false): JsValue = {
    Json.obj("regime" -> "PODA", "requiresNameMatch" -> requiresNameMatch, "isAnAgent" -> false)
  }

  def registrationNoIdOrganisation: Action[OrganisationRegistrant] = Action.async(parse.json[OrganisationRegistrant]) {
    implicit request => {
      retrieveUser { user =>
        registerConnector.registrationNoIdOrganisation(user, request.body) map {
          case Right(response) => Ok(Json.toJson(response))
          case Left(e) => result(e)
        }
      }
    }
  }

  def registrationNoIdIndividual: Action[RegistrationNoIdIndividualRequest] = Action.async(parse.json[RegistrationNoIdIndividualRequest]) {
    implicit request => {
      retrieveUser { user =>
        registerConnector.registrationNoIdIndividual(user, request.body) map {
          case Right(response) => Ok(Json.toJson(response))
          case Left(e) => result(e)
        }
      }
    }
  }


  private def retrieveUser(fn: models.User => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    authorised().retrieve(Retrievals.externalId and Retrievals.affinityGroup) {
      case Some(externalId) ~ Some(affinityGroup) =>
        fn(models.User(externalId, affinityGroup))
      case _ =>
        Future.failed(Upstream4xxResponse("Not authorized", UNAUTHORIZED, UNAUTHORIZED))
    }
  }
}
