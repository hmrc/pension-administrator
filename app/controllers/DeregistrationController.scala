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
import play.api.libs.json.{Json, OFormat}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import utils.ErrorHandler
import utils.validationUtils._

import scala.concurrent.ExecutionContext

class DeregistrationController @Inject()(
                                          schemeConnector: SchemeConnector
                                        )(implicit val ec: ExecutionContext) extends BaseController with ErrorHandler {

  private case class SchemeDetail(name: String, referenceNumber: String, schemeStatus: String, openDate: Option[String],
                                  pstr: Option[String] = None, relationShip: Option[String], underAppeal: Option[String] = None)

  private object SchemeDetail {
    implicit val format: OFormat[SchemeDetail] = Json.format[SchemeDetail]
  }

  private case class ListOfSchemes(processingDate: String, totalSchemesRegistered: String,
                                   schemeDetail: Option[List[SchemeDetail]] = None)

  private object ListOfSchemes {
    implicit val format: OFormat[ListOfSchemes] = Json.format[ListOfSchemes]
  }

  def canDeregister(psaId: String): Action[AnyContent] = Action.async {
    implicit request => {

      schemeConnector.listOfSchemes(psaId).map {
        case Right(jsValue) =>
          val schemes = jsValue.convertTo[ListOfSchemes].schemeDetail.toSeq.flatten
              .filter(_.schemeStatus != "Wound-up")
          Ok(Json.toJson(schemes.isEmpty))
        case Left(e) =>
          result(e)
      }

    }
  }
}
