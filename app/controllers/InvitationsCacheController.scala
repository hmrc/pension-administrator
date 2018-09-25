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
import play.api.Configuration
import play.api.mvc.{Action, AnyContent, Result}
import repositories.InvitationsCacheRepository
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.{ExecutionContext, Future}

class InvitationsCacheController @Inject()(
                                            config: Configuration,
                                            repository: InvitationsCacheRepository,
                                            val authConnector: AuthConnector
                                          ) extends BaseController with AuthorisedFunctions {

  private val maxSize: Int = config.underlying.getInt("mongodb.pension-administrator-cache.maxSize")


  def add: Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        request.body.asJson.map {
          jsValue =>
            request.headers.get("inviteePsaId").map { inviteePsaId =>
              request.headers.get("pstr").map { pstr =>
                repository.insert(inviteePsaId, pstr, jsValue)
                  .map(_ => Ok)
              }.getOrElse(Future.successful(BadRequest))
            }.getOrElse(Future.successful(BadRequest))
        } getOrElse Future.successful(BadRequest)
      }
  }

  def get: Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        request.headers.get("inviteePsaId").flatMap { inviteePsaId =>
          request.headers.get("pstr").map { pstr =>
            getByMap(Map("inviteePsaId" -> inviteePsaId, "pstr" -> pstr))
          }
        }.getOrElse(Future.successful(BadRequest))
      }
  }

  private def getByMap(map: Map[String, String])(implicit ec: ExecutionContext): Future[Result] = {
    repository.getByKeys(map).map { response =>
      response.map {
        Ok(_)
      }
        .getOrElse(NotFound)
    }
  }

  def getForScheme: Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        request.headers.get("pstr").map { pstr =>
          getByMap(Map("pstr" -> pstr))
        }.getOrElse(Future.successful(BadRequest))
      }
  }

  def getForInvitee: Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        request.headers.get("inviteePsaId").map { inviteePsaId =>
          getByMap(Map("inviteePsaId" -> inviteePsaId))
        }.getOrElse(Future.successful(BadRequest))
      }
  }

  def remove: Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        request.headers.get("inviteePsaId").map { inviteePsaId =>
          request.headers.get("pstr").map { pstr =>
            repository.remove(inviteePsaId, pstr).map(_ => Ok)
          }.getOrElse(Future.successful(BadRequest))
        }.getOrElse(Future.successful(BadRequest))
      }
  }
}
