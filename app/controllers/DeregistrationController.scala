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

package controllers

import com.google.inject.Inject
import connectors.SchemeConnector
import controllers.actions.PsaEnrolmentAuthAction
import models.{ListOfSchemes, SchemeDetails}
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class DeregistrationController @Inject()(
                                          schemeConnector: SchemeConnector,
                                          cc: ControllerComponents,
                                          authAction: PsaEnrolmentAuthAction
                                        )(implicit val ec: ExecutionContext)
  extends BackendController(cc)
    with ErrorHandler {

  def canDeregisterSelf: Action[AnyContent] = authAction.async {
    implicit request =>
      schemeConnector.listOfSchemes(request.psaId.id).flatMap {
        case Right(jsValue) =>

          jsValue.validate[ListOfSchemes] match {
            case JsSuccess(listOfSchemes, _) =>
              val schemes: Seq[SchemeDetails] = listOfSchemes.schemeDetails.getOrElse(List.empty)
              val canDeregister: Boolean =
                schemes == List.empty || schemes.forall(s => s.schemeStatus == "Rejected" || s.schemeStatus == "Wound-up")
              otherPsaAttached(canDeregister, schemes, request.psaId.id).map { list =>
                val isOtherPsaAttached: Boolean = list.contains(true)
                Ok(Json.obj("canDeregister" -> JsBoolean(canDeregister),
                  "isOtherPsaAttached" -> JsBoolean(isOtherPsaAttached)))
              }
            case JsError(errors) => throw JsResultException(errors)
          }
        case Left(e) =>
          Future.successful(result(e))
      }
  }

  private def otherPsaAttached(canDeregister: Boolean, schemes: Seq[SchemeDetails], psaId: String)
                              (implicit hc: HeaderCarrier): Future[Seq[Boolean]] =
    if (!canDeregister && schemes.exists(_.schemeStatus == "Open")) {
      Future.sequence(schemes.filter(_.schemeStatus == "Open").map { scheme =>
        schemeConnector.getSchemeDetails(psaId, "srn", scheme.referenceNumber).map {
          case Right(jsValue) =>
            (jsValue \ "psaDetails").as[Seq[PsaDetails]](Reads.seq[PsaDetails]).exists(_.id != psaId)
          case Left(_) => false
        }
      })
    } else {
      Future(List.empty[Boolean])
    }
}

case class PsaDetails(id: String)

object PsaDetails {
  implicit val format: Format[PsaDetails] = Json.format[PsaDetails]
}
