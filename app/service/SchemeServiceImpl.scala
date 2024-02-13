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

package service

import audit.{AuditService, SchemeAuditService}
import com.google.inject.Inject
import connectors.DesConnector
import models.PensionSchemeAdministrator
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import repositories.MinimalDetailsCacheRepository
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException}
import utils.ValidationUtils._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SchemeServiceImpl @Inject()(desConnector: DesConnector,
                                  auditService: AuditService,
                                  schemeAuditService: SchemeAuditService,
                                  minimalDetailsCacheRepository: MinimalDetailsCacheRepository) extends SchemeService {

  private val logger = Logger(classOf[SchemeServiceImpl])

  override def registerPSA(json: JsValue)(
    implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, rh: RequestHeader): Future[Either[HttpException, JsValue]] = {

    convertPensionSchemeAdministrator(json) { pensionSchemeAdministrator =>
      val psaJsValue = Json.toJson(pensionSchemeAdministrator)(PensionSchemeAdministrator.psaSubmissionWrites)
      logger.debug(s"[PSA-Registration-Outgoing-Payload]$psaJsValue")
      desConnector.registerPSA(psaJsValue) andThen
        schemeAuditService.sendPSASubscriptionEvent(pensionSchemeAdministrator, psaJsValue)(auditService.sendEvent)
    }

  }

  override def updatePSA(psaId: String, json: JsValue)(
    implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, JsValue]] = {
    logger.debug(s"[PSA-Variation-Incoming-Payload]$json")

    convertPensionSchemeAdministrator(json) { pensionSchemeAdministrator =>
      val psaJsValue = Json.toJson(pensionSchemeAdministrator)(PensionSchemeAdministrator.psaUpdateWrites)
      logger.debug(s"[PSA-Variation-Outgoing-Payload]$psaJsValue")
      val result = for {
        apiCallResult <- desConnector.updatePSA(psaId, psaJsValue)
        internalResult <- apiCallResult match {
          case Right(_) => minimalDetailsCacheRepository.remove(psaId).map(_ => apiCallResult)
          case _ => Future.successful(apiCallResult)
        }
      } yield internalResult
      result andThen schemeAuditService.sendPSAChangeEvent(pensionSchemeAdministrator, psaJsValue)(auditService.sendEvent)
    }
  }

  private def convertPensionSchemeAdministrator(json: JsValue)(
    block: PensionSchemeAdministrator => Future[Either[HttpException, JsValue]]): Future[Either[HttpException, JsValue]] = {

    Try(json.convertTo[PensionSchemeAdministrator](PensionSchemeAdministrator.apiReads)) match {
      case Success(pensionSchemeAdministrator) =>
        block(pensionSchemeAdministrator)
      case Failure(e) =>
        logger.warn(s"Bad Request returned from frontend for PSA $e")
        Future.failed(new BadRequestException(s"Bad Request returned from frontend for PSA $e"))
    }
  }
}

