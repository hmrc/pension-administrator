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

package models.registrationnoid

import org.joda.time.LocalDate
import play.api.libs.json._

case class Address(
  addressLine1: String,
  addressLine2: String,
  addressLine3: Option[String],
  addressLine4: Option[String],
  postcode: Option[String],
  country: String
)

object Address {
  implicit val formats: Format[Address] = Json.format[Address]
}

case class RegistrationNoIdIndividualRequest(firstName: String, lastName: String, dateOfBirth: LocalDate, address: Address)

object RegistrationNoIdIndividualRequest {
  implicit val formats: Format[RegistrationNoIdIndividualRequest] = Json.format[RegistrationNoIdIndividualRequest]
}

case class RegistrationNoIdIndividualResponse(sapNumber: String, safeId: String)

object RegistrationNoIdIndividualResponse {
  implicit val formats: Format[RegistrationNoIdIndividualResponse] = Json.format[RegistrationNoIdIndividualResponse]
}