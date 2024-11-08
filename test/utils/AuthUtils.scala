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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, EnrolmentIdentifier, Enrolments}

import scala.concurrent.Future

object AuthUtils {
  val id = "id"
  val psaId = "psaId"

  def failedAuthStub(mockAuthConnector: AuthConnector): OngoingStubbing[Future[Unit]] = when(mockAuthConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed(new Exception())
  def authStub(mockAuthConnector: AuthConnector): OngoingStubbing[Future[Enrolments ~ Option[String]]] =
    when(mockAuthConnector.authorise[Enrolments ~ Option[String]](any(), any())(any(), any())) thenReturn Future.successful(AuthUtils.authResponse)
  val authResponse = new ~(
    Enrolments(
      Set(
        new Enrolment("HMRC-PODS-ORG", Seq(EnrolmentIdentifier("PsaId", psaId)), "Activated")
      )
    ), Some(id)
  )
}
