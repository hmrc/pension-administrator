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

import com.google.inject.{ImplementedBy, Singleton, Inject}
import config.AppConfig
import connectors.helper.HeaderUtils
import play.api.LoggerLike
import play.api.http.Status._
import play.api.libs.json.JsValue
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure, Try}


@ImplementedBy(classOf[AssociationConnectorImpl])
trait AssociationConnector {

  def getPSAMinimalDetails(psaId : String)(implicit
                                           headerCarrier: HeaderCarrier,
                                           ec: ExecutionContext): Future[Either[HttpException,JsValue]]
}

@Singleton
class AssociationConnectorImpl@Inject()(httpClient: HttpClient,
                                    appConfig : AppConfig,
                                    logger : LoggerLike,
                                    headerUtils: HeaderUtils) extends AssociationConnector with HttpResponseHelper{


  def getPSAMinimalDetails(psaId : String)(implicit
                                           headerCarrier: HeaderCarrier,
                                           ec: ExecutionContext): Future[Either[HttpException,JsValue]] = {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeader(headerCarrier))

    val getURL = appConfig.psaMinimalDetailsUrl.format(psaId)

    httpClient.GET(getURL)(implicitly[HttpReads[HttpResponse]], implicitly[HeaderCarrier](hc),
      implicitly) map { handleResponse(_, getURL) } andThen logWarningAndIssues("PSA minimal details")

  }

  private def handleResponse(response: HttpResponse, url: String): Either[HttpException, JsValue] = {
    val badResponseSeq = Seq("INVALID_PSAID", "INVALID_CORRELATIONID")
    response.status match {
      case OK => Right(response.json)
      case _ => Left(handleErrorResponse("PSA minimal details", url, response, badResponseSeq))
    }
  }

  private def logWarningAndIssues(endpoint: String): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {
    case Success(Left(e: HttpException)) => logger.warn(s"$endpoint received error response from DES", e)
    case Failure(e) => logger.error(s"$endpoint received error response from DES", e)
  }

}
