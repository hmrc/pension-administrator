/*
 * Copyright 2023 HM Revenue & Customs
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

package config

import scala.util.matching.Regex

object Constants {
  val AcceptHeaderPattern: Regex = "^application/vnd[.]{1}hmrc[.]{1}(.*?)[+]{1}(.*)$".r


  val XClientIdHeader: String = "X-Client-Id"


  val LegacyEnrolmentKey: String   = "HMRC-PSA-ORG"
  val LegacyEnrolmentIdKey: String = "psaID"

  val NewEnrolmentKey: String   = "HMRC-PODS-ORG"
  val NewEnrolmentIdKey: String = "psaID"


  val MissingECCEnrolmentMessage: String =
    "User does not have the ECC enrolment, and will be unable to submit phase 5 declarations. See https://www.gov.uk/guidance/how-to-subscribe-to-the-new-computerised-transit-system"

}
