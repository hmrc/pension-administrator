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

package connectors

import audit.*
import com.google.inject.Inject
import config.AppConfig
import models.PsaSubscription
import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.*
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import utils.JsonTransformations.PSASubscriptionDetailsTransformer
import utils.{ErrorHandler, HttpResponseHelper}

import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.{Base64, Date, UUID}
import scala.concurrent.{ExecutionContext, Future}

class HipConnector @Inject()(
  http: HttpClientV2,
  config: AppConfig,
  auditService: AuditService,
  schemeAuditService: SchemeAuditService,
  psaSubscriptionDetailsTransformer: PSASubscriptionDetailsTransformer
)(implicit ec: ExecutionContext)
  extends HttpResponseHelper
    with ErrorHandler
    with Logging {

  private val token: String =
    Base64
      .getEncoder
      .encodeToString(s"${config.hipClientId}:${config.hipClientSecret}".getBytes(StandardCharsets.UTF_8))

  private def headers: Seq[(String, String)] =
    Seq(
      "X-Transmitting-System" -> "HIP",
      "X-Originating-System"  -> "PSA",
      "X-Receipt-Date"        -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()),
      "correlationid"         -> UUID.randomUUID().toString,
      "Authorization"         -> s"Basic $token"
    )

  def getPSASubscriptionDetails(psaId: String)
                               (implicit hc: HeaderCarrier, rh: RequestHeader): Future[Either[HttpException, JsValue]] =
    http
      .get(url"${config.hipPsaSubscriptionDetailsUrl.format(psaId)}")
      .setHeader(headers *)
      .execute[HttpResponse]
      .map(handleInboundResponse)
      .andThen {
        schemeAuditService.sendPSADetailsEvent(psaId)(auditService.sendEvent)
      }
      .andThen {
        logWarning("PSA subscription details")
      }

  def registerPSA(data: JsValue)
                 (implicit hc: HeaderCarrier): Future[Either[HttpException, JsValue]] =
    http
      .post(url"${config.hipPsaSubscriptionCreateUrl}")
      .setHeader(headers *)
      .withBody(data)
      .execute[HttpResponse]
      .map(handleOutboundResponse)

  def updatePSA(psaId: String, data: JsValue)
               (implicit hc: HeaderCarrier): Future[Either[HttpException, JsValue]] =
    http
      .put(url"${config.hipPsaVariationDetailsUrl.format(psaId)}")
      .setHeader(headers *)
      .withBody(data)
      .execute[HttpResponse]
      .map(handleOutboundResponse)

  private def handleOutboundResponse(response: HttpResponse): Either[HttpException, JsValue] =
    response.status match {
      case OK | CREATED =>
        response.json \ "success" match {
          case JsDefined(value) =>
            Right(value)
          case _ =>
            Left(HttpException(response.body, response.status))
        }
      case UNPROCESSABLE_ENTITY =>
        response.json \ "errors" \ "code" match {
          case JsDefined(code) if code.as[String] == "004" =>
            Left(ConflictException(response.body))
          case _ =>
            Left(UnprocessableEntityException(response.body))
        }
      case _ =>
        Left(HttpException(response.body, response.status))
    }

  private def handleInboundResponse(response: HttpResponse): Either[HttpException, JsValue] =
    response.status match {
      case OK =>
        (response.json \ "success").validate[PsaSubscription] match {
          case JsSuccess(_, _) =>
            Right(
              (response.json \ "success").transform(psaSubscriptionDetailsTransformer.transformToUserAnswers) match {
                case JsSuccess(value, _) =>
                  value
                case JsError(errors) =>
                  throw JsResultException(errors)
              }
            )
          case JsError(errors) =>
            throw JsResultException(errors)
        }
      case UNPROCESSABLE_ENTITY =>
        response.json \ "errors" \ "code" match {
          case JsDefined(code) if code.as[String] == "004" =>
            Left(ConflictException(response.body))
          case JsDefined(code) if code.as[String] == "046" =>
            Left(BadRequestException(response.body))
          case _ =>
            Left(UnprocessableEntityException(response.body))
        }
      case _ =>
        Left(HttpException(response.body, response.status))
    }
}
