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

package controllers.cache

import play.api.libs.json.JodaWrites._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.api.{Configuration, Logger}
import repositories.ManageCacheRepository
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class PensionAdministratorCacheController(
                                                    config: Configuration,
                                                    repository: ManageCacheRepository,
                                                    val authConnector: AuthConnector,
                                                    cc: ControllerComponents
                                            ) extends BackendController(cc) with AuthorisedFunctions {

  private val maxSize: Int = config.underlying.getInt("mongodb.pension-administrator-cache.maxSize")

  def save(id: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        request.body.asJson.map {
          jsValue =>
            repository.upsert(id, jsValue)
              .map(_ => Ok)
        } getOrElse Future.successful(EntityTooLarge)
      }
  }

  def get(id: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        Logger.debug("controllers.cache.PensionAdministratorCacheController.get: Authorised Request " + id)
        repository.get(id).map { response =>
          Logger.debug(s"controllers.cache.PensionAdministratorCacheController.get: Response for request Id $id is $response")
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
        Logger.debug("controllers.cache.PensionAdministratorCacheController.get: Authorised Request " + id)
        repository.getLastUpdated(id).map { response =>
          Logger.debug("controllers.cache.PensionAdministratorCacheController.get: Response " + response)
          response.map { date => Ok(Json.toJson(date)) }
            .getOrElse(NotFound)
        }
      }
  }

  def remove(id: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        repository.remove(id).map(_ => Ok)
      }
  }
}