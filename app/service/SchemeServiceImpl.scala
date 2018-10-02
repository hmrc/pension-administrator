/*
 * Copyright 2018 HM Revenue & Customs
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
import config.AppConfig
import connectors.DesConnector
import models.PensionSchemeAdministrator
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import utils.validationUtils._

class SchemeServiceImpl @Inject()(schemeConnector: DesConnector,
                                  auditService: AuditService, appConfig: AppConfig) extends SchemeService with SchemeAuditService {

  override def registerPSA(json: JsValue)
                          (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, rh: RequestHeader): Future[Either[HttpException, JsValue]] = {
    Try(json.convertTo[PensionSchemeAdministrator](PensionSchemeAdministrator.apiReads)) match {
      case Success(pensionSchemeAdministrator) =>
        val psaJsValue = Json.toJson(pensionSchemeAdministrator)(PensionSchemeAdministrator.psaSubmissionWrites)
        Logger.debug(s"[PSA-Registration-Outgoing-Payload]$psaJsValue")

        schemeConnector.registerPSA(psaJsValue) andThen sendPSASubscriptionEvent(pensionSchemeAdministrator, psaJsValue)(auditService.sendEvent)

      case Failure(e) =>
        Logger.warn(s"Bad Request returned from frontend for PSA $e")
        Future.failed(new BadRequestException(s"Bad Request returned from frontend for PSA $e"))
    }

  }
}
