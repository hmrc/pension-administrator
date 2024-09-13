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
import models.User
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HttpClient, _}
import utils.{ErrorHandler, HttpResponseHelper, InvalidPayloadHandler, JSONPayloadSchemaValidator, ValidationFailure}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[RegistrationConnectorImpl])
trait RegistrationConnector {
  def registerWithIdIndividual(nino: String, user: User, registerData: JsValue)(
    implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, JsValue]]

  def registerWithIdOrganisation(utr: String, user: User, registerData: JsValue)(
    implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, JsValue]]

  def registrationNoIdOrganisation(user: User, registerData: OrganisationRegistrant)(
    implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, JsValue]]

  def registrationNoIdIndividual(user: User, registrationRequest: RegistrationNoIdIndividualRequest)(
    implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, JsValue]]

}

case class RegistrationRequestValidationFailureException(error: String) extends Exception(error)

case class RegistrationResponseValidationFailureException(error: String) extends Exception(error)


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
                                        request: RequestHeader): Future[Either[HttpException, JsValue]] = {
    val registerWithIdUrl = config.registerWithIdIndividualUrl.format(nino)
    val requestSchema = "/resources/schemas/1163-registerWithId-RequestSchema-4.3.0.json"
    val responseSchema = "/resources/schemas/1163-registerWithId-ResponseSchema-4.2.1.json"
    val requestValidationResult = jsonPayloadSchemaValidator.validateJsonPayload(requestSchema, registerData)
    val responseValidation = jsonPayloadSchemaValidator.validateJsonPayload(responseSchema, _)

    logger.debug(s"[Pensions-Scheme-Header-Carrier]-${headerUtils.desHeaderWithoutCorrelationId.toString()}")

    if (requestValidationResult.nonEmpty) {
      throw RegistrationRequestValidationFailureException(s"Invalid payload for registerWithIdIndividual: ${requestValidationResult.mkString}")
    } else {
      http.POST(registerWithIdUrl, registerData)(implicitly, implicitly[HttpReads[HttpResponse]], desHeaderCarrierWithoutCorrelationId, implicitly) map {
        handleResponse(_,
          registerWithIdUrl,
          requestSchema,
          registerData,
          "Business Partner Matching",
          responseValidation
        )
      } andThen sendPSARegistrationEvent(
        withId = true, user, "Individual", registerData, withIdIsUk
      )(auditService.sendEvent) andThen logWarning("registerWithIdIndividual")
    }
  }

  override def registerWithIdOrganisation(utr: String, user: User, registerData: JsValue)
                                         (implicit hc: HeaderCarrier,
                                          ec: ExecutionContext,
                                          request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val registerWithIdUrl = config.registerWithIdOrganisationUrl.format(utr)
    val psaType: String = organisationPsaType(registerData)
    val requestSchema = "/resources/schemas/1163-registerWithId-RequestSchema-4.3.0.json"
    val responseSchema = "/resources/schemas/1163-registerWithId-ResponseSchema-4.2.1.json"

    val requestValidationResult = jsonPayloadSchemaValidator.validateJsonPayload(requestSchema, registerData)
    val responseValidation = jsonPayloadSchemaValidator.validateJsonPayload(responseSchema, _)

    if (requestValidationResult.nonEmpty) {
      throw RegistrationRequestValidationFailureException(s"Invalid payload for registerWithIdOrganisation: ${requestValidationResult.mkString}")
    } else {
      http.POST(registerWithIdUrl, registerData)(implicitly, implicitly[HttpReads[HttpResponse]], desHeaderCarrierWithoutCorrelationId, implicitly) map {
        handleResponse(_,
          registerWithIdUrl,
          requestSchema,
          registerData,
          "Business Partner Matching",
          responseValidation
        )
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
                                            request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val requestSchema = "/resources/schemas/1335_1336-registerWithoutId-RequestSchema-2.3.0.json"
    val responseSchema = "/resources/schemas/1335_1336-registerWithoutId-ResponseSchema.json"

    val url = config.registerWithoutIdOrganisationUrl
    val correlationId = headerUtils.getCorrelationId

    val registerWithNoIdData = Json.toJson(registerData)(OrganisationRegistrant.writesOrganisationRegistrantRequest(correlationId))

    logger.debug(s"Registration Without Id Organisation request body:" +
      s"${Json.prettyPrint(registerWithNoIdData)}) headers: ${headerUtils.desHeaderWithoutCorrelationId.toString()}")

    val requestValidationResult = jsonPayloadSchemaValidator.validateJsonPayload(requestSchema, registerWithNoIdData)

    val responseValidation = jsonPayloadSchemaValidator.validateJsonPayload(responseSchema, _)

    if (requestValidationResult.nonEmpty) {
      throw RegistrationRequestValidationFailureException(s"Invalid payload for registrationNoIdOrganisation: ${requestValidationResult.mkString}")
    } else {
      http.POST(url, registerWithNoIdData)(implicitly, httpResponseReads, desHeaderCarrierWithoutCorrelationId, implicitly) map {
        response =>
          logger.debug(s"Registration Without Id Organisation response. Status=${response.status}\n${response.body}")

          handleResponse(response,
            url,
            requestSchema,
            registerWithNoIdData,
            "Register without Id Organisation",
            responseValidation
          )

      } andThen sendPSARegWithoutIdEvent(
        withId = false, user, "Organisation", Json.toJson(registerWithNoIdData), _ => Some(false)
      )(auditService.sendEvent) andThen logWarning("registrationNoIdOrganisation")
    }
  }

  override def registrationNoIdIndividual(user: User, registrationRequest: RegistrationNoIdIndividualRequest)
                                         (implicit hc: HeaderCarrier,
                                          ec: ExecutionContext,
                                          request: RequestHeader): Future[Either[HttpException, JsValue]] = {
    val requestSchema = "/resources/schemas/1335_1336-registerWithoutId-RequestSchema-2.3.0.json"
    val responseSchema = "/resources/schemas/1335_1336-registerWithoutId-ResponseSchema.json"
    val url = config.registerWithoutIdIndividualUrl
    val correlationId = headerUtils.getCorrelationId

    val body = Json.toJson(registrationRequest)(RegistrationNoIdIndividualRequest.writesRegistrationNoIdIndividualRequest(correlationId))

    logger.debug(s"Registration Without Id Individual request body:" +
      s"${Json.prettyPrint(body)}) headers: ${headerUtils.desHeaderWithoutCorrelationId.toString()}")

    val requestValidationResult = jsonPayloadSchemaValidator.validateJsonPayload(requestSchema, body)

    val responseValidation = jsonPayloadSchemaValidator.validateJsonPayload(responseSchema, _)

    if (requestValidationResult.nonEmpty) {
      throw RegistrationRequestValidationFailureException(s"Invalid payload for registrationNoIdIndividual: ${requestValidationResult.mkString}")
    } else {
      http.POST(url, body)(implicitly, httpResponseReads, desHeaderCarrierWithoutCorrelationId, implicitly) map {
        response =>
          logger.debug(s"Registration Without Id Individual response. Status=${response.status}\n${response.body}")

          handleResponse(response,
            url,
            requestSchema,
            body,
            "Register without Id Individual",
            responseValidation)

      } andThen sendPSARegWithoutIdEvent(
        withId = false, user, "Individual", Json.toJson(registrationRequest), _ => Some(false)
      )(auditService.sendEvent) andThen logWarning("registrationNoIdIndividual")
    }
  }


  private def handleResponse(response: HttpResponse,
                                url: String,
                                schema: String,
                                requestBody: JsValue,
                                methodContext: String,
                                validateResponse: JsValue => Set[ValidationFailure]): Either[HttpException, JsValue] = {

    val method = "POST"
    response.status match {
      case OK =>
        val jsonResponse = Json.parse(response.body)
        println(s"\n\n jsonResponse is: $jsonResponse")
        val responseValidation = validateResponse(jsonResponse)

        if (responseValidation.isEmpty) {
          Right(jsonResponse)
        } else {
          throw RegistrationResponseValidationFailureException(s"Invalid response to registration: ${responseValidation.mkString}")
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
