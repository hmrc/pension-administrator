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

import audit.{AssociationAuditService, AuditService}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.AppConfig
import connectors.helper.HeaderUtils
import models._
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{BadRequestException, _}
import utils.{ErrorHandler, HttpResponseHelper, InvalidPayloadHandler}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[AssociationConnectorImpl])
trait AssociationConnector {

  def getMinimalDetails(idValue: String, idType: String, regime: String)
                       (implicit
                        headerCarrier: HeaderCarrier,
                        ec: ExecutionContext,
                        request: RequestHeader): Future[Either[HttpException, MinimalDetails]]

  def acceptInvitation(invitation: AcceptedInvitation)
                      (implicit
                       headerCarrier: HeaderCarrier,
                       ec: ExecutionContext,
                       request: RequestHeader): Future[Either[HttpException, Unit]]

}

@Singleton
class AssociationConnectorImpl @Inject()(
                                          httpV2Client: HttpClientV2,
                                          appConfig: AppConfig,
                                          invalidPayloadHandler: InvalidPayloadHandler,
                                          headerUtils: HeaderUtils,
                                          auditService: AuditService
                                        )
  extends AssociationConnector
    with HttpResponseHelper
    with ErrorHandler
    with AssociationAuditService {

  import AssociationConnectorImpl._

  private val logger = Logger(classOf[AssociationConnector])

  override def getMinimalDetails(idValue: String, idType: String, regime: String)
                                (implicit
                                 headerCarrier: HeaderCarrier,
                                 ec: ExecutionContext,
                                 request: RequestHeader): Future[Either[HttpException, MinimalDetails]] = {

    val minimalDetailsUrl = url"${appConfig.psaMinimalDetailsUrl.format(regime, idType, idValue)}"
    httpV2Client.get(minimalDetailsUrl).setHeader(headerUtils.integrationFrameworkHeader: _*).execute[HttpResponse] map {
      handleResponseIF(_, minimalDetailsUrl.toString)
    } andThen sendGetMinimalDetailsEvent(idType, idValue)(auditService.sendEvent) andThen logWarning("IF PSA minimal details")
  }

  override def logWarning[A](endpoint: String): PartialFunction[Try[Either[Throwable, A]], Unit] = {
    case Success(Left(e: HttpException))
      if !e.getMessage.contains("DELIMITED_PSPID") && !e.getMessage.contains("DELIMITED_PSAID") &&
        !e.getMessage.contains("PSAID_NOT_FOUND") && !e.getMessage.contains("PSPID_NOT_FOUND")=>
        logger.warn(s"$endpoint received error response from DES", e)
    case Failure(e) =>
      logger.error(s"$endpoint received error response from DES", e)
  }

  private def handleResponseIF(response: HttpResponse, url: String): Either[HttpException, MinimalDetails] = {
    val badResponseSeq = Seq("INVALID_IDTYPE", "INVALID_PAYLOAD", "INVALID_CORRELATIONID", "INVALID_REGIME")
    response.status match {
      case OK =>
        logger.debug(s"Get minimal details from IF returned OK with response ${response.json}")
        response.json.validate[MinimalDetails](MinimalDetails.minimalDetailsIFReads).fold(
          _ => {
            invalidPayloadHandler.logFailures("/resources/schemas/getMinDetails1442.json")(response.json)
            Left(new BadRequestException("INVALID PAYLOAD"))
          },
          value => {
            logger.debug(s"Get minimal details from IF transformed model: $value")
            Right(value)
          }
        )
      case FORBIDDEN if response.body.contains("DELIMITED_PSPID") || response.body.contains("DELIMITED_PSAID") =>
        Left(
          new HttpException(response.body, FORBIDDEN)
        )
      case NOT_FOUND if response.body.contains("PSPID_NOT_FOUND") || response.body.contains("PSAID_NOT_FOUND") =>
        logger.info("Invalid PSP/PSA ID entered by user.")
        Left(
          new HttpException(response.body, NOT_FOUND)
        )
      case _ =>
        Left(handleErrorResponse("Minimal details", url, response, badResponseSeq))
    }
  }

  private def processFailureResponse(response: HttpResponse, url: String, schemaFile: String): Either[HttpException, Unit] = {
    logger.warn(s"POST or $url returned ${response.status} with body ${response.body}")

    response.status match {
      case FORBIDDEN if response.body.contains("ACTIVE_RELATIONSHIP_EXISTS") => Left(new ConflictException("ACTIVE_RELATIONSHIP_EXISTS"))
      case FORBIDDEN if response.body.contains("INVALID_INVITER_PSAID") => Left(new BadRequestException("INVALID_INVITER_PSAID"))
      case FORBIDDEN if response.body.contains("INVALID_INVITEE_PSAID") => Left(new BadRequestException("INVALID_INVITEE_PSAID"))
      case BAD_REQUEST if response.body.contains("INVALID_PAYLOAD") =>
        invalidPayloadHandler.logFailures(s"/resources/schemas/$schemaFile", url)(response.json)
        Left(new BadRequestException("INVALID PAYLOAD"))
      case _ => Left(handleErrorResponse("POST", url, response, Seq("INVALID_PSTR", "INVALID_CORRELATION_ID")))
    }

  }

  private def processResponse(acceptedInvitation: AcceptedInvitation,
                              response: HttpResponse, url: String, schemaFile: String)
                             (implicit request: RequestHeader, ec: ExecutionContext): Either[HttpException, Unit] = {

    sendAcceptInvitationAuditEvent(acceptedInvitation, response.status,
      if (response.body.isEmpty) None else Some(response.json))(auditService.sendEvent)

    if (response.status == OK) {
      logger.info(s"POST of $url returned successfully")
      Right(())
    } else {
      processFailureResponse(response, url, schemaFile)
    }
  }

  def acceptInvitation(acceptedInvitation: AcceptedInvitation)
                      (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, Unit]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders =
      headerUtils.integrationFrameworkHeader)
    val url = url"${appConfig.createPsaAssociationUrl.format(acceptedInvitation.pstr)}"

    val data = Json.toJson(acceptedInvitation)(writesIFAcceptedInvitation)
    association(url, data, acceptedInvitation, "createAssociationRequest1445.json", headerUtils.integrationFrameworkHeader)(hc, implicitly, implicitly)
  }

  private def association(url: java.net.URL, data: JsValue, acceptedInvitation: AcceptedInvitation,
                          schemaFile: String,
                          headers: Seq[(String, String)])
                         (implicit hc: HeaderCarrier, ec: ExecutionContext,
                          request: RequestHeader): Future[Either[HttpException, Unit]] = {
    logger.debug(s"[Accept-Invitation-Outgoing-Payload] - ${data.toString()}")
    httpV2Client.post(url)
      .setHeader(headers: _*)
      .withBody(data).execute[HttpResponse] map (processResponse(acceptedInvitation, _, url.toString, schemaFile))
  }
}

