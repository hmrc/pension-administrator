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
import models.{OrganisationRegistrant, User}
import play.Logger
import play.api.http.Status._
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.{ErrorHandler, HttpResponseHelper}

import scala.concurrent.{ExecutionContext, Future}

class RegistrationConnectorImpl @Inject()(
                                           http: HttpClient,
                                           config: AppConfig,
                                           auditService: AuditService,
                                           headerUtils: HeaderUtils
                                         ) extends RegistrationConnector with HttpResponseHelper with ErrorHandler with RegistrationAuditService {

  import RegistrationConnectorImpl._

  private val desHeader = Seq(
    "Environment" -> config.desEnvironment,
    "Authorization" -> config.authorization,
    "Content-Type" -> "application/json"
  )

  override def registerWithIdIndividual(nino: String, user: User, registerData: JsValue)
                                       (implicit hc: HeaderCarrier,
                                        ec: ExecutionContext,
                                        request: RequestHeader): Future[Either[HttpException, JsValue]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader)

    val registerWithIdUrl = config.registerWithIdIndividualUrl.format(nino)

    Logger.debug(s"[Pensions-Scheme-Header-Carrier]-${desHeader.toString()}")

    http.POST(registerWithIdUrl, registerData, desHeader)(implicitly, implicitly[HttpReads[HttpResponse]], HeaderCarrier(), implicitly) map {
      handleResponse(_, registerWithIdUrl)
    } andThen sendPSARegistrationEvent(
      withId = true, user, "Individual", registerData, withIdIsUk
    )(auditService.sendEvent) andThen logWarning("registerWithIdIndividual")

  }

  override def registerWithIdOrganisation(utr: String, user: User, registerData: JsValue)
                                         (implicit hc: HeaderCarrier,
                                          ec: ExecutionContext,
                                          request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val registerWithIdUrl = config.registerWithIdOrganisationUrl.format(utr)
    val psaType: String = organisationPsaType(registerData)

    http.POST(registerWithIdUrl, registerData, desHeader)(implicitly, implicitly[HttpReads[HttpResponse]], HeaderCarrier(), implicitly) map {
      handleResponse(_, registerWithIdUrl)
    } andThen sendPSARegistrationEvent(
      withId = true, user, psaType, registerData, withIdIsUk
    )(auditService.sendEvent) andThen logWarning("registerWithIdOrganisation")

  }

  override def registrationNoIdOrganisation(user: User, registerData: OrganisationRegistrant)
                                           (implicit headerCarrier: HeaderCarrier,
                                            ec: ExecutionContext,
                                            request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val hcWithDesHeaders: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeader(headerCarrier))
    val schemeAdminRegisterUrl = config.registerWithoutIdOrganisationUrl
    val correlationId = headerUtils.getCorrelationId(hcWithDesHeaders.requestId.map(_.value))

    val registerWithNoIdData = mandatoryWithoutIdData(correlationId).as[JsObject] ++
      Json.toJson(registerData)(OrganisationRegistrant.apiWrites).as[JsObject]

    http.POST(schemeAdminRegisterUrl, registerWithNoIdData)(implicitly, implicitly[HttpReads[HttpResponse]], hcWithDesHeaders, implicitly) map {
      handleResponse(_, schemeAdminRegisterUrl)
    } andThen sendPSARegistrationEvent(
      withId = false, user, "Organisation", Json.toJson(registerWithNoIdData), _ => Some(false)
    )(auditService.sendEvent) andThen logWarning("registrationNoIdOrganisation")
  }

  private def handleResponse(response: HttpResponse, url: String): Either[HttpException, JsValue] = {
    val badResponseSeq = Seq("INVALID_NINO", "INVALID_PAYLOAD", "INVALID_UTR")
    response.status match {
      case OK => Right(response.json)
      case CONFLICT => Left(new ConflictException(response.body))
      case FORBIDDEN if response.body.contains("INVALID_SUBMISSION") => Left(new ForbiddenException(response.body))
      case _ => Left(handleErrorResponse("Business Partner Matching", url, response, badResponseSeq))
    }
  }
}

object RegistrationConnectorImpl {

  private def mandatoryWithoutIdData(correlationId: String): JsValue = {
    Json.obj("regime" -> "PODA",
      "acknowledgementReference" -> correlationId,
      "isAnAgent" -> false,
      "isAGroup" -> false,
      "contactDetails" -> Json.obj(
        "phoneNumber" -> JsNull,
        "mobileNumber" -> JsNull,
        "faxNumber" -> JsNull,
        "emailAddress" -> JsNull
      )
    )
  }
}

@ImplementedBy(classOf[RegistrationConnectorImpl])
trait RegistrationConnector {
  def registerWithIdIndividual(nino: String, user: User, registerData: JsValue)(implicit
                                                                                hc: HeaderCarrier,
                                                                                ec: ExecutionContext,
                                                                                request: RequestHeader): Future[Either[HttpException, JsValue]]

  def registerWithIdOrganisation(utr: String, user: User, registerData: JsValue)(implicit
                                                                                 hc: HeaderCarrier,
                                                                                 ec: ExecutionContext,
                                                                                 request: RequestHeader): Future[Either[HttpException, JsValue]]

  def registrationNoIdOrganisation(user: User, registerData: OrganisationRegistrant)(implicit
                                                                                     hc: HeaderCarrier,
                                                                                     ec: ExecutionContext,
                                                                                     request: RequestHeader): Future[Either[HttpException, JsValue]]
}
