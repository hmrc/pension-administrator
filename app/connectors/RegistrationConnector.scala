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

package connectors

import audit._
import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import connectors.helper.HeaderUtils
import models.registrationnoid._
import models.{SuccessResponse, User}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HttpClient, _}
import utils.{ErrorHandler, HttpResponseHelper, InvalidPayloadHandler, JSONPayloadSchemaValidator}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

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

case class RegistrationValidationFailureException(error: String) extends Exception(error)

class RegistrationConnectorImpl @Inject()(
                                           http: HttpClient,
                                           config: AppConfig,
                                           auditService: AuditService,
                                           headerUtils: HeaderUtils,
                                           invalidPayloadHandler: InvalidPayloadHandler,
                                           jsonPayloadSchemaValidator: JSONPayloadSchemaValidator
                                         ) extends RegistrationConnector with HttpResponseHelper with ErrorHandler with RegistrationAuditService {

  private val logger = Logger(classOf[RegistrationConnector])

  private def desHeaderCarrierWithoutCorrelationId: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeaderWithoutCorrelationId)

  override def registerWithIdIndividual(nino: String, user: User, registerData: JsValue)
                                       (implicit hc: HeaderCarrier,
                                        ec: ExecutionContext,
                                        request: RequestHeader): Future[Either[HttpException, SuccessResponse]] = {
    val registerWithIdUrl = config.registerWithIdIndividualUrl.format(nino)
    val requestSchema = "/resources/schemas/1163-registerWithId-RequestSchema-4.3.0.json"
    val requestValidationResult = jsonPayloadSchemaValidator.validateJsonPayload(requestSchema, registerData)

    logger.debug(s"[Pensions-Scheme-Header-Carrier]-${headerUtils.desHeaderWithoutCorrelationId.toString()}")

    if (requestValidationResult.nonEmpty) {
      throw RegistrationValidationFailureException(s"Invalid payload for registerWithIdIndividual: ${requestValidationResult.mkString}")
    } else {
      http.POST(registerWithIdUrl, registerData)(implicitly, implicitly[HttpReads[HttpResponse]], desHeaderCarrierWithoutCorrelationId, implicitly) map {
        handleResponse[SuccessResponse](_, registerWithIdUrl, requestSchema, registerData, "Business Partner Matching")
      } andThen sendPSARegistrationEvent(
        withId = true, user, "Individual", registerData, withIdIsUk
      )(auditService.sendEvent) andThen logWarning("registerWithIdIndividual")
    }
  }

  override def registerWithIdOrganisation(utr: String, user: User, registerData: JsValue)
                                         (implicit hc: HeaderCarrier,
                                          ec: ExecutionContext,
                                          request: RequestHeader): Future[Either[HttpException, SuccessResponse]] = {

    val registerWithIdUrl = config.registerWithIdOrganisationUrl.format(utr)
    val psaType: String = organisationPsaType(registerData)
    val requestSchema = "/resources/schemas/1163-registerWithId-RequestSchema-4.3.0.json"

    val requestValidationResult = jsonPayloadSchemaValidator.validateJsonPayload(requestSchema, registerData)

    if (requestValidationResult.nonEmpty) {
      throw RegistrationValidationFailureException(s"Invalid payload for registerWithIdOrganisation: ${requestValidationResult.mkString}")
    } else {
      http.POST(registerWithIdUrl, registerData)(implicitly, implicitly[HttpReads[HttpResponse]], desHeaderCarrierWithoutCorrelationId, implicitly) map {
        handleResponse[SuccessResponse](_, registerWithIdUrl, requestSchema, registerData, "Business Partner Matching")
      } andThen sendPSARegistrationEvent(
        withId = true, user, psaType, registerData, withIdIsUk
      )(auditService.sendEvent) andThen logWarningWithoutNotFound("registerWithIdOrganisation")
    }
  }

  def logWarningWithoutNotFound[A](endpoint: String): PartialFunction[Try[Either[HttpException, A]], Unit] = {
    case Success(Left(_: NotFoundException)) => () //Don't log 404 responses as warnings.
    case Success(Left(e: HttpException)) =>
      logger.warn(s"$endpoint received error response from DES", e)
    case Failure(e) =>
      logger.error(s"$endpoint received error response from DES", e)
  }

  override def registrationNoIdOrganisation(user: User, registerData: OrganisationRegistrant)
                                           (implicit hc: HeaderCarrier,
                                            ec: ExecutionContext,
                                            request: RequestHeader): Future[Either[HttpException, RegisterWithoutIdResponse]] = {

    val requestSchema = "/resources/schemas/1335_1336-registerWithoutId-RequestSchema-2.3.0.json"
    val url = config.registerWithoutIdOrganisationUrl
    val correlationId = headerUtils.getCorrelationId

    val registerWithNoIdData = Json.toJson(registerData)(OrganisationRegistrant.writesOrganisationRegistrantRequest(correlationId))

    logger.debug(s"Registration Without Id Organisation request body:" +
      s"${Json.prettyPrint(registerWithNoIdData)}) headers: ${headerUtils.desHeaderWithoutCorrelationId.toString()}")

    val requestValidationResult = jsonPayloadSchemaValidator.validateJsonPayload(requestSchema, registerWithNoIdData)

    if (requestValidationResult.nonEmpty) {
      throw RegistrationValidationFailureException(s"Invalid payload for registrationNoIdOrganisation: ${requestValidationResult.mkString}")
    } else {
      http.POST(url, registerWithNoIdData)(implicitly, httpResponseReads, desHeaderCarrierWithoutCorrelationId, implicitly) map {
        response =>
          logger.debug(s"Registration Without Id Organisation response. Status=${response.status}\n${response.body}")

          handleResponse[RegisterWithoutIdResponse](response, url, requestSchema, registerWithNoIdData, "Register without Id Organisation")

      } andThen sendPSARegWithoutIdEvent(
        withId = false, user, "Organisation", Json.toJson(registerWithNoIdData), _ => Some(false)
      )(auditService.sendEvent) andThen logWarning("registrationNoIdOrganisation")
    }
  }

  override def registrationNoIdIndividual(user: User, registrationRequest: RegistrationNoIdIndividualRequest)
                                         (implicit hc: HeaderCarrier,
                                          ec: ExecutionContext,
                                          request: RequestHeader): Future[Either[HttpException, RegisterWithoutIdResponse]] = {
    val requestSchema = "/resources/schemas/1335_1336-registerWithoutId-RequestSchema-2.3.0.json"
    val url = config.registerWithoutIdIndividualUrl
    val correlationId = headerUtils.getCorrelationId

    val body = Json.toJson(registrationRequest)(RegistrationNoIdIndividualRequest.writesRegistrationNoIdIndividualRequest(correlationId))

    logger.debug(s"Registration Without Id Individual request body:" +
      s"${Json.prettyPrint(body)}) headers: ${headerUtils.desHeaderWithoutCorrelationId.toString()}")

    val requestValidationResult = jsonPayloadSchemaValidator.validateJsonPayload(requestSchema, body)

    if (requestValidationResult.nonEmpty) {
      throw RegistrationValidationFailureException(s"Invalid payload for registrationNoIdIndividual: ${requestValidationResult.mkString}")
    } else {
      http.POST(url, body)(implicitly, httpResponseReads, desHeaderCarrierWithoutCorrelationId, implicitly) map {
        response =>
          logger.debug(s"Registration Without Id Individual response. Status=${response.status}\n${response.body}")

          handleResponse[RegisterWithoutIdResponse](response, url, requestSchema, body, "Register without Id Individual")

      } andThen sendPSARegWithoutIdEvent(
        withId = false, user, "Individual", Json.toJson(registrationRequest), _ => Some(false)
      )(auditService.sendEvent) andThen logWarning("registrationNoIdIndividual")
    }
  }


  private def handleResponse[A](response: HttpResponse, url: String, schema: String, requestBody: JsValue, methodContext: String)(
    implicit reads: Reads[A]): Either[HttpException, A] = {

    val method = "POST"
    response.status match {
      case OK =>
        Json.parse(response.body).validate[A] match {
          case JsSuccess(value, _) => Right(value)
          case JsError(errors) => throw JsResultException(errors)
        }
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
