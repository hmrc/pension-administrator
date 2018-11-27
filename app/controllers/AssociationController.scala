/*
 * Copyright 2018 HM Revenue & Customs
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
import connectors.AssociationConnector
import models.{AcceptedInvitation, PSAMinimalDetails}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import utils.{AuthRetrievals, ErrorHandler}

import scala.concurrent.{ExecutionContext, Future}

class AssociationController @Inject()(
                                       associationConnector: AssociationConnector,
                                       retrievals: AuthRetrievals,
                                       cc: ControllerComponents
                                     )(implicit ec: ExecutionContext) extends BackendController(cc) with ErrorHandler {
  def getMinimalDetails: Action[AnyContent] = Action.async {
    implicit request =>
      val psaId = request.headers.get("psaId")
      psaId match {
        case Some(id) =>
          getPSAMinimalDetails(id).map {
            case Right(psaDetails) => Ok(Json.toJson(psaDetails))
            case Left(e) => result(e)
          }
        case _ => Future.failed(new BadRequestException("No PSA Id in the header for get minimal details"))
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
        case Some(psaId) => getPSAMinimalDetails(psaId.id) map {
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

  private def getPSAMinimalDetails(id: String)(implicit hc: HeaderCarrier): Future[Either[HttpException, PSAMinimalDetails]] = {
    associationConnector.getPSAMinimalDetails(PsaId(id))
  }

  private def getPSAMinimalDetails(psaId: Option[PsaId])(implicit hc: HeaderCarrier): Future[Either[HttpException, PSAMinimalDetails]] = {
    psaId map {
      id =>
        associationConnector.getPSAMinimalDetails(id)
    } getOrElse {
      Future.failed(new UnauthorizedException("Cannot retrieve enrolment PSAID"))
    }
  }

}
