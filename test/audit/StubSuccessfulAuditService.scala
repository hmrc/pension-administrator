/*
 * Copyright 2021 HM Revenue & Customs
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

package audit

import play.api.mvc.RequestHeader

import scala.collection.mutable
import scala.concurrent.ExecutionContext

class StubSuccessfulAuditService extends AuditService {

  private val events: mutable.ListBuffer[AuditEvent] = mutable.ListBuffer()

  override def sendEvent[T <: AuditEvent](event: T)
                                         (implicit rh: RequestHeader, ec: ExecutionContext): Unit = {
    events += event
  }

  def verifySent[T <: AuditEvent](event: T): Boolean = events.contains(event)

  def verifyNothingSent(): Boolean = events.isEmpty

  def reset(): Unit = {
    events.clear()
  }

  def lastEvent: Option[AuditEvent] = {
    events.lastOption
  }

}
