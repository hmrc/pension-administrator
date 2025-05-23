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

import controllers.actions.NoEnrolmentAuthAction
import play.api.Logger
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.ManageCacheRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

abstract class PensionAdministratorCacheController(
                                                    repository: ManageCacheRepository,
                                                    cc: ControllerComponents,
                                                    authAction: NoEnrolmentAuthAction
                                                  )(implicit val ec: ExecutionContext) extends BackendController(cc){

  private val logger = Logger(classOf[PensionAdministratorCacheController])

  def saveSelf: Action[AnyContent] = authAction.async {
    implicit request =>
      request.body.asJson.map {
        jsValue =>
          repository.upsert(request.externalId, jsValue)
            .map(_ => Ok)
      } getOrElse Future.successful(EntityTooLarge)
  }

  def getSelf: Action[AnyContent] = authAction.async { request =>
    val id = request.externalId
    logger.debug("controllers.cache.PensionAdministratorCacheController.get: Authorised Request " + id)
    repository.get(id).map { response =>
      logger.debug(s"controllers.cache.PensionAdministratorCacheController.get: Response for request Id $id is $response")
      response.map {
          Ok(_)
        }
        .getOrElse(NotFound)
    }
  }

  def removeSelf: Action[AnyContent] = authAction.async { request =>
    repository.remove(request.externalId).map(_ => Ok)
  }
}