object AssociationConnectorImpl {

  private def optional(key: String, value: Option[String]): Map[String, JsValue] = {
    value match {
      case Some(v) => Map(key -> JsString(v))
      case _ => Map.empty
    }
  }

  private val ifAddressWrites: Writes[Address] = Writes {
    case ukAddress: UkAddress =>
      val underlying = Map[String, JsValue](
        "nonUKAddress" -> JsString("false"),
        "addressLine1" -> JsString(ukAddress.addressLine1),
        "postalCode" -> JsString(ukAddress.postalCode),
        "countryCode" -> JsString(ukAddress.countryCode)
      ) ++
        optional("addressLine2", ukAddress.addressLine2) ++
        optional("addressLine3", ukAddress.addressLine3) ++
        optional("addressLine4", ukAddress.addressLine4)
      JsObject(underlying)
    case nonUkAddress: InternationalAddress =>
      val underlying = Map[String, JsValue](
        "nonUKAddress" -> JsString("true"),
        "addressLine1" -> JsString(nonUkAddress.addressLine1),
        "countryCode" -> JsString(nonUkAddress.countryCode)
      ) ++
        optional("addressLine2", nonUkAddress.addressLine2) ++
        optional("addressLine3", nonUkAddress.addressLine3) ++
        optional("addressLine4", nonUkAddress.addressLine4) ++
        optional("postalCode", nonUkAddress.postalCode)
      JsObject(underlying)
  }

  private val writesIFAcceptedInvitation: Writes[AcceptedInvitation] = Writes {
    { invite =>
      val pensionAdviserDetails = invite.pensionAdviserDetails match {
        case Some(adviser) => Json.obj(
          "pensionAdviserDetails" -> Json.obj(
            "name" -> adviser.name,
            "addressDetails" -> ifAddressWrites.writes(adviser.addressDetail),
            "contactDetails" -> Json.obj(
              "email" -> adviser.email
            )
          )
        )
        case _ => Json.obj()
      }

      val declarationDuties: JsObject =
        if (invite.declarationDuties) {
          Json.obj("box5" -> JsBoolean(true))
        } else {
          Json.obj(
            "box6" -> JsBoolean(true)) ++ pensionAdviserDetails
        }

      val declarationDetails = Json.obj(
        "box1" -> invite.declaration,
        "box2" -> invite.declaration,
        "box3" -> invite.declaration,
        "box4" -> invite.declaration
      ) ++ declarationDuties

      Json.obj(
        "psaAssociationIDsDetails" -> Json.obj(
          "inviteeIDType" -> "PSAID",
          "inviteeIDNumber" -> invite.inviteePsaId,
          "inviterPSAID" -> invite.inviterPsaId
        ),
        "psaDeclarationDetails" -> declarationDetails
      )
    }
  }
}
