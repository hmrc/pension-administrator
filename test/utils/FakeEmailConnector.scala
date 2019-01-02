/*
 * Copyright 2019 HM Revenue & Customs
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

import connectors.{EmailConnector, EmailSent, EmailStatus}
import models.SendEmailRequest
import org.scalatest.matchers.{MatchResult, Matcher}
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class FakeEmailConnector extends EmailConnector {

  private val sent: mutable.MutableList[SendEmailRequest] = mutable.MutableList[SendEmailRequest]()

  override def sendEmail(email: SendEmailRequest)
    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EmailStatus] = {

    sent += email
    Future.successful(EmailSent)

  }

  def sentEmails: List[SendEmailRequest] = sent.toList

}

object FakeEmailConnector {

  def containEmail(email: SendEmailRequest): Matcher[List[SendEmailRequest]] = EmailListMatcher(email)

  case class EmailListMatcher(email: SendEmailRequest) extends Matcher[List[SendEmailRequest]] {

    override def apply(emails: List[SendEmailRequest]): MatchResult = {
      MatchResult(
        matches = emails.contains(email),
        rawFailureMessage = s"List of ${emails.length} emails did not contain $email",
        rawNegatedFailureMessage = ""
      )
    }

  }

}
