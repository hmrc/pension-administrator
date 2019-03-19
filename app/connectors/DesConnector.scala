/*
 * Copyright 2019 HM Revenue & Customs
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
import config.{AppConfig, FeatureSwitchManagementService}
import connectors.helper.HeaderUtils
import models.{PsaSubscription, PsaToBeRemovedFromScheme}
import org.joda.time.LocalDate
import play.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.JsonTransformations.PSASubscriptionDetailsTransformer
import utils.Toggles.IsVariationsEnabled
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
                                               request: RequestHeader): Future[Either[HttpException, JsValue]]

  def removePSA(psaToBeRemoved: PsaToBeRemovedFromScheme)(implicit
                                                          headerCarrier: HeaderCarrier,
                                                          ec: ExecutionContext,
                                                          request: RequestHeader): Future[Either[HttpException, JsValue]]

  def deregisterPSA(psaId: String)(implicit
                                   headerCarrier: HeaderCarrier,
                                   ec: ExecutionContext,
                                   request: RequestHeader): Future[Either[HttpException, JsValue]]

  def updatePSA(psaId: String, data: JsValue)(implicit
                               headerCarrier: HeaderCarrier,
                               ec: ExecutionContext,
                               request: RequestHeader): Future[Either[HttpException, JsValue]]
}

case class PSAFailedMapToUserAnswersException() extends Exception

class DesConnectorImpl @Inject()(
                                  http: HttpClient,
                                  config: AppConfig,
                                  auditService: AuditService,
                                  invalidPayloadHandler: InvalidPayloadHandler,
                                  headerUtils: HeaderUtils,
                                  psaSubscriptionDetailsTransformer: PSASubscriptionDetailsTransformer,
                                  fs: FeatureSwitchManagementService,
                                  schemeAuditService: SchemeAuditService
                                ) extends DesConnector with HttpResponseHelper with ErrorHandler{

  override def registerPSA(registerData: JsValue)(implicit
                                                  headerCarrier: HeaderCarrier,
                                                  ec: ExecutionContext,
                                                  request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val psaSchema = "/resources/schemas/psaSubscription.json"

    val schemeAdminRegisterUrl = config.schemeAdminRegistrationUrl

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeader(headerCarrier))

    http.POST[JsValue, HttpResponse](schemeAdminRegisterUrl, registerData)(implicitly, implicitly, hc, implicitly) map {
      handlePostResponse(_, schemeAdminRegisterUrl)
    } andThen logFailures("register PSA", registerData, psaSchema)
  }

  override def getPSASubscriptionDetails(psaId: String)(implicit
                                                        headerCarrier: HeaderCarrier,
                                                        ec: ExecutionContext,
                                                        request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeader(headerCarrier))

    val subscriptionDetailsUrl = config.psaSubscriptionDetailsUrl.format(psaId)

    http.GET[HttpResponse](subscriptionDetailsUrl)(
      implicitly[HttpReads[HttpResponse]],
      implicitly[HeaderCarrier](hc),
      implicitly) map {
      handleGetResponse(_, subscriptionDetailsUrl)
    } andThen schemeAuditService.sendPSADetailsEvent(psaId)(auditService.sendEvent) andThen logWarning("PSA subscription details")

  }

  override def removePSA(psaToBeRemoved: PsaToBeRemovedFromScheme)(implicit
                                                                   headerCarrier: HeaderCarrier,
                                                                   ec: ExecutionContext,
                                                                   request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val removePsaSchema = "/resources/schemas/removePsa.json"

    val removePsaUrl = config.removePsaUrl.format(psaToBeRemoved.psaId, psaToBeRemoved.pstr)

    val data: JsValue = Json.obj("ceaseDate" -> psaToBeRemoved.removalDate.toString)

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeader(headerCarrier))

    http.POST[JsValue, HttpResponse](removePsaUrl, data)(implicitly, implicitly, hc, implicitly) map {
      handlePostResponse(_, removePsaUrl)
    } andThen {
      case Success(Right(_)) =>
        auditService.sendEvent(PSARemovalFromSchemeAuditEvent(psaToBeRemoved))
    } andThen logFailures("remove PSA", data, removePsaSchema)
  }

  override def deregisterPSA(psaId: String)(implicit
                                            headerCarrier: HeaderCarrier,
                                            ec: ExecutionContext,
                                            request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val deregisterPsaSchema = "/resources/schemas/deregisterPsa.json"

    val deregisterPsaUrl = config.deregisterPsaUrl.format(psaId)

    val data: JsValue = Json.obj("deregistrationDate" -> LocalDate.now().toString, "reason" -> "1")

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeader(headerCarrier))

    http.POST[JsValue, HttpResponse](deregisterPsaUrl, data)(implicitly, implicitly, hc, implicitly) map {
      handlePostResponse(_, deregisterPsaUrl)
    } andThen logFailures("deregister PSA", data, deregisterPsaSchema)
  }

  override def updatePSA(psaId: String, data: JsValue)(implicit
                                                       headerCarrier: HeaderCarrier,
                                                       ec: ExecutionContext,
                                                       request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val psaVariationSchema = "/resources/schemas/psaVariation.json"

    val psaVariationUrl = config.psaVariationDetailsUrl.format(psaId)

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeader(headerCarrier))

    http.POST[JsValue, HttpResponse](psaVariationUrl, data)(implicitly, implicitly, hc, implicitly) map {
      handlePostResponse(_, psaVariationUrl)
    } andThen logFailures("Update PSA", data, psaVariationSchema)
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
      case OK => Right(validateJson(response.json))
      case status => Left(handleErrorResponse("PSA Subscription details", url, response, badResponseSeq))
    }

  }

  private def logFailures(endpoint: String, data: JsValue, schemaPath: String): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {
    case Success(Left(e: BadRequestException)) if e.message.contains("INVALID_PAYLOAD") =>
      invalidPayloadHandler.logFailures(schemaPath)(data)
    case Success(Left(e: HttpResponse)) => Logger.warn(s"$endpoint received error response from DES", e)
  }

  private def validateJson(json: JsValue): JsValue = {

    val temporaryMappingTest = json.transform(psaSubscriptionDetailsTransformer.transformToUserAnswers)
    if (temporaryMappingTest.isSuccess)
      Logger.warn("PensionAdministratorSuccessfulMapToUserAnswers")
    else {
      Logger.warn(s"PensionAdministratorFailedMapToUserAnswers - [$temporaryMappingTest]")
    }


    json.validate[PsaSubscription] match {
      case JsSuccess(value, _) =>  if(fs.get(IsVariationsEnabled)) {
        json.transform(psaSubscriptionDetailsTransformer.transformToUserAnswers).getOrElse(throw new PSAFailedMapToUserAnswersException)
      } else {
        Json.toJson(value)
      }
      case JsError(errors) => throw new JsResultException(errors)
    }
  }

}
