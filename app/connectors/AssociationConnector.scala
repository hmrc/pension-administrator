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

import akka.util.LineNumbers.Result
import com.google.inject.{Inject, Singleton}
import config.AppConfig
import connectors.helper.HeaderUtils
import play.api.http.Status._
import play.api.libs.json.JsValue
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AssociationConnector@Inject()(httpClient: HttpClient,
                                    appConfig : AppConfig,
                                    headerUtils: HeaderUtils) extends HttpErrorFunctions{

  implicit val rds: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse): HttpResponse = response
  }

  def getPSAMinimalDetails(psaId : String)(implicit
                                           headerCarrier: HeaderCarrier,
                                           ec: ExecutionContext): Future[Either[HttpException,JsValue]] = {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeader(headerCarrier))

    val getURL = appConfig.psaMinimalDetailsUrl.format(psaId)

    httpClient.GET(getURL)(implicitly[HttpReads[HttpResponse]], implicitly[HeaderCarrier](hc),
      implicitly[ExecutionContext]) map handleResponse

  }

  private def handleResponse(response: HttpResponse): Either[HttpException, JsValue] = {
    val badResponseSeq = Seq("INVALID_PSAID", "INVALID_CORRELATIONID")
    response.status match {
      case NOT_FOUND => Left(new NotFoundException(response.body))
      case status => handleCommonResponse(response, badResponseSeq)
    }
  }

  private def handleCommonResponse(response: HttpResponse,  badResponseSeq: Seq[String]): Either[HttpException, JsValue] = {
    response.status match {
      case OK => Right(response.json)
      case BAD_REQUEST if badResponseSeq.exists(response.body.contains(_)) => Left(new BadRequestException(response.body))
      case status if is4xx(status) => throw Upstream4xxResponse(response.body, status, BAD_REQUEST, response.allHeaders)
      case status if is5xx(status) => throw Upstream5xxResponse(response.body, status, BAD_GATEWAY)
      case status => throw new Exception(s"PSA minimal details failed with status $status. Response body: '${response.body}'")
    }
  }

}