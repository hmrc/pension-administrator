/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.admin

import javax.inject.Inject
import models.FeatureToggleName
import play.api.libs.json.{JsBoolean, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import service.FeatureToggleService
import utils.ErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class FeatureToggleController @Inject()(
                                          cc: ControllerComponents,
                                          featureToggleService: FeatureToggleService
                                        )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with ErrorHandler {

  def getAll: Action[AnyContent] = Action.async {
    _ =>
      featureToggleService.getAll.map(
        toggles =>
          Ok(Json.toJson(toggles.sortWith(_.name.asString < _.name.asString)))
        )
  }

  def get(toggleName: FeatureToggleName): Action[AnyContent] = Action.async {
    _ =>
      featureToggleService.get(toggleName).map(
        toggle =>
          Ok(Json.toJson(toggle))
      )
  }

  def put(toggleName: FeatureToggleName): Action[AnyContent] = Action.async {
    request => {
      request.body.asJson match {
        case Some(JsBoolean(enabled)) =>
          featureToggleService.set(toggleName, enabled).map(_ => NoContent)
        case _ =>
          Future.successful(BadRequest)
      }
    }
  }

  def createToggle: Action[AnyContent] = Action.async {
    request => {

     request.body.asJson match {
       case Some(body) =>
         val toggleName = (body \ "toggleName").as[String]
         val toggleDescription = (body \ "toggleDescription").as[String]
         val isEnabled = (body \ "isEnabled").as[Boolean]

       //featureToggleService.createToggle(toggleName, toggleDescription, isEnabled).map(_ => NoContent)

       case None =>
         Future.successful(NotFound)
     }

      request.body.asJson match {
        case Some(JsBoolean(enabled)) =>
          //featureToggleService.createToggle(toggleName, isEnabled).map(_ => NoContent)
          Future.successful(Ok)
        case _ =>
          Future.successful(BadRequest)
      }
    }
  }
}
