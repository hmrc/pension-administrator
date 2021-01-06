/*
 * Copyright 2021 HM Revenue & Customs
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
import models.{SuccessResponse, User}
import models.registrationnoid._
import play.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.{ErrorHandler, HttpResponseHelper, InvalidPayloadHandler}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[RegistrationConnectorImpl])
trait RegistrationConnector {
  def registerWithIdIndividual(nino: String, user: User, registerData: JsValue)(
    implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, SuccessResponse]]

  def registerWithIdOrganisation(utr: String, user: User, registerData: JsValue)(
    implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, SuccessResponse]]

  def registrationNoIdOrganisation(user: User, registerData: OrganisationRegistrant)(
    implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, RegisterWithoutIdResponse]]

  def registrationNoIdIndividual(user: User, registrationRequest: RegistrationNoIdIndividualRequest)(
    implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, RegisterWithoutIdResponse]]

}

class RegistrationConnectorImpl @Inject()(
                                           http: HttpClient,
                                           config: AppConfig,
                                           auditService: AuditService,
                                           headerUtils: HeaderUtils,
                                           invalidPayloadHandler: InvalidPayloadHandler
                                         ) extends RegistrationConnector with HttpResponseHelper with ErrorHandler with RegistrationAuditService {

  private def desHeaderCarrierWithoutCorrelationId: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeaderWithoutCorrelationId)

  override def registerWithIdIndividual(nino: String, user: User, registerData: JsValue)
                                       (implicit hc: HeaderCarrier,
                                        ec: ExecutionContext,
                                        request: RequestHeader): Future[Either[HttpException, SuccessResponse]] = {
    val registerWithIdUrl = config.registerWithIdIndividualUrl.format(nino)
    val schema = "/resources/schemas/registrationWithIdRequest.json"

    Logger.debug(s"[Pensions-Scheme-Header-Carrier]-${headerUtils.desHeaderWithoutCorrelationId.toString()}")

    http.POST(registerWithIdUrl, registerData)(implicitly, implicitly[HttpReads[HttpResponse]], desHeaderCarrierWithoutCorrelationId, implicitly) map {
      handleResponse[SuccessResponse](_, registerWithIdUrl, schema,  registerData, "Business Partner Matching")
    } andThen sendPSARegistrationEvent(
      withId = true, user, "Individual", registerData, withIdIsUk
    )(auditService.sendEvent) andThen logWarning("registerWithIdIndividual")

  }

  override def registerWithIdOrganisation(utr: String, user: User, registerData: JsValue)
                                         (implicit hc: HeaderCarrier,
                                          ec: ExecutionContext,
                                          request: RequestHeader): Future[Either[HttpException, SuccessResponse]] = {

    val registerWithIdUrl = config.registerWithIdOrganisationUrl.format(utr)
    val psaType: String = organisationPsaType(registerData)
    val schema = "/resources/schemas/registrationWithIdRequest.json"

    http.POST(registerWithIdUrl, registerData)(implicitly, implicitly[HttpReads[HttpResponse]], desHeaderCarrierWithoutCorrelationId, implicitly) map {
      handleResponse[SuccessResponse](_, registerWithIdUrl,schema, registerData,  "Business Partner Matching")
    } andThen sendPSARegistrationEvent(
      withId = true, user, psaType, registerData, withIdIsUk
    )(auditService.sendEvent) andThen logWarning("registerWithIdOrganisation")

  }

  override def registrationNoIdOrganisation(user: User, registerData: OrganisationRegistrant)
                                           (implicit hc: HeaderCarrier,
                                            ec: ExecutionContext,
                                            request: RequestHeader): Future[Either[HttpException, RegisterWithoutIdResponse]] = {

    val schema = "/resources/schemas/registrationWithoutIdRequest.json"
    val url = config.registerWithoutIdOrganisationUrl
    val correlationId = headerUtils.getCorrelationId(hc.requestId.map(_.value))

    val registerWithNoIdData = Json.toJson(registerData)(OrganisationRegistrant.writesOrganisationRegistrantRequest(correlationId))

    Logger.debug(s"Registration Without Id Organisation request body:" +
      s"${Json.prettyPrint(registerWithNoIdData)}) headers: ${headerUtils.desHeaderWithoutCorrelationId.toString()}")

    http.POST(url, registerWithNoIdData)(implicitly, httpResponseReads, desHeaderCarrierWithoutCorrelationId, implicitly) map {
      response =>
        Logger.debug(s"Registration Without Id Organisation response. Status=${response.status}\n${response.body}")

        handleResponse[RegisterWithoutIdResponse](response, url, schema, registerWithNoIdData, "Register without Id Organisation")

    } andThen sendPSARegWithoutIdEvent(
      withId = false, user, "Organisation", Json.toJson(registerWithNoIdData), _ => Some(false)
    )(auditService.sendEvent) andThen logWarning("registrationNoIdOrganisation")
  }

  override def registrationNoIdIndividual(user: User, registrationRequest: RegistrationNoIdIndividualRequest)
                                         (implicit hc: HeaderCarrier,
                                          ec: ExecutionContext,
                                          request: RequestHeader): Future[Either[HttpException, RegisterWithoutIdResponse]] = {
    val schema = "/resources/schemas/registrationWithoutIdRequest.json"
    val url = config.registerWithoutIdIndividualUrl
    val correlationId = headerUtils.getCorrelationId(hc.requestId.map(_.value))

    val body = Json.toJson(registrationRequest)(RegistrationNoIdIndividualRequest.writesRegistrationNoIdIndividualRequest(correlationId))

    Logger.debug(s"Registration Without Id Individual request body:" +
      s"${Json.prettyPrint(body)}) headers: ${headerUtils.desHeaderWithoutCorrelationId.toString()}")

    http.POST(url, body)(implicitly, httpResponseReads, desHeaderCarrierWithoutCorrelationId, implicitly) map {
      response =>
        Logger.debug(s"Registration Without Id Individual response. Status=${response.status}\n${response.body}")

        handleResponse[RegisterWithoutIdResponse](response, url, schema, body, "Register without Id Individual")

    } andThen sendPSARegWithoutIdEvent(
      withId = false, user, "Individual", Json.toJson(registrationRequest), _ => Some(false)
    )(auditService.sendEvent) andThen logWarning("registrationNoIdIndividual")
  }


  private def handleResponse[A](response: HttpResponse, url: String, schema: String, requestBody: JsValue, methodContext: String)(
    implicit reads: Reads[A]): Either[HttpException, A] = {

    val method = "POST"
    response.status match {
      case OK =>
        val onInvalid = invalidPayloadHandler.logFailures(schema) _
        Right(parseAndValidateJson[A](response.body, method, url, onInvalid))

      case BAD_REQUEST if response.body.contains("INVALID_PAYLOAD") =>
        invalidPayloadHandler.logFailures(schema)(requestBody)
        Left(new BadRequestException(upstreamResponseMessage(method, url, BAD_REQUEST, response.body)))

      case FORBIDDEN if response.body.contains("INVALID_SUBMISSION") =>
        Left(new BadRequestException(upstreamResponseMessage(method, url, BAD_REQUEST, response.body)))

      case _ =>
        Left(handleErrorResponse(methodContext, url, response, Seq.empty))
    }
  }
}
