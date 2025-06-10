/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.actions

import connectors.SchemeConnector
import models.SchemeReferenceNumber
import play.api.Logging
import play.api.mvc.Results.Forbidden
import play.api.mvc.{ActionFunction, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import utils.ErrorHandler

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PsaSchemeActionImpl (srn:SchemeReferenceNumber, schemeConnector: SchemeConnector)
                          (implicit val executionContext: ExecutionContext)
  extends ActionFunction[PsaAuthRequest, PsaAuthRequest] with BackendHeaderCarrierProvider with Logging with ErrorHandler {


  override def invokeBlock[A](request: PsaAuthRequest[A], block: PsaAuthRequest[A] => Future[Result]): Future[Result] = {

    val isAssociated = schemeConnector.checkForAssociation(
      Left(request.psaId),
      srn
    )(using hc(using request), executionContext)

    isAssociated.flatMap {
      case Right(true) => block(request)
      case Right(false) =>
        logger.warn("Potentially prevented unauthorised access")
        Future.successful(Forbidden("PSA is not associated with pension scheme"))
      case Left(e) =>
        logger.error("Is associated call failed", e)
        recoverFromError(e)
    }
  }
}

class PsaSchemeAuthAction @Inject()(schemeDetailsConnector: SchemeConnector)(implicit ec: ExecutionContext){
  def apply(srn: SchemeReferenceNumber): ActionFunction[PsaAuthRequest, PsaAuthRequest] =
    new PsaSchemeActionImpl(srn, schemeDetailsConnector)
}
