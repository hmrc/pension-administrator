/*
 * Copyright 2019 HM Revenue & Customs
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
import connectors.SchemeConnector
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import utils.ErrorHandler

import scala.concurrent.ExecutionContext

class DeregistrationController @Inject()(
                                          schemeConnector: SchemeConnector,
                                          cc: ControllerComponents
                                        )(implicit val ec: ExecutionContext) extends BaseController(cc) with ErrorHandler {
  private[controllers] def parseSchemes(jsValue: JsValue): Seq[String] = {
    (JsPath \ "schemeDetail") (jsValue).flatMap (
      _.validate[JsArray] match {
        case JsSuccess(jsArray, _) =>
          jsArray.value.map(scheme => (JsPath \ "schemeStatus") (scheme).head.as[String])
        case JsError(ex) =>
          throw new RuntimeException("Unable to read schemes:" + ex.toString)
      }
    )
  }

  def canDeregister(psaId: String): Action[AnyContent] = Action.async {
    implicit request =>
      schemeConnector.listOfSchemes(psaId).map {
        case Right(jsValue) =>
          Ok(Json.toJson(!parseSchemes(jsValue).exists(_ != "Wound-up")))
        case Left(e) =>
          result(e)
      }
  }
}
