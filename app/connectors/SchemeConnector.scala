/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.{PsaId, PspId}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import utils.{ErrorHandler, HttpResponseHelper}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[SchemeConnectorImpl])
trait SchemeConnector {

  def checkForAssociation(psaIdOrPspId: Either[PsaId, PspId], srn: SchemeReferenceNumber)
                         (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpException, Boolean]]

  def listOfSchemes(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpException, JsValue]]

  def getSchemeDetails(schemeIdType: String, idNumber: String, srn: SchemeReferenceNumber)
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpException, JsValue]]
}

class SchemeConnectorImpl @Inject()(httpV2Client: HttpClientV2, config: AppConfig)
                                   (implicit val ec: ExecutionContext)
  extends SchemeConnector
    with HttpResponseHelper
    with ErrorHandler
    with Logging {

  override def checkForAssociation(psaIdOrPspId: Either[PsaId, PspId], srn: SchemeReferenceNumber)
                                  (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpException, Boolean]] = {

    val id: (String, String) = psaIdOrPspId match {
      case Left(psaId) => ("psaId", psaId.value)
      case Right(pspId) => ("pspId", pspId.value)
    }

    val headers: Seq[(String, String)] = Seq(id, ("schemeReferenceNumber", srn), ("Content-Type", "application/json"))
    val url = url"${config.checkAssociationUrl}"
                                    
    httpV2Client
      .get(url)
      .setHeader(headers *)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            response
              .json
              .validate[Boolean]
              .fold(_ =>
                Left(new InternalServerException("Response from pension-scheme cannot be parsed to boolean")),
                Right(_)
              )
          case _ =>
            Left(handleErrorResponse(
              methodContext  = s"Check for Association with headers: ${headers.toString}",
              url            = url.toString,
              response       = response,
              badResponseSeq = Seq("Bad Request with missing parameters PSA Id or SRN")
            ))
        }
    }

  }

  def listOfSchemes(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpException, JsValue]] = {
    val headers = Seq(("idType", "PSA"), ("Content-Type", "application/json"))
    val url = url"${config.listOfSchemesUrl}"

    httpV2Client
      .get(url)
      .setHeader(headers *)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Right(response.json)
          case _ =>
            Left(handleErrorResponse(
              methodContext  = s"List schemes with headers: ${headers.toString}",
              url            = url.toString,
              response       = response,
              badResponseSeq = Seq("Bad Request with missing parameter PSA Id")
            ))
      }
    }
  }

  def getSchemeDetails(schemeIdType: String, idNumber: String, srn: SchemeReferenceNumber)
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpException, JsValue]] = {

    val url = url"${config.getSchemeDetailsUrl(srn)}"
    val headers = Seq(("schemeIdType", schemeIdType), ("idNumber", idNumber))

    httpV2Client
      .get(url)
      .setHeader(headers *)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Right(Json.parse(response.body))
          case _ =>
            Left(handleErrorResponse(
              methodContext  = "GET",
              url            = url.toString,
              response       = response,
              badResponseSeq = Seq.empty
            ))
        }
      }
      .andThen {
        case Failure(t: Throwable) =>
          logger.warn("Unable to get scheme details in canPsaRegister call", t)
      }
  }
}
