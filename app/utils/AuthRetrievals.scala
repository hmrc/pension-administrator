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

package utils

import javax.inject.Inject
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AuthRetrievals @Inject()(
                              val authConnector: AuthConnector
                              ) extends AuthorisedFunctions{

  def getPsaId(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PsaId]] = {
    authorised().retrieve(Retrievals.allEnrolments) { enrolments =>

      val psaId = enrolments.getEnrolment("HMRC-PODS-ORG")
        .flatMap(_.getIdentifier("PSAID"))
        .map(psaId => PsaId(psaId.value))

      Future.successful(psaId)
    }
  }
}
