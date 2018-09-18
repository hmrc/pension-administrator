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

import com.google.inject.{ImplementedBy, Inject}
import connectors.AssociationConnector
import models.{IndividualDetails, Invitation, PSAMinimalDetails}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException, NotFoundException}
import utils.FuzzyNameMatcher

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[InvitationServiceImpl])
trait InvitationService {

  def invitePSA(jsValue: JsValue)
                 (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, Unit]]

}

class InvitationServiceImpl @Inject()(associationConnector: AssociationConnector) extends InvitationService {

  override def invitePSA(jsValue: JsValue)
                          (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, rh: RequestHeader): Future[Either[HttpException, Unit]] = {
    jsValue.validate[Invitation].fold(
      {
        errors =>
          Logger.warn(s"Json contains bad data $errors")
          Future.failed(new BadRequestException(s"Bad invitation format sent $errors"))
      },
      {
        invitation =>
          associationConnector.getPSAMinimalDetails(invitation.inviteePsaId) map {
            case Right(psaDetails) => doNamesMatch(invitation.inviteeName, psaDetails)
            case Left(ex) => Left(ex)
          }
      }
    )
  }

  private def doNamesMatch(inviteeName: String, psaDetails: PSAMinimalDetails): Either[HttpException, Unit] = {

    val matches = (psaDetails.organisationName, psaDetails.individualDetails) match {
      case (Some(organisationName), _) => doOrganisationNamesMatch(inviteeName, organisationName)
      case (_, Some(individual)) => doIndividualNamesMatch(inviteeName, individual, true)
      case _ => throw new IllegalArgumentException("InvitationService cannot match a PSA without organisation or individual detail")
    }

    if (matches) {
      Right(())
    } else {
      logMismatch(inviteeName, psaDetails)
      Left(new NotFoundException("NOT_FOUND"))
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
