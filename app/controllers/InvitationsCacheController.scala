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
import models.{Invitation, PSAMinimalDetails}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Logger}
import repositories.InvitationsCacheRepositoryImpl
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class InvitationsCacheController @Inject()(
                                                    config: Configuration,
                                                    repository: InvitationsCacheRepositoryImpl,
                                                    val authConnector: AuthConnector
                                                  ) extends BaseController with AuthorisedFunctions {

  private val maxSize: Int = config.underlying.getInt("mongodb.pension-administrator-cache.maxSize")

  def add: Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        request.body.asJson.map {
          jsValue =>

            jsValue.validate[Invitation].fold(
              _=> Future.failed(new BadRequestException("not valid value for PSA Invitation")),
              value => repository.insert(value).map(_ => Created)
            )

        } getOrElse Future.successful(BadRequest("Bad Request with no request body returned for PSA Invitation"))
      }
  }

  def get: Action[AnyContent] = Action.async {
    implicit request =>
      val inviteePsaId = ""
      val pstr = ""
      authorised() {
        repository.get(inviteePsaId, pstr).map { response =>
          response.map {
            Ok(_)
          }
            .getOrElse(NotFound)
        }
      }
  }

  def getForScheme: Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        val pstr: String = ""
        repository.getForScheme(pstr).map { response =>
          response.map {
            Ok(_)
          }
            .getOrElse(NotFound)
        }
      }
  }

  def getForInvitee: Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        val inviteePsaId: String = ""
        repository.getForInvitee(inviteePsaId).map { response =>
          response.map {
            Ok(_)
          }
            .getOrElse(NotFound)
        }
      }
  }


  def lastUpdated(id: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        Logger.debug("controllers.InvitationsCacheController.get: Authorised Request " + id)
        repository.getLastUpdated(id).map { response =>
          Logger.debug("controllers.InvitationsCacheController.get: Response " + response)
          response.map { date => Ok(Json.toJson(date)) }
            .getOrElse(NotFound)
        }
      }
  }

  def remove: Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        val inviteePsaId = ""
        val pstr = ""
        repository.remove(inviteePsaId, pstr).map(_ => Ok)
      }
  }
}
