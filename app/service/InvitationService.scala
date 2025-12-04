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

package service

import audit.{AuditService, InvitationAuditEvent}
import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import connectors.*
import models.*
import models.enumeration.JourneyType
import play.api.Logger
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.*
import play.api.mvc.RequestHeader
import repositories.InvitationsCacheRepository
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.*
import utils.{DateHelper, FuzzyNameMatcher}

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

case class MongoDBFailedException(exceptionMessage: String) extends HttpException(exceptionMessage, INTERNAL_SERVER_ERROR)

@ImplementedBy(classOf[InvitationServiceImpl])
trait InvitationService {

  def invitePSA(jsValue: JsValue)
               (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, Unit]]

}

class InvitationServiceImpl @Inject()(
                                       associationConnector: AssociationConnector,
                                       emailConnector: EmailConnector,
                                       config: AppConfig,
                                       repository: InvitationsCacheRepository,
                                       auditService: AuditService,
                                       schemeConnector: SchemeConnector,
                                       crypto: JsonCryptoService
                                     ) extends InvitationService {

  private val logger = Logger(classOf[InvitationServiceImpl])

  override def invitePSA(jsValue: JsValue)
                        (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, rh: RequestHeader): Future[Either[HttpException, Unit]] = {
    jsValue.validate[Invitation](using Invitation.formats).fold(
      {
        errors =>
          logger.warn(s"Json contains bad data $errors")
          Future.failed(new BadRequestException(s"Bad invitation format sent $errors"))
      },
      {
        invitation =>
          handle(associationConnector.getMinimalDetails(invitation.inviteePsaId.id, "psaid", "poda")) { psaDetails =>
            handle(schemeConnector.checkForAssociation(Left(invitation.inviteePsaId), SchemeReferenceNumber(invitation.srn))) {
              case false if doNamesMatch(invitation.inviteeName, psaDetails) =>
                handle(insertInvitation(invitation)) { _ =>
                  auditService.sendEvent(InvitationAuditEvent(invitation))
                  sendInviteeEmail(invitation, psaDetails).map(Right(_))
                }
              case true if doNamesMatch(invitation.inviteeName, psaDetails) =>
                Future.successful(Left(new ForbiddenException("The invitation is to a PSA already associated with this scheme")))
              case _ => Future.successful(Left(new NotFoundException("The name and PSA Id do not match")))
            }
          }
      }
    )
  }

  private def doNamesMatch(inviteeName: String, psaDetails: MinimalDetails): Boolean = {

    val matches = (psaDetails.organisationName, psaDetails.individualDetails) match {
      case (Some(organisationName), _) => doOrganisationNamesMatch(inviteeName, organisationName)
      case (_, Some(individual)) => doIndividualNamesMatch(inviteeName, individual, matchFullName = true)
      case _ => throw new IllegalArgumentException("InvitationService cannot match a PSA without organisation or individual detail")
    }

    if (!matches) {
      logMismatch(inviteeName, psaDetails)
    }
    matches
  }


  private def insertInvitation(invitation: Invitation)(implicit ec: ExecutionContext): Future[Either[HttpException, Unit]] =
    repository.upsert(invitation).map(_ => Right(())) recover {
      case exception: Exception => Left(MongoDBFailedException(s"""Could not perform DB operation: ${exception.getMessage}"""))
    }

  private def doOrganisationNamesMatch(inviteeName: String, organisationName: String): Boolean =
    FuzzyNameMatcher.matches(inviteeName, organisationName)

  @tailrec
  private def doIndividualNamesMatch(inviteeName: String, individualDetails: IndividualDetails, matchFullName: Boolean): Boolean = {

    val psaName = whichIndividualName(individualDetails, matchFullName)

    if (FuzzyNameMatcher.matches(inviteeName, psaName) ||
      (inviteeName.contains(individualDetails.firstName) && inviteeName.contains(individualDetails.lastName))) {
      true
    } else if (matchFullName) {
      doIndividualNamesMatch(inviteeName, individualDetails, matchFullName = false)
    } else {
      false
    }

  }

  private def whichIndividualName(individualDetails: IndividualDetails, matchFullName: Boolean): String = {

    if (matchFullName) {
      individualDetails.fullName
    } else {
      individualDetails.name
    }

  }

  private def logMismatch(inviteeName: String, psaDetails: MinimalDetails): Unit = {

    val psaName =
      depersonalise(
        psaDetails
          .individualDetails
          .map(_.fullName)
          .getOrElse(
            psaDetails
              .organisationName
              .getOrElse("")
          )
      )

    val entityType =
      psaDetails
        .individualDetails
        .map(_ => "Individual")
        .getOrElse(
          psaDetails
            .organisationName
            .map(_ => "Organisation")
            .getOrElse("Unknown")
        )

    logger.warn(
      s"Cannot match invitee and PSA names. Logging depersonalised names.\n" +
        s"Entity Type: $entityType\n" +
        s"Invitee: ${depersonalise(inviteeName)}\n" +
        s"PSA: $psaName"
    )

  }

  private def depersonalise(value: String): String = {
    value
      .replaceAll("[a-zA-Z]", "x")
      .replaceAll("[0-9]", "9")
  }

  private def sendInviteeEmail(invitation: Invitation, psaDetails: MinimalDetails)
                              (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    val name = psaDetails.individualDetails.map(_.fullName).getOrElse(psaDetails.organisationName.getOrElse(""))
    val expiryDate = DateHelper.formatDate(invitation.expireAt)
    val inviteePsaId = invitation.inviteePsaId

    val email = SendEmailRequest(
      List(psaDetails.email),
      "pods_psa_invited",
      Map(
        "inviteeName" -> name,
        "schemeName" -> invitation.schemeName,
        "expiryDate" -> expiryDate
      ),
      force = false,
      Some(callBackUrl(inviteePsaId))
    )

    emailConnector.sendEmail(email).map {
      case EmailSent => ()
      case _ =>
        logger.error("Unable to send email to invited PSA. Support intervention possibly required.")
        ()
    }

  }

  private def callBackUrl(psaId: PsaId): String = {
    val encryptedPsaId = crypto.jsonCrypto.encrypt(PlainText(psaId.value)).value
    config.invitationCallbackUrl.format(JourneyType.INVITE.toString, encryptedPsaId)
  }

  private def handle[T](request: Future[Either[HttpException, T]])
                       (f: T => Future[Either[HttpException, Unit]])(implicit ec: ExecutionContext): Future[Either[HttpException, Unit]] = {
    request flatMap {
      case Right(n) =>
        f(n)
      case Left(error) =>
        Future.successful(Left(error))
    }
  }

}
