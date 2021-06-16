/*
 * Copyright 2021 HM Revenue & Customs
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
import com.google.inject.{Inject, Singleton, ImplementedBy}
import config.AppConfig
import connectors.helper.HeaderUtils
import models._
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HttpClient, BadRequestException, _}
import utils.{ErrorHandler, InvalidPayloadHandler, HttpResponseHelper}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AssociationConnectorImpl])
trait AssociationConnector {

  def findMinimalDetailsByID(idValue: String, idType: String, regime: String)
    (implicit
      headerCarrier: HeaderCarrier,
      ec: ExecutionContext,
      request: RequestHeader): Future[Either[HttpException, Option[MinimalDetails]]]

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
                                          httpClient: HttpClient,
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

  private val logger = Logger(classOf[AssociationConnectorImpl])

  override def findMinimalDetailsByID(idValue: String, idType: String, regime: String)
    (implicit
      headerCarrier: HeaderCarrier,
      ec: ExecutionContext,
      request: RequestHeader): Future[Either[HttpException, Option[MinimalDetails]]] = {
        retrieveMinimalDetails(idValue, idType, regime)
          .map{
            case Left(ex) if ex.responseCode == NOT_FOUND => Right(None)
            case response => response.map(Option(_))
          } andThen logWarning("IF PSA minimal details")
    }

  override def getMinimalDetails(idValue: String, idType: String, regime: String)
                                (implicit
                                 headerCarrier: HeaderCarrier,
                                 ec: ExecutionContext,
                                 request: RequestHeader): Future[Either[HttpException, MinimalDetails]] =
    retrieveMinimalDetails(idValue, idType, regime) andThen logWarning("IF PSA minimal details")

  private def retrieveMinimalDetails(idValue: String, idType: String, regime: String)
    (implicit
      headerCarrier: HeaderCarrier,
      ec: ExecutionContext,
      request: RequestHeader): Future[Either[HttpException, MinimalDetails]] = {
      implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders =
        headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier)))
      val minimalDetailsUrl = appConfig.psaMinimalDetailsUrl.format(regime, idType, idValue)
      httpClient.GET(minimalDetailsUrl)(implicitly[HttpReads[HttpResponse]], implicitly[HeaderCarrier](hc),
        implicitly) map {
        handleResponseIF(_, minimalDetailsUrl)
      } andThen sendGetMinimalDetailsEvent(idType, idValue)(auditService.sendEvent)
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
        headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier)))
      val url = appConfig.createPsaAssociationUrl.format(acceptedInvitation.pstr)
      val data = Json.toJson(acceptedInvitation)(writesIFAcceptedInvitation)
      association(url, data, acceptedInvitation, "createAssociationRequest1445.json")(hc, implicitly, implicitly)
    }

  private def association(url: String, data: JsValue, acceptedInvitation: AcceptedInvitation, schemaFile: String)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext,
                          request: RequestHeader): Future[Either[HttpException, Unit]] = {
    logger.debug(s"[Accept-Invitation-Outgoing-Payload] - ${data.toString()}")
    httpClient.POST[JsValue, HttpResponse](url, data)(
      implicitly, implicitly, hc, implicitly
    ) map (processResponse(acceptedInvitation, _, url, schemaFile))
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
