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
import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import connectors.helper.HeaderUtils
import models.{PsaSubscription, PsaToBeRemovedFromScheme}
import play.api.Logger
import play.api.http.Status.*
import play.api.libs.json.*
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import utils.JsonTransformations.PSASubscriptionDetailsTransformer
import utils.{ErrorHandler, HttpResponseHelper, InvalidPayloadHandler, JSONPayloadSchemaValidator}

import java.time.LocalDate
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@ImplementedBy(classOf[DesConnectorImpl])
trait DesConnector {

  def registerPSA(registerData: JsValue)
                 (implicit
                  headerCarrier: HeaderCarrier,
                  ec: ExecutionContext,
                  request: RequestHeader): Future[Either[HttpException, JsValue]]

  def getPSASubscriptionDetails(psaId: String)
                               (implicit
                                headerCarrier: HeaderCarrier,
                                ec: ExecutionContext,
                                request: RequestHeader): Future[Either[HttpException, JsValue]]

  def removePSA(psaToBeRemoved: PsaToBeRemovedFromScheme)
               (implicit
                headerCarrier: HeaderCarrier,
                ec: ExecutionContext,
                request: RequestHeader): Future[Either[HttpException, JsValue]]

  def deregisterPSA(psaId: String)
                   (implicit
                    headerCarrier: HeaderCarrier,
                    ec: ExecutionContext,
                    request: RequestHeader): Future[Either[HttpException, JsValue]]

  def updatePSA(psaId: String, data: JsValue)
               (implicit
                headerCarrier: HeaderCarrier,
                ec: ExecutionContext,
                request: RequestHeader): Future[Either[HttpException, JsValue]]
}

case class PSAFailedMapToUserAnswersException() extends Exception

case class PSAValidationFailureException(error: String) extends Exception(error)

