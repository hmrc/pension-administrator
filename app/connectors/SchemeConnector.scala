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

package connectors

import audit.AuditService
import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import connectors.helper.HeaderUtils
import models.{PSAMinimalDetails, SchemeReferenceNumber}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.{ErrorHandler, HttpResponseHelper, InvalidPayloadHandler}
import play.api.http.Status._
import play.api.libs.json.JsSuccess

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SchemeConnectorImpl])
trait SchemeConnector {

    def checkForAssociation(psaId: PsaId, srn: SchemeReferenceNumber)(implicit
                                                                      headerCarrier: HeaderCarrier,
                                                                      ec: ExecutionContext,
                                                                      request: RequestHeader): Future[Either[HttpException, Boolean]]

}

class SchemeConnectorImpl @Inject()(
                                  http: HttpClient,
                                  config: AppConfig,
                                  auditService: AuditService,
                                  invalidPayloadHandler: InvalidPayloadHandler,
                                  headerUtils: HeaderUtils
                                ) extends SchemeConnector with HttpResponseHelper with ErrorHandler {


  override def checkForAssociation(psaId: PsaId, srn: SchemeReferenceNumber)(implicit
                                                                             headerCarrier: HeaderCarrier,
                                                                             ec: ExecutionContext,
                                                                             request: RequestHeader): Future[Either[HttpException, Boolean]] = {

    val headers: Seq[(String, String)] = Seq(("psaId", psaId.value), ("schemeReferenceNumber", srn), ("Content-Type", "application/json"))

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headers)

    http.GET[HttpResponse](config.checkAssociationUrl)(implicitly, hc, implicitly) map handleResponse
  }

  private def handleResponse(response: HttpResponse)(implicit hc: HeaderCarrier): Either[HttpException, Boolean] = {
    val badResponse = Seq("Bad Request with missing parameters PSA Id or SRN")
    response.status match {
      case OK => response.json.validate[Boolean] match {
        case JsSuccess(value, _) => Right(value)
      }
      case _ => Left(handleErrorResponse(s"PSA minimal details with headers: ${hc.headers}", config.checkAssociationUrl, response, badResponse))
    }
  }

}