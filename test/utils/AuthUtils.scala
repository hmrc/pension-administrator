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

package utils

import com.google.inject.Inject
import models.SchemeReferenceNumber
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

object AuthUtils {
  val id = "id"
  val psaId = "A2123456"
  val pspId = "21000005"
  val srn: SchemeReferenceNumber = SchemeReferenceNumber("S2400000006")
  val externalId = "externalId"

  def failedAuthStub(mockAuthConnector: AuthConnector): OngoingStubbing[Future[Unit]] =
    when(mockAuthConnector.authorise[Unit](any(), any())(using any(), any())).thenReturn(Future.failed(InsufficientEnrolments()))

  def authStub(mockAuthConnector: AuthConnector): OngoingStubbing[Future[Enrolments ~ Option[String]]] =
    when(mockAuthConnector.authorise[Enrolments ~ Option[String]](any(), any())(using any(), any())).thenReturn(Future.successful(AuthUtils.authResponse))
  val authResponse = new ~(
    Enrolments(
      Set(
        new Enrolment("HMRC-PODS-ORG", Seq(EnrolmentIdentifier("PsaId", psaId)), "Activated")
      )
    ), Some(id)
  )

  def authStubPsp(mockAuthConnector: AuthConnector): OngoingStubbing[Future[Enrolments ~ Option[String]]] =
    when(mockAuthConnector.authorise[Enrolments ~ Option[String]](any(), any())(using any(), any())).thenReturn(Future.successful(AuthUtils.authResponsePsp))
  val authResponsePsp = new ~(
    Enrolments(
      Set(
        new Enrolment("HMRC-PODSPP-ORG", Seq(EnrolmentIdentifier("PspId", pspId)), "Activated")
      )
    ), Some(id)
  )

  def noEnrolmentAuthStub(mockAuthConnector: AuthConnector): OngoingStubbing[Future[Option[String]]] =
    when(mockAuthConnector.authorise[Option[String]](any(), any())(using any(), any())).thenReturn(Future.successful(AuthUtils.noEnrolmentAuthResponse))

  val noEnrolmentAuthResponse: Option[String] = Some(externalId)

  class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
    val serviceUrl: String = ""

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.failed(exceptionToReturn)
  }
}
