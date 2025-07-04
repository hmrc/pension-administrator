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

import com.google.inject.Inject
import play.api.Logging
import play.api.http.Status.UNAUTHORIZED
import play.api.mvc.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}


case class AuthRequestWithNoEnrollment[A](request: Request[A], externalId: String) extends WrappedRequest[A](request)

class NoEnrolmentAuthAction @Inject()(
                                       override val authConnector: AuthConnector,
                                       val parser: BodyParsers.Default
                                     )(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[AuthRequestWithNoEnrollment, AnyContent]
    with ActionFunction[Request, AuthRequestWithNoEnrollment]
    with AuthorisedFunctions
    with Logging {


  override def invokeBlock[A](request: Request[A], block: AuthRequestWithNoEnrollment[A] => Future[Result]): Future[Result] =
    invoke(request, block)(using HeaderCarrierConverter.fromRequest(request))

  def invoke[A](request: Request[A], block: AuthRequestWithNoEnrollment[A] => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {

    authorised().retrieve(Retrievals.externalId) {
      case Some(externalId) =>
        block(AuthRequestWithNoEnrollment(request, externalId))
      case _ => Future.failed(UpstreamErrorResponse("Not authorized", UNAUTHORIZED, UNAUTHORIZED))
    }
  }
}
