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

package connector

import java.util.UUID.randomUUID

import audit._
import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import play.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Writes}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.InvalidPayloadHandler
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[SchemeConnectorImpl])
trait SchemeConnector {
   def registerPSA(registerData: JsValue)(implicit
                                         headerCarrier: HeaderCarrier,
                                         ec: ExecutionContext,
                                         request: RequestHeader): Future[Either[HttpException, JsValue]]

  def getCorrelationId(requestId: Option[String]): String
}

class SchemeConnectorImpl @Inject()(
                                     http: HttpClient,
                                     config: AppConfig,
                                     auditService: AuditService,
                                     invalidPayloadHandler: InvalidPayloadHandler
                                   ) extends SchemeConnector with HttpErrorFunctions {

  def desHeader(implicit hc: HeaderCarrier): Seq[(String, String)] = {
    val requestId = getCorrelationId(hc.requestId.map(_.value))

    Seq("Environment" -> config.desEnvironment, "Authorization" -> config.authorization,
      "Content-Type" -> "application/json", "CorrelationId" -> requestId)
  }

  def getCorrelationId(requestId: Option[String]): String = {
    requestId.getOrElse {
      Logger.error("No Request Id found while calling register with Id")
      randomUUID.toString
    }.replaceAll("(govuk-tax-|-)", "").slice(0, 32)
  }

  //scalastyle:off cyclomatic.complexity
  private def handleResponse(response: HttpResponse): Either[HttpException, JsValue] = {
    val badResponseSeq = Seq("INVALID_CORRELATION_ID", "INVALID_PAYLOAD")
    response.status match {
      case OK => Right(response.json)
      case BAD_REQUEST if badResponseSeq.exists(response.body.contains(_)) => Left(new BadRequestException(response.body))
      case CONFLICT if response.body.contains("DUPLICATE_SUBMISSION") => Left(new ConflictException(response.body))
      case NOT_FOUND => Left(new NotFoundException(response.body))
      case FORBIDDEN if response.body.contains("INVALID_BUSINESS_PARTNER") => Left(new ForbiddenException(response.body))
      case status if is4xx(status) => throw Upstream4xxResponse(response.body, status, BAD_REQUEST, response.allHeaders)
      case status if is5xx(status) => throw Upstream5xxResponse(response.body, status, BAD_GATEWAY)
      case status => throw new Exception(s"Subscription failed with status $status. Response body: '${response.body}'")
    }
  }
  //scalastyle:on cyclomatic.complexity
  private def logFailures(endpoint: String, data: JsValue, schemaPath: String): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {
    case Success(Left(e: BadRequestException)) if e.message.contains("INVALID_PAYLOAD") =>
      invalidPayloadHandler.logFailures(schemaPath, data)
    case Success(Left(e: HttpResponse)) => Logger.warn(s"$endpoint received error response from DES", e)
  }

  override def registerPSA(registerData: JsValue)(implicit
                                                  headerCarrier: HeaderCarrier,
                                                  ec: ExecutionContext,
                                                  request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    implicit val rds: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse = response
    }

    val psaSchema = "/resources/schemas/psaSubscription.json"
    val schemeAdminRegisterUrl = config.schemeAdminRegistrationUrl
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader(implicitly[HeaderCarrier](headerCarrier)))

    http.POST[JsValue, HttpResponse](schemeAdminRegisterUrl, registerData)(implicitly[Writes[JsValue]],
      implicitly, implicitly[HeaderCarrier](hc), implicitly) map handleResponse andThen logFailures("register PSA", registerData, psaSchema)
  }
}
