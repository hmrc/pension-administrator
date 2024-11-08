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

package controllers.actions

import com.google.inject.{ImplementedBy, Inject}
import config.Constants
import config.Constants._
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{ActionFunction, _}
import service.EnrolmentLoggingService
import uk.gov.hmrc.auth.core.retrieve.Retrievals.externalId
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, _}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

@ImplementedBy(classOf[PsaPspEnrolmentAuthAction])
trait AuthAction
  extends ActionBuilder[AuthRequest, AnyContent]
    with ActionFunction[Request, AuthRequest]
    with AuthorisedFunctions
    with Logging

trait AuthActionNoEnrollment
  extends ActionBuilder[AuthRequestWithNoEnrollment, AnyContent]
    with ActionFunction[Request, AuthRequestWithNoEnrollment]
    with AuthorisedFunctions
    with Logging


class PsaPspEnrolmentAuthAction @Inject()(
                             override val authConnector: AuthConnector,
                             val parser: BodyParsers.Default,
                             enrolmentLoggingService: EnrolmentLoggingService
                           )(implicit val executionContext: ExecutionContext)
  extends AuthAction {

  def getEnrolmentIdentifier(
                              enrolments: Enrolments,
                              enrolmentKey: String,
                              enrolmentIdKey: String
                            ): Option[String] =
    for {
      enrolment  <- enrolments.getEnrolment(enrolmentKey)
      identifier <- enrolment.getIdentifier(enrolmentIdKey)
    } yield identifier.value

  override def invokeBlock[A](request: Request[A], block: AuthRequest[A] => Future[Result]): Future[Result] =
    invoke(request, block)(HeaderCarrierConverter.fromRequest(request))

  def invoke[A](request: Request[A], block: AuthRequest[A] => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {

    authorised(Enrolment(PSAEnrolmentKey)).retrieve(Retrievals.authorisedEnrolments and Retrievals.externalId) {
      case enrolments ~ Some(externalId) =>
        val psaEnrolmentId = getEnrolmentIdentifier(
          enrolments,
          PSAEnrolmentKey,
          PSAEnrolmentIdKey
        )

        val pspEnrolmentId = getEnrolmentIdentifier(
          enrolments,
          PSPEnrolmentKey,
          PSPEnrolmentIdKey
        )

        psaEnrolmentId
          .orElse(pspEnrolmentId)
          .map {
            psaOrPspId =>
              block(AuthRequest(request, psaOrPspId, externalId))
          }
          .getOrElse {
            Future.failed(InsufficientEnrolments(s"Unable to retrieve enrolment for either $PSAEnrolmentKey or $PSPEnrolmentKey"))
          }
    }
  } recover {
    case e: InsufficientEnrolments =>
      logger.warn("Failed to authorise due to insufficient enrolments", e)
      enrolmentLoggingService.logEnrolments(request.headers.get(Constants.XClientIdHeader))
      Forbidden("Current user doesn't have a valid EORI enrolment.")
    case e: AuthorisationException =>
      logger.warn(s"Failed to authorise", e)
      Unauthorized(s"Failed to authorise user: ${e.reason}")
    case NonFatal(thr) =>
      logger.error(s"Error returned from auth service: ${thr.getMessage}", thr)
      InternalServerError(s"$thr")
  }
}

class NoEnrolmentAuthAction @Inject()(
                                           override val authConnector: AuthConnector,
                                           val parser: BodyParsers.Default,
                                         )(implicit val executionContext: ExecutionContext)
  extends AuthActionNoEnrollment {


  override def invokeBlock[A](request: Request[A], block: AuthRequestWithNoEnrollment[A] => Future[Result]): Future[Result] =
    invoke(request, block)(HeaderCarrierConverter.fromRequest(request))

  def invoke[A](request: Request[A], block: AuthRequestWithNoEnrollment[A] => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {

    authorised().retrieve(Retrievals.externalId) {
      externalId =>
            block(AuthRequestWithNoEnrollment(request, externalId))
        }
    }
  }
