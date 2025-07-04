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

import uk.gov.hmrc.http.HeaderCarrier

import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class FakeEnrolmentLoggingService extends EnrolmentLoggingService {
  override def logEnrolments(clientId: Option[String])(implicit
                                                       @unused hc: HeaderCarrier,
                                                       @unused ec: ExecutionContext): Future[Unit] = Future.successful(())
}
