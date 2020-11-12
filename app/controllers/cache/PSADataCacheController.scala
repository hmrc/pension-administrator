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

import com.google.inject.Inject
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.api.{Configuration, Logger}
import repositories.PSADataCacheRepository
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PSADataCacheController @Inject()(
                                        config: Configuration,
                                        repository: PSADataCacheRepository,
                                        val authConnector: AuthConnector,
                                        cc: ControllerComponents
                                      ) extends BackendController(cc) with AuthorisedFunctions {
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
        Logger.debug(message = "controllers.cache.PSADataCacheController.get: Authorised Request " + id)
        repository.get(id).map { response =>
          Logger.debug(message = s"controllers.cache.PSADataCacheController.get: Response for request Id $id is $response")
          response.map {
            Ok(_)
          }
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