/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.Inject
import models.{MinimalDetails, AcceptedInvitation}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.{ControllerComponents, RequestHeader, AnyContent, Action}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import utils.{AuthRetrievals, ErrorHandler}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AssociationController @Inject()(
                                       associationConnector: AssociationConnector,
                                       retrievals: AuthRetrievals,
                                       cc: ControllerComponents
                                     ) extends BackendController(cc) with ErrorHandler {
  def getMinimalDetails: Action[AnyContent] = Action.async {
    implicit request =>
      retrieveIdAndTypeFromHeaders{ (idValue, idType, regime) =>
          associationConnector.getMinimalDetails(idValue, idType, regime).map {
            case Right(psaDetails) => Ok(Json.toJson(psaDetails))
            case Left(e) => result(e)
          }
      }
  }

  private def retrieveIdAndTypeFromHeaders(block: (String, String, String) => Future[Result])(implicit request: RequestHeader):Future[Result] = {
      (request.headers.get("psaId"), request.headers.get("pspId")) match {
        case (Some(id), None) => block(id, "psaid", "poda")
        case (None, Some(id)) => block(id, "pspid", "podp")
        case _ => Future.failed(new BadRequestException("No PSA or PSP Id in the header for get minimal details"))
      }
  }

  def acceptInvitation: Action[AnyContent] = Action.async {
    implicit request =>
      val feJson = request.body.asJson
      Logger.debug(s"[Accept-Invitation-Incoming-Payload]$feJson")

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

  def getEmail: Action[AnyContent] = Action.async {
    implicit request =>
      retrievals.getPsaId flatMap {
        case Some(psaId) => associationConnector.getMinimalDetails(psaId.id, "psaid", "poda") map {
          case Right(psaDetails) => Ok(psaDetails.email)
          case Left(e) => result(e)
        }
        case _ => Future.failed(new UnauthorizedException("Cannot retrieve enrolment PSAID"))
      }
  }

  def getName: Action[AnyContent] = Action.async {
    implicit request =>

      for {
        maybePsaId <- retrievals.getPsaId
        maybePsaDetails <- getPSAMinimalDetails(maybePsaId)
      } yield {
        maybePsaDetails match {
          case Right(psaDetails) =>
            psaDetails
              .name
              .map(n => Ok(n))
              .getOrElse(throw new NotFoundException("PSA minimal details contains neither individual or organisation name"))
          case Left(e) => result(e)
        }
      }

  }

  private def getPSAMinimalDetails(psaId: Option[PsaId])(
    implicit hc: HeaderCarrier, request: RequestHeader): Future[Either[HttpException, MinimalDetails]] = {
    psaId map {
      id =>
        associationConnector.getMinimalDetails(id.id, "psaid", "poda")
    } getOrElse {
      Future.failed(new UnauthorizedException("Cannot retrieve enrolment PSAID"))
    }
  }

}
