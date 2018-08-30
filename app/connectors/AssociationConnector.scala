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

import com.google.inject.{Inject, Singleton}
import config.AppConfig
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HttpException, HttpReads, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AssociationConnector@Inject()(httpClient: HttpClient, appConfig : AppConfig) {

  implicit val rds: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse): HttpResponse = response
  }

  def getPSAMinimalDetails(psaId : String)(implicit
                                           headerCarrier: HeaderCarrier,
                                           ec: ExecutionContext): Future[Either[HttpException,JsValue]] = {

    val getURL = appConfig.psaMinimalDetailsUrl.format(psaId)

    httpClient.GET(getURL) map{ result =>
      result.status match {
      case 200 => Right(result.json)
      case _ => Left(new HttpException("exception", 400))
    }

  }

}