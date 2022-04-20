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

import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import connectors.helper.HeaderUtils
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HttpClient, _}
import utils.{ErrorHandler, HttpResponseHelper, InvalidPayloadHandler}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[UpdateClientReferenceConnectorImpl])
trait UpdateClientReferenceConnector {

  def updateClientReference(jsValue: JsValue)(
    implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, JsValue]]
}

class UpdateClientReferenceConnectorImpl @Inject()(
                                           http: HttpClient,
                                           config: AppConfig,
                                           headerUtils: HeaderUtils,
                                           invalidPayloadHandler: InvalidPayloadHandler
                                         ) extends UpdateClientReferenceConnector with HttpResponseHelper with ErrorHandler {

  override def updateClientReference(jsValue: JsValue)
                                       (implicit headerCarrier: HeaderCarrier,
                                        ec: ExecutionContext,
                                        request: RequestHeader): Future[Either[HttpException, JsValue]] = {
    val updateClientReferenceUrl = config.updateClientReferenceUrl
    val schema = "/resources/schemas/updateClientReference1857.json"

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders =
      headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier)))
    http.POST(updateClientReferenceUrl, jsValue)(implicitly, implicitly[HttpReads[HttpResponse]], hc, implicitly) map {
      handleResponse[JsValue](_, updateClientReferenceUrl, schema,  jsValue, "Update Client Reference")
    }

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

      case _ =>
        Left(handleErrorResponse(methodContext, url, response, Seq.empty))
    }
  }
}
