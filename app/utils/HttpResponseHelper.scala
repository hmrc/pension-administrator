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

package utils

import uk.gov.hmrc.http._
import play.api.http.Status._

trait HttpResponseHelper extends HttpErrorFunctions {

  implicit val httpResponseReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse): HttpResponse = response
  }

  def handleErrorResponse(methodContext: String, url: String, response: HttpResponse, badResponseSeq: Seq[String]): HttpException =
    response.status match {
      case BAD_REQUEST if badResponseSeq.exists(response.body.contains(_)) => new BadRequestException(response.body)
      case NOT_FOUND => new NotFoundException(response.body)
      case status if is4xx(status) =>
        throw Upstream4xxResponse(upstreamResponseMessage(methodContext, url, status, response.body), status, status, response.allHeaders)
      case status if is5xx(status) =>
        throw Upstream5xxResponse(upstreamResponseMessage(methodContext, url, status, response.body), status, BAD_GATEWAY)
      case _ =>
        throw new UnrecognisedHttpResponseException(methodContext, url, response)
    }

}

class UnrecognisedHttpResponseException(method: String, url: String, response: HttpResponse)
  extends Exception(s"$method to $url failed with status ${response.status}. Response body: '${response.body}'")