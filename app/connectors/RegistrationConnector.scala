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
import models.{OrganisationRegistrant, User}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.ErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class RegistrationConnectorImpl @Inject()(
                                           http: HttpClient,
                                           config: AppConfig,
                                           auditService: AuditService
                                         ) extends RegistrationConnector with HttpErrorFunctions with ErrorHandler with RegistrationAuditService {

  private val desHeader = Seq(
    "Environment" -> config.desEnvironment,
    "Authorization" -> config.authorization,
    "Content-Type" -> "application/json"
  )

  implicit val rds: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse): HttpResponse = response
  }

  override def registerWithIdIndividual(nino: String, user: User, registerData: JsValue)
                                       (implicit hc: HeaderCarrier,
                                        ec: ExecutionContext,
                                        request: RequestHeader): Future[Either[HttpException, JsValue]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader)

    val registerWithIdUrl = config.registerWithIdIndividualUrl.format(nino)

    Logger.debug(s"[Pensions-Scheme-Header-Carrier]-${desHeader.toString()}")

    http.POST(registerWithIdUrl, registerData, desHeader)(implicitly, implicitly[HttpReads[HttpResponse]], HeaderCarrier(), implicitly) map
      handleResponse andThen sendPSARegistrationEvent(
        withId = true, user, "Individual", registerData, withIdIsUk
      )(auditService.sendEvent) andThen logWarning("registerWithIdIndividual")

  }

  override def registerWithIdOrganisation(utr: String, user: User, registerData: JsValue)
                                         (implicit hc: HeaderCarrier,
                                          ec: ExecutionContext,
                                          request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val registerWithIdUrl = config.registerWithIdOrganisationUrl.format(utr)
    val psaType: String = organisationPsaType(registerData)

    http.POST(registerWithIdUrl, registerData, desHeader)(implicitly, implicitly[HttpReads[HttpResponse]], HeaderCarrier(), implicitly) map
      handleResponse andThen sendPSARegistrationEvent(
        withId = true, user, psaType, registerData, withIdIsUk
      )(auditService.sendEvent) andThen logWarning("registerWithIdOrganisation")

  }

  override def registrationNoIdOrganisation(user: User, registerData: OrganisationRegistrant)
                                           (implicit hc: HeaderCarrier,
                                            ec: ExecutionContext,
                                            request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader)
    val schemeAdminRegisterUrl = config.registerWithoutIdOrganisationUrl

    http.POST(schemeAdminRegisterUrl, registerData)(OrganisationRegistrant.apiWrites, implicitly[HttpReads[HttpResponse]], implicitly, implicitly) map
      handleResponse andThen sendPSARegistrationEvent(
        withId = false, user, "Organisation", Json.toJson(registerData), noIdIsUk(registerData)
      )(auditService.sendEvent) andThen logWarning("registrationNoIdOrganisation")

  }
  //scalastyle:off cyclomatic.complexity
  private def handleResponse(response: HttpResponse): Either[HttpException, JsValue] = {
    val badResponseSeq = Seq("INVALID_NINO", "INVALID_PAYLOAD", "INVALID_UTR")
    response.status match {
      case OK => Right(response.json)
      case BAD_REQUEST if badResponseSeq.exists(response.body.contains(_)) => Left(new BadRequestException(response.body))
      case NOT_FOUND => Left(new NotFoundException(response.body))
      case CONFLICT => Left(new ConflictException(response.body))
      case FORBIDDEN if response.body.contains("INVALID_SUBMISSION") => Left(new ForbiddenException(response.body))
      case status if is4xx(status) => throw Upstream4xxResponse(response.body, status, BAD_REQUEST, response.allHeaders)
      case status if is5xx(status) => throw Upstream5xxResponse(response.body, status, BAD_GATEWAY)
      case status => throw new Exception(s"Business Partner Matching fail with status $status. Response body: '${response.body}'")
    }
  }
  //scalastyle:on cyclomatic.complexity
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