class DesConnectorImpl @Inject()(
                                  httpV2Client: HttpClientV2,
                                  config: AppConfig,
                                  auditService: AuditService,
                                  invalidPayloadHandler: InvalidPayloadHandler,
                                  headerUtils: HeaderUtils,
                                  psaSubscriptionDetailsTransformer: PSASubscriptionDetailsTransformer,
                                  schemeAuditService: SchemeAuditService,
                                  jsonPayloadSchemaValidator: JSONPayloadSchemaValidator
                                )
  extends DesConnector
    with HttpResponseHelper
    with ErrorHandler
    with PSADeEnrolAuditService {

  private val logger = Logger(classOf[DesConnectorImpl])

  override def registerPSA(registerData: JsValue)
                          (implicit
                           headerCarrier: HeaderCarrier,
                           ec: ExecutionContext,
                           @unused request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val psaSchema = "/resources/schemas/psaSubscription.json"
    val url = url"${config.schemeAdminRegistrationUrl}"

    val validationResult = jsonPayloadSchemaValidator.validateJsonPayload(psaSchema, registerData)
    if (validationResult.nonEmpty)
      throw PSAValidationFailureException(s"Invalid payload when registerPSA :-\n${validationResult.mkString}")
    else
      httpV2Client.post(url)
        .setHeader(headerUtils.desHeader *)
        .withBody(registerData).execute[HttpResponse] map {
        handlePostResponse(_, url.toString)
      }
  }

  override def getPSASubscriptionDetails(psaId: String)
                                        (implicit
                                         headerCarrier: HeaderCarrier,
                                         ec: ExecutionContext,
                                         request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val subscriptionDetailsUrl = url"${config.psaSubscriptionDetailsUrl.format(psaId)}"

    httpV2Client.get(subscriptionDetailsUrl)
      .setHeader(headerUtils.desHeader *)
      .execute[HttpResponse] map {
      handleGetResponse(_, subscriptionDetailsUrl.toString)
    } andThen schemeAuditService.sendPSADetailsEvent(psaId)(auditService.sendEvent) andThen
      logWarning("PSA subscription details")

  }

  override def removePSA(psaToBeRemoved: PsaToBeRemovedFromScheme)
                        (implicit
                         headerCarrier: HeaderCarrier,
                         ec: ExecutionContext,
                         request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    implicit val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headerUtils.integrationFrameworkHeader *)
    val url = url"${config.removePsaUrl.format(psaToBeRemoved.pstr)}"
    val data: JsValue = Json.obj(
      "ceaseIDType" -> "PSAID",
      "ceaseNumber" -> psaToBeRemoved.psaId,
      "initiatedIDType" -> "PSAID",
      "initiatedIDNumber" -> psaToBeRemoved.psaId,
      "ceaseDate" -> psaToBeRemoved.removalDate.toString
    )
    removePSAFromScheme(url, data, "/resources/schemas/ceaseFromScheme1461.json", psaToBeRemoved)(using hc, implicitly, implicitly)
  }

  private def removePSAFromScheme(url: java.net.URL,
                                  data: JsValue,
                                  schema: String,
                                  psaToBeRemoved: PsaToBeRemovedFromScheme)
                                 (implicit hc: HeaderCarrier,
                                  ec: ExecutionContext,
                                  request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val validationResult = jsonPayloadSchemaValidator.validateJsonPayload(schema, data)

    if (validationResult.nonEmpty)
      throw PSAValidationFailureException(s"Invalid payload when registerPSA :-\n${validationResult.mkString}")
    else
      httpV2Client.post(url).withBody( data).execute[HttpResponse] map {
        handlePostResponse(_, url.toString)
      } andThen schemeAuditService.sendPSARemovalAuditEvent(psaToBeRemoved)(auditService.sendEvent) andThen
        logFailures("Remove PSA from scheme", data, schema, url.toString)
  }

  override def deregisterPSA(psaId: String)
                            (implicit
                             headerCarrier: HeaderCarrier,
                             ec: ExecutionContext,
                             request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val data: JsValue = Json.obj("deregistrationDate" -> LocalDate.now().toString, "reason" -> "1")
    implicit val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headerUtils.integrationFrameworkHeader *)

    deregisterAdministrator(
      url"${config.deregisterPsaUrl.format(psaId)}",
      data,
      "/resources/schemas/deregister1469.json",
      psaId)(using hc, implicitly, implicitly)
  }

  private def deregisterAdministrator(url: java.net.URL, data: JsValue, schema: String, psaId: String)
                                     (implicit hc: HeaderCarrier,
                                      ec: ExecutionContext,
                                      request: RequestHeader): Future[Either[HttpException, JsValue]] =

    httpV2Client.post(url).withBody(data).execute[HttpResponse] map {
      handlePostResponse(_, url.toString)
    } andThen
      sendPSADeEnrolEvent(psaId)(auditService.sendEvent) andThen
      logFailures("deregister PSA", data, schema, url.toString)

  override def updatePSA(psaId: String, data: JsValue)
                        (implicit
                         headerCarrier: HeaderCarrier,
                         ec: ExecutionContext,
                         @unused request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val psaVariationSchema = "/resources/schemas/psaVariation.json"

    val url = url"${config.psaVariationDetailsUrl.format(psaId)}"

    val validationResult = jsonPayloadSchemaValidator.validateJsonPayload(psaVariationSchema, data)
    if (validationResult.nonEmpty)
      throw PSAValidationFailureException(s"Invalid payload when updatePSA :-\n${validationResult.mkString}")
    else
      httpV2Client.post(url).setHeader(headerUtils.desHeader *).withBody(data).execute[HttpResponse] map {
        handlePostResponse(_, url.toString)
      }
  }

  private def handlePostResponse(response: HttpResponse, url: String): Either[HttpException, JsValue] = {

    val badResponseSeq = Seq("INVALID_CORRELATION_ID", "INVALID_PAYLOAD", "INVALID_PSTR", "INVALID_PSAID", "INVALID_IDTYPE", "INVALID_IDVALUE")
    val forbiddenResponseSeq = Seq("INVALID_BUSINESS_PARTNER", "NO_RELATIONSHIP_EXISTS", "NO_OTHER_ASSOCIATED_PSA", "FUTURE_CEASE_DATE",
      "PSAID_NOT_ACTIVE", "ACTIVE_RELATIONSHIP_EXISTS", "ALREADY_DEREGISTERED", "INVALID_DEREGISTRATION_DATE")

    response.status match {
      case OK => Right(response.json)
      case CONFLICT if response.body.contains("DUPLICATE_SUBMISSION") => Left(new ConflictException(response.body))
      case FORBIDDEN if forbiddenResponseSeq.exists(response.body.contains(_)) => Left(new ForbiddenException(response.body))
      case _ => Left(handleErrorResponse("Subscription", url, response, badResponseSeq))
    }

  }

  private def handleGetResponse(response: HttpResponse, url: String): Either[HttpException, JsValue] = {

    val badResponseSeq = Seq("INVALID_PSAID", "INVALID_CORRELATION_ID")

    response.status match {
      case OK => Right(validateGetJson(response.json))
      case _ => Left(handleErrorResponse("PSA Subscription details", url, response, badResponseSeq))
    }

  }

  private def logFailures(endpoint: String, data: JsValue, schemaPath: String, args: String*): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {
    case Success(Left(e: BadRequestException)) if e.message.contains("INVALID_PAYLOAD") =>
      invalidPayloadHandler.logFailures(schemaPath, args.headOption.getOrElse(""))(data)
    case Success(Left(e: HttpResponse)) => logger.warn(s"$endpoint received error response from DES", e)
  }

  private def validateGetJson(json: JsValue): JsValue = {

    val temporaryMappingTest = json.transform(psaSubscriptionDetailsTransformer.transformToUserAnswers)
    if (temporaryMappingTest.isSuccess)
      logger.info("PensionAdministratorSuccessfulMapToUserAnswers")
    else {
      logger.warn(s"PensionAdministratorFailedMapToUserAnswers - [$temporaryMappingTest]")
    }
    json.validate[PsaSubscription] match {
      case JsSuccess(_, _) =>
        json.transform(psaSubscriptionDetailsTransformer.transformToUserAnswers).getOrElse(throw new PSAFailedMapToUserAnswersException)
      case JsError(errors) => throw JsResultException(errors)
    }
  }

}
