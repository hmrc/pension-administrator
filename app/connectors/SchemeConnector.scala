/*
 * Copyright 2024 HM Revenue & Customs
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
import models.SchemeReferenceNumber
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpClient, _}
import utils.{ErrorHandler, HttpResponseHelper}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[SchemeConnectorImpl])
trait SchemeConnector {

  def checkForAssociation(psaId: PsaId, srn: SchemeReferenceNumber)
                         (implicit headerCarrier: HeaderCarrier,
                          ec: ExecutionContext,
                          request: RequestHeader): Future[Either[HttpException, JsValue]]

  def listOfSchemes(psaId: String)
                   (implicit headerCarrier: HeaderCarrier,
                    ec: ExecutionContext,
                    request: RequestHeader): Future[Either[HttpException, JsValue]]

  def getSchemeDetails(psaId: String, schemeIdType: String, idNumber: String)
                      (implicit hc: HeaderCarrier,
                       ec: ExecutionContext): Future[Either[HttpException, JsValue]]
}

class SchemeConnectorImpl @Inject()(
                                     httpV2Client: HttpClientV2,
                                     config: AppConfig
                                   ) extends SchemeConnector with HttpResponseHelper with ErrorHandler {

  private val logger = Logger(classOf[SchemeConnector])

  override def checkForAssociation(psaId: PsaId, srn: SchemeReferenceNumber)
                                  (implicit headerCarrier: HeaderCarrier,
                                   ec: ExecutionContext,
                                   request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val headers: Seq[(String, String)] = Seq(("psaId", psaId.value), ("schemeReferenceNumber", srn), ("Content-Type", "application/json"))


    httpV2Client.get(url"${config.checkAssociationUrl}")
      .setHeader(headers: _*)
      .execute[HttpResponse] map { response =>
      val badResponse = Seq("Bad Request with missing parameters PSA Id or SRN")
      response.status match {
        case OK => Right(response.json)
        case _ => Left(handleErrorResponse(s"Check for Association with headers: ${headers.toString}", config.checkAssociationUrl, response, badResponse))
      }
    }

  }

  def listOfSchemes(psaId: String)
                   (implicit headerCarrier: HeaderCarrier,
                    ec: ExecutionContext,
                    request: RequestHeader): Future[Either[HttpException, JsValue]] = {
    val headers = Seq(("idType", "psaid"), ("idValue", psaId), ("Content-Type", "application/json"))
    callListOfSchemes(url"${config.listOfSchemesUrl}", headers)
  }

  private def callListOfSchemes(url: java.net.URL, headers: Seq[(String, String)])
                               (implicit headerCarrier: HeaderCarrier,
                                ec: ExecutionContext): Future[Either[HttpException, JsValue]] = {

    httpV2Client.get(url).setHeader(headers: _*).execute[HttpResponse] map { response =>
      val badResponse = Seq("Bad Request with missing parameter PSA Id")
      response.status match {
        case OK => Right(response.json)
        case _ => Left(handleErrorResponse(s"List schemes with headers: ${headers.toString}", url.toString, response, badResponse))
      }
    }
  }

  override def getSchemeDetails(psaId: String, schemeIdType: String, idNumber: String)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext)
  : Future[Either[HttpException, JsValue]] = {

    val url = url"${config.getSchemeDetailsUrl}"
    val headers = Seq(("schemeIdType", schemeIdType), ("idNumber", idNumber), ("PSAId", psaId))

    httpV2Client.get(url).setHeader(headers: _*).execute[HttpResponse] map { response =>
      response.status match {
        case OK => Right(Json.parse(response.body))
        case _ => Left(handleErrorResponse("GET", url.toString, response, Seq.empty))
      }
    } andThen {
      case Failure(t: Throwable) => logger.warn("Unable to get scheme details in canPsaRegister call", t)
    }
  }
}
