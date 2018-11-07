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

import audit._
import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import connectors.helper.HeaderUtils
import models.{PsaSubscription, PsaToBeRemovedFromScheme}
import play.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.{ErrorHandler, HttpResponseHelper, InvalidPayloadHandler}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@ImplementedBy(classOf[DesConnectorImpl])
trait DesConnector {

  def registerPSA(registerData: JsValue)(implicit
                                         headerCarrier: HeaderCarrier,
                                         ec: ExecutionContext,
                                         request: RequestHeader): Future[Either[HttpException, JsValue]]

  def getPSASubscriptionDetails(psaId: String)(implicit
                                               headerCarrier: HeaderCarrier,
                                               ec: ExecutionContext,
                                               request: RequestHeader): Future[Either[HttpException, PsaSubscription]]

  def ceasePSA(psaToBeCeased: PsaToBeRemovedFromScheme)(implicit
                                         headerCarrier: HeaderCarrier,
                                         ec: ExecutionContext,
                                         request: RequestHeader): Future[Either[HttpException, JsValue]]
}

class DesConnectorImpl @Inject()(
                                     http: HttpClient,
                                     config: AppConfig,
                                     auditService: AuditService,
                                     invalidPayloadHandler: InvalidPayloadHandler,
                                     headerUtils: HeaderUtils
                                   ) extends DesConnector with HttpResponseHelper with ErrorHandler {

  override def registerPSA(registerData: JsValue)(implicit
                                                  headerCarrier: HeaderCarrier,
                                                  ec: ExecutionContext,
                                                  request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val psaSchema = "/resources/schemas/psaSubscription.json"

    val schemeAdminRegisterUrl = config.schemeAdminRegistrationUrl

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeader(headerCarrier))

    http.POST[JsValue, HttpResponse](schemeAdminRegisterUrl, registerData)(implicitly, implicitly, hc, implicitly) map {
      handlePostResponse(_, schemeAdminRegisterUrl) } andThen logFailures("register PSA", registerData, psaSchema)
  }

  override def getPSASubscriptionDetails(psaId: String)(implicit
                                                        headerCarrier: HeaderCarrier,
                                                        ec: ExecutionContext,
                                                        request: RequestHeader): Future[Either[HttpException, PsaSubscription]] = {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeader(headerCarrier))

    val subscriptionDetailsUrl = config.psaSubscriptionDetailsUrl.format(psaId)

    http.GET[HttpResponse](subscriptionDetailsUrl)(
      implicitly[HttpReads[HttpResponse]],
      implicitly[HeaderCarrier](hc),
      implicitly) map { handleGetResponse(_, subscriptionDetailsUrl) } andThen logWarning("PSA subscription details")

  }

  override def ceasePSA(psaToBeCeased: PsaToBeRemovedFromScheme)(implicit
                                                                 headerCarrier: HeaderCarrier,
                                                                 ec: ExecutionContext,
                                                                 request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val ceasePsaSchema = "/resources/schemas/ceasePsa.json"

    val ceasePsaUrl = config.ceasePsaUrl.format(psaToBeCeased.psaId, psaToBeCeased.pstr)

    val data: JsValue = Json.obj("ceaseDate" -> psaToBeCeased.removalDate.toString)

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeader(headerCarrier))

    http.POST[JsValue, HttpResponse](ceasePsaUrl, data)(implicitly, implicitly, hc, implicitly) map {
      handlePostResponse(_, ceasePsaUrl) } andThen logFailures("cease PSA", data, ceasePsaSchema)
  }

  private def handlePostResponse(response: HttpResponse, url: String): Either[HttpException, JsValue] = {

    val badResponseSeq = Seq("INVALID_CORRELATION_ID", "INVALID_PAYLOAD", "INVALID_PSTR", "INVALID_PSAID")
    val forbiddenResponseSeq = Seq("INVALID_BUSINESS_PARTNER", "NO_RELATIONSHIP_EXISTS", "NO_OTHER_ASSOCIATED_PSA", "FUTURE_CEASE_DATE", "PSAID_NOT_ACTIVE")

    response.status match {
      case OK => Right(response.json)
      case CONFLICT if response.body.contains("DUPLICATE_SUBMISSION") => Left(new ConflictException(response.body))
      case FORBIDDEN if forbiddenResponseSeq.exists(response.body.contains(_)) => Left(new ForbiddenException(response.body))
      case _ => Left(handleErrorResponse("Subscription", url, response, badResponseSeq))
    }

  }

  private def handleGetResponse(response: HttpResponse, url: String): Either[HttpException, PsaSubscription] = {

    val badResponseSeq = Seq("INVALID_PSAID", "INVALID_CORRELATION_ID")

    response.status match {
      case OK => Right(validateJson(response.json))
      case status => Left(handleErrorResponse("PSA Subscription details", url, response, badResponseSeq))
    }

  }

  private def logFailures(endpoint: String, data: JsValue, schemaPath: String): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {
    case Success(Left(e: BadRequestException)) if e.message.contains("INVALID_PAYLOAD") =>
      invalidPayloadHandler.logFailures(schemaPath)(data)
    case Success(Left(e: HttpResponse)) => Logger.warn(s"$endpoint received error response from DES", e)
  }

  private def validateJson(json: JsValue): PsaSubscription ={
    json.validate[PsaSubscription] match {
      case JsSuccess(value, _) => value
      case JsError(errors) => throw new JsResultException(errors)
    }
  }

}
