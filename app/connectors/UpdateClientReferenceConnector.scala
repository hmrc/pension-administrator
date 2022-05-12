/*
 * Copyright 2022 HM Revenue & Customs
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

import audit.{AuditService, UpdateClientReferenceAuditService}
import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import connectors.helper.HeaderUtils
import models.{IdentifierDetails, UpdateClientReferenceRequest}
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HttpClient, _}
import utils.{ErrorHandler, HttpResponseHelper, JSONPayloadSchemaValidator}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[UpdateClientReferenceConnectorImpl])
trait UpdateClientReferenceConnector {

  def updateClientReference(updateClientReferenceRequest: UpdateClientReferenceRequest, userAction: Option[String])(
    implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, JsValue]]
}

case class UpdateClientRefValidationFailureException(error: String) extends Exception(error)

class UpdateClientReferenceConnectorImpl @Inject()(
                                                    http: HttpClient,
                                                    config: AppConfig,
                                                    headerUtils: HeaderUtils,
                                                    jsonPayloadSchemaValidator: JSONPayloadSchemaValidator,
                                                    auditService: AuditService
                                                  ) extends UpdateClientReferenceConnector
  with HttpResponseHelper with ErrorHandler with UpdateClientReferenceAuditService {

  override def updateClientReference(updateClientReferenceRequest: UpdateClientReferenceRequest, userAction: Option[String])
                                    (implicit headerCarrier: HeaderCarrier,
                                     ec: ExecutionContext,
                                     request: RequestHeader): Future[Either[HttpException, JsValue]] = {
    val updateClientReferenceUrl = config.updateClientReferenceUrl
    val schema = "/resources/schemas/updateClientReference1857.json"
    val jsValue = Json.toJson(new IdentifierDetails(updateClientReferenceRequest))
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders =
      headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier)))
    val validationResult = jsonPayloadSchemaValidator.validateJsonPayload(schema, jsValue)
    if(validationResult.nonEmpty)
      throw UpdateClientRefValidationFailureException(s"Invalid payload when updateClientReference :-\n${validationResult.mkString}")
      else
      http.PUT[JsValue, HttpResponse](updateClientReferenceUrl, jsValue)(implicitly, implicitly[HttpReads[HttpResponse]], hc, implicitly) map {
        handlePostResponse(_, updateClientReferenceUrl)
      } andThen sendClientReferenceEvent(updateClientReferenceRequest, userAction)(auditService.sendEvent)
  }

  private def handlePostResponse(response: HttpResponse, url: String): Either[HttpException, JsValue] = {

    val badResponseSeq = Seq("INVALID_CORRELATION_ID", "INVALID_PAYLOAD", "INVALID_REGIME")

    response.status match {
      case OK => Right(response.json)
      case _ => Left(handleErrorResponse("Update Client Reference", url, response, badResponseSeq))
    }
  }
}
