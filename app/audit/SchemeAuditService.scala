/*
 * Copyright 2022 HM Revenue & Customs
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

package audit

import com.google.inject.Inject
import models.{PensionSchemeAdministrator, PsaToBeRemovedFromScheme}
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HttpException

import scala.util.{Failure, Success, Try}

class SchemeAuditService @Inject()() {

  private val logger = Logger(classOf[SchemeAuditService])

  def sendPSASubscriptionEvent(psa: PensionSchemeAdministrator, request: JsValue)
                              (sendEvent: PSASubscription => Unit): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {
    case Success(Right(json)) =>
      sendEvent(
        PSASubscription(
          existingUser = psa.pensionSchemeAdministratorIdentifierStatus.isExistingPensionSchemaAdministrator,
          legalStatus = psa.legalStatus,
          status = Status.OK,
          request = request,
          response = Some(json)
        )
      )
    case Success(Left(e)) =>
      sendEvent(
        PSASubscription(
          existingUser = psa.pensionSchemeAdministratorIdentifierStatus.isExistingPensionSchemaAdministrator,
          legalStatus = psa.legalStatus,
          status = e.responseCode,
          request = request,
          response = None
        )
      )
    case Failure(t) =>
      logger.error("Error in sending audit event for PSA Subscription", t)
  }

  def sendPSAChangeEvent(psa: PensionSchemeAdministrator, request: JsValue)
                        (sendEvent: PSAChanges => Unit): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {
    case Success(Right(json)) =>
      sendEvent(
        PSAChanges(
          legalStatus = psa.legalStatus,
          status = Status.OK,
          request = request,
          response = Some(json)
        )
      )
    case Success(Left(e)) =>
      sendEvent(
        PSAChanges(
          legalStatus = psa.legalStatus,
          status = e.responseCode,
          request = request,
          response = None
        )
      )
    case Failure(t) =>
      logger.error("Error in sending audit event for PSA Changes", t)
  }

  def sendPSADetailsEvent(psaId: String)
                         (sendEvent: PSADetails => Unit): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {

    case Success(Right(psaSubscription)) =>
      sendEvent(
        PSADetails(
          psaId = psaId,
          psaName = SchemeAuditService.getName(psaSubscription),
          status = Status.OK,
          response = Some(psaSubscription)
        )
      )
    case Success(Left(e)) =>
      sendEvent(
        PSADetails(
          psaId = psaId,
          psaName = None,
          status = e.responseCode,
          response = None
        )
      )
    case Failure(t) =>
      logger.error("Error in sending audit event for get PSA details", t)

  }

  def sendPSARemovalAuditEvent(psaToBeRemovedFromScheme: PsaToBeRemovedFromScheme)
                              (sendEvent: PSARemovalFromSchemeAuditEvent => Unit)
  : PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {
    case Success(Right(_)) =>
      sendEvent(PSARemovalFromSchemeAuditEvent(psaToBeRemovedFromScheme))
    case Failure(t) =>
      logger.error("Error in sending audit event for get PSA details", t)

  }
}

object SchemeAuditService {
  private[audit] def getName(psaSubscription: JsValue): Option[String] =
    (psaSubscription \ "registrationInfo" \ "legalStatus").as[String] match {
      case "Individual" =>
        Some(Seq((psaSubscription \ "individualDetails" \ "firstName").asOpt[String],
          (psaSubscription \ "individualDetails" \ "middleName").asOpt[String],
          (psaSubscription \ "individualDetails" \ "lastName").asOpt[String]).flatten(s => s).mkString(" "))
      case "Partnership" => (psaSubscription \ "partnershipDetails" \ "companyName").asOpt[String]
      case "Limited Company" => (psaSubscription \ "businessDetails" \ "companyName").asOpt[String]
      case _ => None
    }
}
