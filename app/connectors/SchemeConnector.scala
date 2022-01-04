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

import com.google.inject.{Inject, ImplementedBy}
import config.AppConfig
import models.SchemeReferenceNumber
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.domain.PsaId
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
                                     http: HttpClient,
                                     config: AppConfig
                                   ) extends SchemeConnector with HttpResponseHelper with ErrorHandler {

  private val logger = Logger(classOf[SchemeConnector])

  override def checkForAssociation(psaId: PsaId, srn: SchemeReferenceNumber)
                                  (implicit headerCarrier: HeaderCarrier,
                                   ec: ExecutionContext,
                                   request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    val headers: Seq[(String, String)] = Seq(("psaId", psaId.value), ("schemeReferenceNumber", srn), ("Content-Type", "application/json"))

    implicit val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.GET[HttpResponse](config.checkAssociationUrl)(implicitly, hc, implicitly) map { response =>
      val badResponse = Seq("Bad Request with missing parameters PSA Id or SRN")
      response.status match {
        case OK => Right(response.json)
        case _ => Left(handleErrorResponse(s"Check for Association with headers: ${hc.headers _}", config.checkAssociationUrl, response, badResponse))
      }
    }

  }

  def listOfSchemes(psaId: String)
                   (implicit headerCarrier: HeaderCarrier,
                    ec: ExecutionContext,
                    request: RequestHeader): Future[Either[HttpException, JsValue]] = {
      val headers = Seq(("idType", "psaid"), ("idValue", psaId), ("Content-Type", "application/json"))
      callListOfSchemes(config.listOfSchemesUrl, headers)
  }

  private def callListOfSchemes(url: String, headers: Seq[(String, String)])
                               (implicit headerCarrier: HeaderCarrier,
                                ec: ExecutionContext,
                                request: RequestHeader): Future[Either[HttpException, JsValue]] = {
    implicit val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.GET[HttpResponse](url)(implicitly, hc, implicitly) map { response =>
      val badResponse = Seq("Bad Request with missing parameter PSA Id")
      response.status match {
        case OK => Right(response.json)
        case _ => Left(handleErrorResponse(s"List schemes with headers: ${hc.headers _}", url, response, badResponse))
      }
    }
  }

  override def getSchemeDetails(psaId: String, schemeIdType: String, idNumber: String)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext)
  : Future[Either[HttpException, JsValue]] = {

    val url = config.getSchemeDetailsUrl
    val schemeHc = hc.withExtraHeaders("schemeIdType" -> schemeIdType, "idNumber" -> idNumber, "PSAId" -> psaId)

    http.GET[HttpResponse](url)(implicitly, schemeHc, implicitly).map { response =>
      response.status match {
        case OK => Right(Json.parse(response.body))
        case _ => Left(handleErrorResponse("GET", url, response, Seq.empty))
      }
    } andThen {
      case Failure(t: Throwable) => logger.warn("Unable to get scheme details in canPsaRegister call", t)
    }
  }
}
