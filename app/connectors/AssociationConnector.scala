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

import audit.{AuditService, InvitationAcceptanceAuditEvent}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.AppConfig
import connectors.helper.HeaderUtils
import models._
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.{ErrorHandler, HttpResponseHelper, InvalidPayloadHandler}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AssociationConnectorImpl])
trait AssociationConnector {

  def getPSAMinimalDetails(psaId: PsaId)(implicit
                                          headerCarrier: HeaderCarrier,
                                          ec: ExecutionContext): Future[Either[HttpException, PSAMinimalDetails]]

  def acceptInvitation(invitation: AcceptedInvitation)
                      (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, Unit]]

}

@Singleton
class AssociationConnectorImpl @Inject()(httpClient: HttpClient,
                                         appConfig: AppConfig,
                                         logger: LoggerLike,
                                         invalidPayloadHandler: InvalidPayloadHandler,
                                         headerUtils: HeaderUtils,
                                         auditService: AuditService) extends AssociationConnector with HttpResponseHelper with ErrorHandler {

  import AssociationConnectorImpl._

  def getPSAMinimalDetails(psaId: PsaId)(implicit
                                          headerCarrier: HeaderCarrier,
                                          ec: ExecutionContext): Future[Either[HttpException, PSAMinimalDetails]] = {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeader(headerCarrier))

    val minimalDetailsUrl = appConfig.psaMinimalDetailsUrl.format(psaId)

    httpClient.GET(minimalDetailsUrl)(implicitly[HttpReads[HttpResponse]], implicitly[HeaderCarrier](hc),
      implicitly) map {
      handleResponse(_, minimalDetailsUrl)
    } andThen logWarning("PSA minimal details")

  }

  private def handleResponse(response: HttpResponse, url: String): Either[HttpException, PSAMinimalDetails] = {
    val badResponseSeq = Seq("INVALID_PSAID", "INVALID_CORRELATIONID")
    response.status match {
      case OK =>
        response.json.validate[PSAMinimalDetails].fold(
          _ => Left(new BadRequestException("INVALID PAYLOAD")),
          value =>
            Right(value)
        )
      case _ => Left(handleErrorResponse("PSA minimal details", url, response, badResponseSeq))
    }
  }

  private def processFailureResponse(response: HttpResponse, url: String): Either[HttpException, Unit] = {
    Logger.warn(s"POST or $url returned ${response.status} with body ${response.body}")

    response.status match {
      case FORBIDDEN if response.body.contains("ACTIVE_RELATIONSHIP_EXISTS") => Left(new ConflictException("ACTIVE_RELATIONSHIP_EXISTS"))
      case FORBIDDEN if response.body.contains("INVALID_INVITER_PSAID") => Left(new BadRequestException("INVALID_INVITER_PSAID"))
      case FORBIDDEN if response.body.contains("INVALID_INVITEE_PSAID") => Left(new BadRequestException("INVALID_INVITEE_PSAID"))
      case BAD_REQUEST if response.body.contains("INVALID_PAYLOAD") =>
        invalidPayloadHandler.logFailures("/resources/schemas/createPsaAssociationRequest.json", response.json)
        Left(new BadRequestException("INVALID PAYLOAD"))
      case _ => Left(handleErrorResponse("POST", url, response, Seq("INVALID_PSTR", "INVALID_CORRELATION_ID")))
    }

  }

  private def processResponse(acceptedInvitation: AcceptedInvitation,
                              response: HttpResponse, url: String)(implicit request: RequestHeader, ec: ExecutionContext) = {
    sendAcceptInvitationAuditEvent(acceptedInvitation, response.status,
      if (response.body.isEmpty) None else Some(response.json))
    if (response.status == OK) {
      Logger.info(s"POST of $url returned successfully")
      Right(())
    } else {
      processFailureResponse(response, url)
    }
  }

  private def sendAcceptInvitationAuditEvent(acceptedInvitation: AcceptedInvitation,
                                             status: Int,
                                             response: Option[JsValue])(implicit request: RequestHeader, ec: ExecutionContext): Unit =
    auditService.sendEvent(InvitationAcceptanceAuditEvent(acceptedInvitation, status, response))

  def acceptInvitation(acceptedInvitation: AcceptedInvitation)
                      (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, Unit]] = {
    val headerCarrierWithDesHeaders: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeader(headerCarrier))
    val url = appConfig.createPsaAssociationUrl.format(acceptedInvitation.pstr)

    httpClient.POST[AcceptedInvitation, HttpResponse](url, acceptedInvitation)(
      writesAcceptedInvitation, implicitly, headerCarrierWithDesHeaders, implicitly) map (processResponse(acceptedInvitation,_, url))
  }
}

object AssociationConnectorImpl {

  private def optional(key: String, value: Option[String]): Map[String, JsValue] = {
    value match {
      case Some(v) => Map(key -> JsString(v))
      case _ => Map.empty
    }
  }

  private val addressWrites: Writes[Address] = Writes {
    case ukAddress: UkAddress =>
      val underlying = Map[String, JsValue](
        "nonUKAddress" -> JsBoolean(false),
        "line1" -> JsString(ukAddress.addressLine1),
        "postalCode" -> JsString(ukAddress.postalCode),
        "countryCode" -> JsString(ukAddress.countryCode)
      ) ++
        optional("line2", ukAddress.addressLine2) ++
        optional("line3", ukAddress.addressLine3) ++
        optional("line4", ukAddress.addressLine4)
      JsObject(underlying)
    case nonUkAddress: InternationalAddress =>
      val underlying = Map[String, JsValue](
        "nonUKAddress" -> JsBoolean(true),
        "line1" -> JsString(nonUkAddress.addressLine1),
        "countryCode" -> JsString(nonUkAddress.countryCode)
      ) ++
        optional("line2", nonUkAddress.addressLine2) ++
        optional("line3", nonUkAddress.addressLine3) ++
        optional("line4", nonUkAddress.addressLine4) ++
        optional("postalCode", nonUkAddress.postalCode)
      JsObject(underlying)
  }

  val writesAcceptedInvitation: Writes[AcceptedInvitation] = Writes {
    { invite =>
      val pensionAdviserDetails = invite.pensionAdviserDetails match {
        case Some(adviser) => Json.obj(
          "pensionAdviserDetails" -> Json.obj(
            "name" -> adviser.name,
            "addressDetails" -> addressWrites.writes(adviser.addressDetail),
            "contactDetails" -> Json.obj(
              "email" -> adviser.email
            )
          )
        )
        case _ => Json.obj()
      }

      val declarationDuties: (String, JsBoolean) =
        if (invite.declarationDuties) {
          "box5" -> JsBoolean(true)
        }
        else {
          "box6" -> JsBoolean(true)
        }

      val declarationDetails = Json.obj(
        "box1" -> invite.declaration,
        "box2" -> invite.declaration,
        "box3" -> invite.declaration,
        "box4" -> invite.declaration
      ) + declarationDuties ++ pensionAdviserDetails

      Json.obj(
        "psaAssociationDetails" -> Json.obj(
          "psaAssociationIDsDetails" -> Json.obj(
            "inviteePSAID" -> invite.inviteePsaId,
            "inviterPSAID" -> invite.inviterPsaId
          ),
          "declarationDetails" -> declarationDetails
        )
      )
    }
  }
}
