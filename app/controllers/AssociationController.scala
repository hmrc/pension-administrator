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
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.{AuthRetrievals, ErrorHandler}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class AssociationController @Inject()(
                                       associationConnector: AssociationConnector,
                                       minimalDetailsCacheRepository: MinimalDetailsCacheRepository,
                                       retrievals: AuthRetrievals,
                                       noEnrollmentAuth: actions.NoEnrolmentAuthAction,
                                       val authConnector: AuthConnector,
                                       cc: ControllerComponents
                                     )(implicit val ec: ExecutionContext)
                                      extends BackendController(cc) with ErrorHandler  with AuthorisedFunctions {

  private val logger = Logger(classOf[AssociationController])

  def getMinimalDetails: Action[AnyContent] = noEnrollmentAuth.async {
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

  private def retrieveIdAndTypeFromHeaders(block: (String, String, String) => Future[Result])(implicit request: RequestHeader):Future[Result] = {
      (request.headers.get("psaId"), request.headers.get("pspId")) match {
        case (Some(id), None) => block(id, "psaid", "poda")
        case (None, Some(id)) => block(id, "pspid", "podp")
        case _ => Future.failed(new BadRequestException("No PSA or PSP Id in the header for get minimal details"))
      }
  }

  def acceptInvitation: Action[AnyContent] = noEnrollmentAuth.async {
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

  def getEmail: Action[AnyContent] = noEnrollmentAuth.async {
    implicit request =>
      retrievals.getPsaId flatMap {
        case Some(psaId) =>
          getMinimalDetail(psaId.id, "psaid", "poda") map {
          case Right(psaDetails) => Ok(psaDetails.email)
          case Left(e) => result(e)
        }
        case _ => Future.failed(new UnauthorizedException("Cannot retrieve enrolment PSAID"))
      }
  }

  def getName: Action[AnyContent] = noEnrollmentAuth.async {
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
        getMinimalDetail(id.id, "psaid", "poda")
    } getOrElse {
      Future.failed(new UnauthorizedException("Cannot retrieve enrolment PSAID"))
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
