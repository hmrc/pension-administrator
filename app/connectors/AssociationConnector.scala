/*
 * Copyright 2020 HM Revenue & Customs
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

import audit.AssociationAuditService
import audit.AuditService
import com.google.inject.ImplementedBy
import com.google.inject.Inject
import com.google.inject.Singleton
import config.AppConfig
import connectors.helper.HeaderUtils
import models.FeatureToggle.Enabled
import models.FeatureToggleName.IntegrationFrameworkListSchemes
import models.FeatureToggleName.IntegrationFrameworkMinimalDetails
import models._
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import service.FeatureToggleService
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.ErrorHandler
import utils.HttpResponseHelper
import utils.InvalidPayloadHandler

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@ImplementedBy(classOf[AssociationConnectorImpl])
trait AssociationConnector {

  def getMinimalDetails(idValue: String, idType: String, regime: String)(implicit
                                          headerCarrier: HeaderCarrier,
                                          ec: ExecutionContext,
                                          request: RequestHeader): Future[Either[HttpException, MinimalDetails]]

  def acceptInvitation(invitation: AcceptedInvitation)
                      (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, Unit]]

}

@Singleton
class AssociationConnectorImpl @Inject()(httpClient: HttpClient,
                                         appConfig: AppConfig,
                                         invalidPayloadHandler: InvalidPayloadHandler,
                                         headerUtils: HeaderUtils,
                                         auditService: AuditService,
                                         featureToggleService: FeatureToggleService)
  extends AssociationConnector with HttpResponseHelper with ErrorHandler with AssociationAuditService {

  import AssociationConnectorImpl._

  override def getMinimalDetails(idValue: String, idType: String, regime: String)(implicit
                                          headerCarrier: HeaderCarrier,
                                          ec: ExecutionContext,
                                          request: RequestHeader): Future[Either[HttpException, MinimalDetails]] = {
    featureToggleService.get(IntegrationFrameworkMinimalDetails).flatMap {
      case Enabled(IntegrationFrameworkMinimalDetails) =>
        implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders =
          headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier)))
        val minimalDetailsUrl = appConfig.psaMinimalDetailsIFUrl.format(regime, idType, idValue)

        httpClient.GET(minimalDetailsUrl)(implicitly[HttpReads[HttpResponse]], implicitly[HeaderCarrier](hc),
          implicitly) map {
          handleResponseIF(_, minimalDetailsUrl)
        } andThen sendGetMinimalDetailsEvent(idType, idValue)(auditService.sendEvent) andThen logWarning("IF PSA minimal details")

      case _ => // Ignore idType for original API because always PSA
        implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeader(headerCarrier))

        val minimalDetailsUrl = appConfig.psaMinimalDetailsUrl.format(idValue)
        httpClient.GET(minimalDetailsUrl)(implicitly[HttpReads[HttpResponse]], implicitly[HeaderCarrier](hc),
          implicitly) map {
          handleResponseDES(_, minimalDetailsUrl)
        } andThen sendGetMinimalPSADetailsEvent(psaId = idValue)(auditService.sendEvent) andThen logWarning("PSA minimal details")
      }
  }

  private def handleResponseDES(response: HttpResponse, url: String): Either[HttpException, MinimalDetails] = {
    val badResponseSeq = Seq("INVALID_PSAID", "INVALID_CORRELATIONID")
    response.status match {
      case OK =>
        response.json.validate[MinimalDetails](MinimalDetails.minimalDetailsDESReads).fold(
          _ => {
            invalidPayloadHandler.logFailures("/resources/schemas/getPSAMinimalDetails.json")(response.json)
            Left(new BadRequestException("INVALID PAYLOAD"))
          },
          value =>
            Right(value)
        )
      case _ => Left(handleErrorResponse("PSA minimal details", url, response, badResponseSeq))
    }
  }

  private def handleResponseIF(response: HttpResponse, url: String): Either[HttpException, MinimalDetails] = {
    val badResponseSeq = Seq("INVALID_IDTYPE", "INVALID_PAYLOAD", "INVALID_CORRELATIONID", "INVALID_REGIME")
    response.status match {
      case OK =>
        response.json.validate[MinimalDetails](MinimalDetails.minimalDetailsIFReads).fold(
          _ => {
            invalidPayloadHandler.logFailures("/resources/schemas/getPSAMinimalDetails.json")(response.json)
            Left(new BadRequestException("INVALID PAYLOAD"))
          },
          value =>
            Right(value)
        )
      case _ => Left(handleErrorResponse("Minimal details", url, response, badResponseSeq))
    }
  }

  private def processFailureResponse(response: HttpResponse, url: String): Either[HttpException, Unit] = {
    Logger.warn(s"POST or $url returned ${response.status} with body ${response.body}")

    response.status match {
      case FORBIDDEN if response.body.contains("ACTIVE_RELATIONSHIP_EXISTS") => Left(new ConflictException("ACTIVE_RELATIONSHIP_EXISTS"))
      case FORBIDDEN if response.body.contains("INVALID_INVITER_PSAID") => Left(new BadRequestException("INVALID_INVITER_PSAID"))
      case FORBIDDEN if response.body.contains("INVALID_INVITEE_PSAID") => Left(new BadRequestException("INVALID_INVITEE_PSAID"))
      case BAD_REQUEST if response.body.contains("INVALID_PAYLOAD") =>
        invalidPayloadHandler.logFailures("/resources/schemas/createPsaAssociationRequest.json", url)(response.json)
        Left(new BadRequestException("INVALID PAYLOAD"))
      case _ => Left(handleErrorResponse("POST", url, response, Seq("INVALID_PSTR", "INVALID_CORRELATION_ID")))
    }

  }

  private def processResponse(acceptedInvitation: AcceptedInvitation, response: HttpResponse, url: String)(
    implicit request: RequestHeader, ec: ExecutionContext) : Either[HttpException, Unit] = {

    sendAcceptInvitationAuditEvent(acceptedInvitation, response.status,
      if (response.body.isEmpty) None else Some(response.json))(auditService.sendEvent)

    if (response.status == OK) {
      Logger.info(s"POST of $url returned successfully")
      Right(())
    } else {
      processFailureResponse(response, url)
    }
  }

  def acceptInvitation(acceptedInvitation: AcceptedInvitation)
    (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, Unit]] =
    featureToggleService.get(IntegrationFrameworkListSchemes).map(_.isEnabled).flatMap { isEnabled =>
      if (isEnabled) {

        implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders =
          headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier)))
        val url = appConfig.createPsaAssociationIFUrl.format(acceptedInvitation.pstr)
        val data = Json.toJson(acceptedInvitation)(writesIFAcceptedInvitation)
        association(url, data, acceptedInvitation)(hc, implicitly, implicitly)

      } else {

        val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.desHeader(headerCarrier))
        val url = appConfig.createPsaAssociationUrl.format(acceptedInvitation.pstr)
        val data = Json.toJson(acceptedInvitation)(writesAcceptedInvitation)
        association(url, data, acceptedInvitation)(hc, implicitly, implicitly)
      }
    }

  private def association(url: String, data: JsValue, acceptedInvitation: AcceptedInvitation)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext,
                          request: RequestHeader): Future[Either[HttpException, Unit]] = {
    Logger.debug(s"[Accept-Invitation-Outgoing-Payload] - ${data.toString()}")
    httpClient.POST[JsValue, HttpResponse](url, data)(
      implicitly, implicitly, hc, implicitly
    ) map (processResponse(acceptedInvitation, _, url))
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

  val writesIFAcceptedInvitation: Writes[AcceptedInvitation] = Writes {
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

      val declarationDuties: (String, JsValue) =
        if (invite.declarationDuties) {
          "box5" -> JsBoolean(true)
        } else {
          "box6" -> JsString("true")
        }

      val declarationDetails = Json.obj(
        "box1" -> invite.declaration,
        "box2" -> invite.declaration,
        "box3" -> invite.declaration,
        "box4" -> invite.declaration
      ) + declarationDuties ++ pensionAdviserDetails

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
