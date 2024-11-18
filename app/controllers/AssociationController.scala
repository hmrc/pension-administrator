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

import connectors.AssociationConnector
import models.{AcceptedInvitation, MinimalDetails}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc._
import repositories.MinimalDetailsCacheRepository
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandler

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AssociationController @Inject()(
                                       associationConnector: AssociationConnector,
                                       minimalDetailsCacheRepository: MinimalDetailsCacheRepository,
                                       psaPspAuth: actions.PsaPspEnrolmentAuthAction,
                                       psaAuth: actions.PsaEnrolmentAuthAction,
                                       cc: ControllerComponents
                                     )(implicit val ec: ExecutionContext)
                                      extends BackendController(cc) with ErrorHandler {

  private val logger = Logger(classOf[AssociationController])

  def getMinimalDetails: Action[AnyContent] = psaPspAuth.async {
    implicit request =>
      retrieveIdAndTypeFromHeaders{ (idValue, idType, regime) =>
        getMinimalDetail(idValue, idType, regime)
         .map {
            case Right(psaDetails) => Ok(Json.toJson(psaDetails))
            case Left(ex) if ex.responseCode == NOT_FOUND => NotFound("no match found")
            case Left(e) => result(e)
          }
      }
  }

  def getMinimalDetailsSelf: Action[AnyContent] = psaPspAuth.async { implicit request =>
    getRequiredUserParametersFromRequest.fold(error => Future.successful(BadRequest(error)), { case (id, idType, regime) =>
      getMinimalDetail(id, idType, regime)
        .map {
          case Right(psaDetails) => Ok(Json.toJson(psaDetails))
          case Left(ex) if ex.responseCode == NOT_FOUND => NotFound("no match found")
          case Left(e) => result(e)
        }
    })

  }

  def getEmailInvitation: Action[AnyContent] = psaPspAuth.async { implicit request =>

    def withRegime(id: String, idType: String, regime: String, name: String) = {
      getMinimalDetail(id, idType, regime) map {
        case Right(minimalDetails) =>
          val nameMatches = minimalDetails.name.contains(name)
          if(nameMatches) {
            Ok(minimalDetails.email)
          } else {
            Forbidden("Provided user's name doesn't match with stored user's name")
          }

        case Left(e) => result(e)
      }
    }

    def withParameters(id: String, idType: String, name: String) = {
      val regime = idType match {
        case "psaid" => Some("poda")
        case "pspid" => Some("podp")
        case _ => None
      }
      regime.map { withRegime(id, idType, _, name) }
        .getOrElse(Future.successful(BadRequest("idType must be either psaid or pspid")))
    }

    (request.headers.get("id"), request.headers.get("idType"), request.headers.get("name")) match {
      case (Some(id), Some(idType), Some(name)) => withParameters(id, idType, name)
      case (id, idType, name) =>
        def printMissingParam(paramName: String, param: Option[String]) = if(param.isEmpty) " " + paramName else ""
        val missingParamErrors = Seq("id" -> id, "idType" -> idType, "name" -> name).map { case (name, param) => printMissingParam(name, param)}.mkString
        Future.successful(
          BadRequest(s"Missing headers:$missingParamErrors")
        )
    }
  }

  private def getRequiredUserParametersFromRequest(implicit req: actions.PsaPspAuthRequest[_]):Either[String, (String, String, String)] = {
    val loggedInAsPsaOpt = Try(req.headers.get("loggedInAsPsa").map(_.toBoolean)).toOption.flatten
    def process(loggedInAsPsa: Boolean) = {

      if(loggedInAsPsa) {
        req.psaId match {
          case Some(psaId) => Right((psaId.id, psaId.name, "poda"))
          case None => Left("PsaId credentials not found")
        }
      } else {
        req.pspId match {
          case Some(pspId) => Right((pspId.id, pspId.name, "podp"))
          case None => Left("PspId credentials not found")
        }
      }
    }

    loggedInAsPsaOpt.map(process)
      .getOrElse(Left("No header present loggedInAsPsa [true, false] - Should indicate if user is logged in as a PSA or PSP"))

  }

  private def retrieveIdAndTypeFromHeaders(block: (String, String, String) => Future[Result])(implicit request: RequestHeader):Future[Result] = {
      (request.headers.get("psaId"), request.headers.get("pspId")) match {
        case (Some(id), None) => block(id, "psaid", "poda")
        case (None, Some(id)) => block(id, "pspid", "podp")
        case _ => Future.failed(new BadRequestException("No PSA or PSP Id in the header for get minimal details"))
      }
  }

  def acceptInvitation: Action[AnyContent] = psaPspAuth.async {
    implicit request =>
        val feJson = request.body.asJson
        logger.debug(s"[Accept-Invitation-Incoming-Payload]$feJson")

        feJson match {
          case Some(acceptedInvitationJsValue) =>
            acceptedInvitationJsValue.validate[AcceptedInvitation].fold(
              _ =>
                Future.failed(
                  new BadRequestException("Bad request received from frontend for accept invitation")
                ),
              acceptedInvitation =>
                associationConnector.acceptInvitation(acceptedInvitation).map {
                  case Right(_) => Created
                  case Left(e) => result(e)
                }
            )
          case None =>
            Future.failed(new BadRequestException("No Request Body received for accept invitation"))

        }
  }

  def getEmail: Action[AnyContent] = psaAuth.async {
    implicit request =>
      getMinimalDetail(request.psaId.id, "psaid", "poda") map {
        case Right(psaDetails) => Ok(psaDetails.email)
        case Left(e) => result(e)
      }
  }

  def getName: Action[AnyContent] = psaAuth.async {
    implicit request =>
      getMinimalDetail(request.psaId.id, "psaid", "poda").map {
        case Right(psaDetails) =>
          psaDetails
            .name
            .map(n => Ok(n))
            .getOrElse(throw new NotFoundException("PSA minimal details contains neither individual or organisation name"))
        case Left(e) => result(e)
      }
  }

  private def getMinimalDetail(idValue: String, idType: String, regime: String)(
    implicit hc: HeaderCarrier, request: RequestHeader): Future[Either[HttpException, MinimalDetails]] = {
            minimalDetailsCacheRepository.get(idValue).flatMap {
              case Some(response)=>
                response.validate[MinimalDetails](MinimalDetails.defaultReads) match {
                  case JsSuccess(value, _) => Future.successful(Right(value))
                  case JsError(_) => getAndCacheMinimalDetails(idValue, idType, regime)
                }
              case _ => getAndCacheMinimalDetails(idValue, idType, regime)
            }
  }

  private def getAndCacheMinimalDetails(idValue: String, idType: String, regime: String)(
  implicit hc: HeaderCarrier, request: RequestHeader):Future[Either[HttpException, MinimalDetails]]={

    associationConnector.getMinimalDetails(idValue, idType, regime) flatMap  {
            case Right(psaDetails) =>
              minimalDetailsCacheRepository.upsert(idValue,Json.toJson(psaDetails)).map(_ =>
                    Right(psaDetails)
              )
            case Left(e) => Future.successful(Left(e))
    }
  }

}
