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

import akka.io.Tcp.Message
import com.google.inject.{ImplementedBy, Inject}
import connectors.AssociationConnector
import models.{IndividualDetails, Invitation, PSAMinimalDetails}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.RequestHeader
import repositories.InvitationsCacheRepository
import uk.gov.hmrc.http._
import play.api.http.Status.INTERNAL_SERVER_ERROR
import utils.FuzzyNameMatcher

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

case class MongoDBFailedException(exceptionMesage: String) extends HttpException(exceptionMesage, INTERNAL_SERVER_ERROR)

@ImplementedBy(classOf[InvitationServiceImpl])
trait InvitationService {

  def invitePSA(jsValue: JsValue)
                 (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, Boolean]]

}

class InvitationServiceImpl @Inject()(associationConnector: AssociationConnector, repository: InvitationsCacheRepository) extends InvitationService {

  override def invitePSA(jsValue: JsValue)
                          (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, rh: RequestHeader): Future[Either[HttpException, Boolean]] = {
    jsValue.validate[Invitation].fold(
      {
        errors =>
          Logger.warn(s"Json contains bad data $errors")
          Future.failed(new BadRequestException(s"Bad invitation format sent $errors"))
      },
      {
        invitation =>
          associationConnector.getPSAMinimalDetails(invitation.inviteePsaId).flatMap {
            case Right(psaDetails) => doNamesMatch(invitation, psaDetails)
            case Left(ex) => Future.successful(Left(ex))
          }
      }
    )
  }

  private def doNamesMatch(invitation: Invitation, psaDetails: PSAMinimalDetails): Future[Either[HttpException, Boolean]] = {

    val matches = (psaDetails.organisationName, psaDetails.individualDetails) match {
      case (Some(organisationName), _) => doOrganisationNamesMatch(invitation.inviteeName, organisationName)
      case (_, Some(individual)) => doIndividualNamesMatch(invitation.inviteeName, individual, true)
      case _ => throw new IllegalArgumentException("InvitationService cannot match a PSA without organisation or individual detail")
    }

    if (matches) {
      repository.insert(invitation.inviteePsaId, invitation.pstr, Json.toJson(psaDetails)).map(Right(_)) recover {
        case exception: Exception => Left(new MongoDBFailedException(s"""Could not perform DB operation: ${exception.getMessage}"""))
      }
    } else {
      logMismatch(invitation.inviteeName, psaDetails)
      Future.successful(Left(new NotFoundException("NOT_FOUND")))
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

}
