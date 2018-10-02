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

package service

import audit.{AuditService, InvitationAuditEvent}
import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import connectors._
import org.joda.time.LocalDate
import play.api.Logger
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json._
import play.api.mvc.RequestHeader
import repositories.InvitationsCacheRepository
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException, NotFoundException}
import utils.{DateHelper, FuzzyNameMatcher}

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import config.AppConfig
import models._
import org.joda.time.LocalDate
import uk.gov.hmrc.domain.PsaId

case class MongoDBFailedException(exceptionMesage: String) extends HttpException(exceptionMesage, INTERNAL_SERVER_ERROR)

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
                                       schemeConnector: SchemeConnector
                                     ) extends InvitationService {

  override def invitePSA(jsValue: JsValue)
                        (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, rh: RequestHeader): Future[Either[HttpException, Unit]] = {
    jsValue.validate[Invitation].fold(
      {
        errors =>
          Logger.warn(s"Json contains bad data $errors")
          Future.failed(new BadRequestException(s"Bad invitation format sent $errors"))
      },
      {
        invitation => {


          associationConnector.getPSAMinimalDetails(invitation.inviteePsaId).flatMap {
            case Right(psaDetails) =>

              isAssociated(invitation.inviteePsaId, ???) flatMap {
                case true => ???
                case false =>


                  if (doNamesMatch(invitation.inviteeName, psaDetails)) {


                    insertInvitation(invitation).flatMap {
                      case Right(_) =>
                        auditService.sendEvent(InvitationAuditEvent(invitation))
                        sendInviteeEmail(invitation, psaDetails, config).map(Right(_))
                      case Left(error) =>
                        Future.successful(Left(error))
                    }

                  }
                  else {
                    Future.successful(Left(new NotFoundException("NOT_FOUND")))
                  }


              }
            case Left(ex) => Future.successful(Left(ex))
          }


        }
      }
    )
  }

  private def isAssociated(psaId: PsaId, srn: SchemeReferenceNumber): Future[Boolean] = ???

  private def doNamesMatch(inviteeName: String, psaDetails: PSAMinimalDetails): Boolean = {

    val matches = (psaDetails.organisationName, psaDetails.individualDetails) match {
      case (Some(organisationName), _) => doOrganisationNamesMatch(inviteeName, organisationName)
      case (_, Some(individual)) => doIndividualNamesMatch(inviteeName, individual, true)
      case _ => throw new IllegalArgumentException("InvitationService cannot match a PSA without organisation or individual detail")
    }

    if (!matches) {
      logMismatch(inviteeName, psaDetails)
    }
    matches
  }

  private def insertInvitation(invitation: Invitation): Future[Either[HttpException, Unit]] = {
    repository.insert(invitation).map(_ => Right(())) recover {
      case exception: Exception => Left(new MongoDBFailedException(s"""Could not perform DB operation: ${exception.getMessage}"""))
    }
  }

  private def doOrganisationNamesMatch(inviteeName: String, organisationName: String): Boolean = {
    FuzzyNameMatcher.matches(inviteeName, organisationName)
  }

  @tailrec
  private def doIndividualNamesMatch(inviteeName: String, individualDetails: IndividualDetails, matchFullName: Boolean): Boolean = {

    val psaName = whichIndividualName(individualDetails, matchFullName)

    if (FuzzyNameMatcher.matches(inviteeName, psaName)) {
      true
    } else if (matchFullName) {
      doIndividualNamesMatch(inviteeName, individualDetails, false)
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

  private def logMismatch(inviteeName: String, psaDetails: PSAMinimalDetails): Unit = {

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

    Logger.warn(
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

  private def sendInviteeEmail(invitation: Invitation, psaDetails: PSAMinimalDetails, config: AppConfig)
                              (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    val name = psaDetails.individualDetails.map(_.fullName).getOrElse(psaDetails.organisationName.getOrElse(""))
    val expiryDate = DateHelper.formatDate(LocalDate.now().plusDays(config.invitationExpiryDays))

    val email = SendEmailRequest(
      List(psaDetails.email),
      "pods_psa_invited",
      Map(
        "inviteeName" -> name,
        "schemeName" -> invitation.schemeName,
        "expiryDate" -> expiryDate
      )
    )

    emailConnector.sendEmail(email).map {
      case EmailSent => ()
      case _ =>
        Logger.error("Unable to send email to invited PSA. Support intervention possibly required.")
        ()
    }

  }

}
