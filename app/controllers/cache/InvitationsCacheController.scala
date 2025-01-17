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

package controllers.cache

import com.google.inject.Inject
import controllers.actions.PsaPspEnrolmentAuthAction
import models.Invitation
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import repositories.InvitationsCacheRepository
import service.MongoDBFailedException
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class InvitationsCacheController @Inject()(
                                            repository: InvitationsCacheRepository,
                                            val authConnector: AuthConnector,
                                            cc: ControllerComponents,
                                            authAction: PsaPspEnrolmentAuthAction
                                          )(implicit val ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  def add: Action[AnyContent] = authAction.async {
    implicit request =>
        request.body.asJson.map {
          jsValue =>
            jsValue.validate[Invitation].fold(
              _ => Future.failed(new BadRequestException("not valid value for PSA Invitation")),
              value => repository.upsert(value).map(_ => Created).recoverWith {
                case exception: Exception =>
                  throw MongoDBFailedException(s"""Could not perform DB operation: ${exception.getMessage}""")
              }
            )

        } getOrElse Future.failed(
          new BadRequestException("Bad Request with no request body returned for PSA Invitation")
        )
  }

  private def getByMap(map: Map[String, String]): Future[Result] = {
    repository.getByKeys(map).map { response =>
      response.map { invitationsList =>
        Ok(Json.toJson(invitationsList))
      }
        .getOrElse(NotFound)
    }
  }

  def get: Action[AnyContent] = authAction.async {
    implicit request =>
        request.headers.get("inviteePsaId").flatMap { inviteePsaId =>
          request.headers.get("pstr").map { pstr =>
            getByMap(Map("inviteePsaId" -> inviteePsaId, "pstr" -> pstr))
          }
        }.getOrElse(Future.successful(BadRequest))
  }

  def getForScheme: Action[AnyContent] = authAction.async {
    implicit request =>
        request.headers.get("pstr").map(pstr => getByMap(Map("pstr" -> pstr)))
          .getOrElse(Future.successful(BadRequest))
  }

  def getForInvitee: Action[AnyContent] = authAction.async {
    implicit request =>
        request.headers.get("inviteePsaId").map(inviteePsaId => getByMap(Map("inviteePsaId" -> inviteePsaId)))
          .getOrElse(Future.successful(BadRequest))
  }

  def remove: Action[AnyContent] = authAction.async {
    implicit request =>
        request.headers.get("inviteePsaId").flatMap(
          inviteePsaId =>
            request.headers.get("pstr").map(
              pstr =>
                repository.remove(Map("inviteePsaId" -> inviteePsaId, "pstr" -> pstr)).map(_ => Ok)
            )
        ).getOrElse(Future.successful(BadRequest))
  }
}
