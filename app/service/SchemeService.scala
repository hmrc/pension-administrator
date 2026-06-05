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
import connectors.{DesConnector, HipConnector}
import models.PensionSchemeAdministrator
import models.admin.PsaRegHipMigrationToggle
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import repositories.MinimalDetailsCacheRepository
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException}
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import utils.ValidationUtils.*

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SchemeService @Inject()(
                               desConnector: DesConnector,
                               hipConnector: HipConnector,
                               auditService: AuditService,
                               schemeAuditService: SchemeAuditService,
                               minimalDetailsCacheRepository: MinimalDetailsCacheRepository,
                               featureFlagService: FeatureFlagService
                             ) extends Logging {

  def registerPSA(json: JsValue)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext, rh: RequestHeader): Future[Either[HttpException, JsValue]] =

    convertPensionSchemeAdministrator(json) { pensionSchemeAdministrator =>
      val psaJsValue = Json.toJson(pensionSchemeAdministrator)(using PensionSchemeAdministrator.psaSubmissionWrites)

      featureFlagService.get(PsaRegHipMigrationToggle)
        .flatMap { toggle =>
          if (toggle.isEnabled) {
            logger.debug(s"[HIP PSA-Registration-Outgoing-Payload]$psaJsValue")
            hipConnector
              .registerPSA(psaJsValue)
              .andThen {
                schemeAuditService.sendPSASubscriptionEvent(pensionSchemeAdministrator, psaJsValue)(auditService.sendEvent)
              }
          } else {
            logger.debug(s"[DES PSA-Registration-Outgoing-Payload]$psaJsValue")
            desConnector
              .registerPSA(psaJsValue)
              .andThen {
                schemeAuditService.sendPSASubscriptionEvent(pensionSchemeAdministrator, psaJsValue)(auditService.sendEvent)
              }
          }
        }
    }


  def updatePSA(psaId: String, json: JsValue)
               (implicit hc: HeaderCarrier, ec: ExecutionContext, rh: RequestHeader): Future[Either[HttpException, JsValue]] =

    convertPensionSchemeAdministrator(json) { pensionSchemeAdministrator =>
      logger.debug(s"[PSA-Variation-Incoming-Payload]$json")

      val psaJsValue = Json.toJson(pensionSchemeAdministrator)(using PensionSchemeAdministrator.psaUpdateWrites)

      logger.debug(s"[PSA-Variation-Outgoing-Payload]$psaJsValue")

      featureFlagService.get(PsaRegHipMigrationToggle)
        .flatMap { toggle =>
          if (toggle.isEnabled) {
            hipConnector
              .updatePSA(psaId, psaJsValue).flatMap {
                case Right(jsValue) =>
                  minimalDetailsCacheRepository.remove(psaId).map(_ => Right(jsValue))
                case Left(httpException) =>
                  Future.successful(Left(httpException))
              }
              .andThen {
                schemeAuditService.sendPSAChangeEvent(pensionSchemeAdministrator, psaJsValue)(auditService.sendEvent)
              }
          } else {
            desConnector
              .updatePSA(psaId, psaJsValue).flatMap {
                case Right(jsValue) =>
                  minimalDetailsCacheRepository.remove(psaId).map(_ => Right(jsValue))
                case Left(httpException) =>
                  Future.successful(Left(httpException))
              }
              .andThen {
                schemeAuditService.sendPSAChangeEvent(pensionSchemeAdministrator, psaJsValue)(auditService.sendEvent)
              }
          }
        }
    }

  private def convertPensionSchemeAdministrator(json: JsValue)
                                               (block: PensionSchemeAdministrator => Future[Either[HttpException, JsValue]]): Future[Either[HttpException, JsValue]] = {

    Try(json.convertTo[PensionSchemeAdministrator](using PensionSchemeAdministrator.apiReads)) match {
      case Success(pensionSchemeAdministrator) =>
        block(pensionSchemeAdministrator)
      case Failure(e) =>
        logger.warn(s"Bad Request returned from frontend for PSA $e")
        Future.failed(new BadRequestException(s"Bad Request returned from frontend for PSA $e"))
    }
  }
}
