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

package controllers.actions

import play.api.Logging
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.{PsaId, PspId}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
case class PsaPspAuthRequest[A](request: Request[A], psaId: Option[PsaId], pspId: Option[PspId], externalId: String) extends WrappedRequest[A](request)

class PsaPspEnrolmentAuthAction @Inject()(
                                  override val authConnector: AuthConnector,
                                  val parser: BodyParsers.Default
                                )(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[PsaPspAuthRequest, AnyContent]
    with ActionFunction[Request, PsaPspAuthRequest]
    with AuthorisedFunctions
    with Logging {

  private val PSPEnrolmentKey: String = "HMRC-PODSPP-ORG"
  private val PSPEnrolmentIdKey: String = "PspID"
  private val PSAEnrolmentKey: String = "HMRC-PODS-ORG"
  private val PSAEnrolmentIdKey: String = "PsaID"
  private def getEnrolmentIdentifier(
                                      enrolments: Enrolments,
                                      enrolmentKey: String,
                                      enrolmentIdKey: String
                                    ): Option[String] =
    for {
      enrolment <- enrolments.getEnrolment(enrolmentKey)
      identifier <- enrolment.getIdentifier(enrolmentIdKey)
    }
    yield identifier.value

  override def
  invokeBlock[A](request: Request[A], block: PsaPspAuthRequest[A] => Future[Result]): Future[Result] =
    invoke(request, block)(HeaderCarrierConverter.fromRequest(request))

  private def invoke[A](request: Request[A], block: PsaPspAuthRequest[A] => Future[Result])
                       (implicit hc: HeaderCarrier): Future[Result] = {
    authorised(Enrolment(PSAEnrolmentKey) or Enrolment(PSPEnrolmentKey)).retrieve(Retrievals.authorisedEnrolments and Retrievals.externalId) {
      case enrolments ~ Some(externalId) =>
        val psaId = getEnrolmentIdentifier(
          enrolments,
          PSAEnrolmentKey,
          PSAEnrolmentIdKey
        )

        val pspId = getEnrolmentIdentifier(
          enrolments,
          PSPEnrolmentKey,
          PSPEnrolmentIdKey
        )

        psaId -> pspId match {
          case (None, None) =>
            logger.warn("Failed to authorise due to insufficient enrolments")
            Future.successful(Forbidden("Enrolments not present"))
          case _ => block(PsaPspAuthRequest(request, psaId.map(PsaId), pspId.map(PspId), externalId))
        }

      case _ => Future.failed(new RuntimeException("No externalId found"))
    }
  } recover {
    case e:
      InsufficientEnrolments =>
      logger.warn("Failed to authorise due to insufficient enrolments", e)
      Forbidden("Current user doesn't have a valid enrolment.")
    case e:
      AuthorisationException =>
      logger.warn(s"Failed to authorise", e)
      Unauthorized(s"Failed to authorise user: ${e.reason}")
    case NonFatal(thr) =>
      logger.error(s"Error returned from auth service: ${thr.getMessage}", thr)
      throw thr
  }
}