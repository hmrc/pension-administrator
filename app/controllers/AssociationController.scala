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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import utils.{AuthRetrievals, ErrorHandler}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AssociationController @Inject()(
                                       associationConnector: AssociationConnector,
                                       retrievals: AuthRetrievals
                                     ) extends BaseController with ErrorHandler {
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

      retrievals.getPsaId flatMap { psaId =>
        retrievals.getAffinityGroup flatMap { affinityGroup =>

          psaId map { id =>

            affinityGroup map { userGroup =>

              getPSAMinimalDetails(id.id) flatMap {
                case Right(psaDetails) =>

                  val name = userGroup match {
                    case AffinityGroup.Individual => psaDetails.individualDetails map { _.fullName }
                    case AffinityGroup.Organisation => psaDetails.organisationName
                    case _ => None
                  }

                  name match {
                    case Some(nm) => Future.successful(Ok(nm))
                    case _ => Future.failed(new NotFoundException("Cannot find name"))
                  }

                case Left(e) => Future.successful(result(e))
              }

            } getOrElse Future.failed(new UnauthorizedException("Cannot retrieve user group"))
          } getOrElse Future.failed(new UnauthorizedException("Cannot retrieve enrolment PSAID"))

        }
      }

  }

  private def getPSAMinimalDetails(id: String)(implicit hc: HeaderCarrier): Future[Either[HttpException, PSAMinimalDetails]] = {
    associationConnector.getPSAMinimalDetails(PsaId(id))
  }

}
