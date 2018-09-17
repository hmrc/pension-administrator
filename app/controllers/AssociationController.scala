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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import utils.ErrorHandler
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AssociationController @Inject()(
                                       associationConnector: AssociationConnector
                                     ) extends BaseController with ErrorHandler {
  def getMinimalDetails: Action[AnyContent] = Action.async {
    implicit request =>
      val psaId = request.headers.get("psaId")
      psaId match {
        case Some(id) =>
          associationConnector.getPSAMinimalDetails(id).map {
            case Right(psaDetails) => Ok(Json.toJson(psaDetails))
            case Left(e) => result(e)
          }
        case _ => Future.failed(new BadRequestException("Bad Request with no request body returned for register PSA"))
      }

  }
}
