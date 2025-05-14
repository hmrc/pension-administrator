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

package models.registrationnoid

import play.api.libs.json._

import java.time.LocalDate

case class RegistrationNoIdIndividualRequest(firstName: String, lastName: String, dateOfBirth: LocalDate, address: Address)

object RegistrationNoIdIndividualRequest {
  implicit val formats: Format[RegistrationNoIdIndividualRequest] = Json.format[RegistrationNoIdIndividualRequest]
  private val jsString: String => JsString = play.api.libs.json.JsString.apply

  def writesRegistrationNoIdIndividualRequest(acknowledgementReference: String): OWrites[RegistrationNoIdIndividualRequest] = {

    (registrant: RegistrationNoIdIndividualRequest) => {
      Json.obj(
        "regime" -> "PODA",
        "acknowledgementReference" -> acknowledgementReference,
        "isAnAgent" -> JsBoolean(false),
        "isAGroup" -> JsBoolean(false),
        "individual" -> Json.obj(
          "firstName" -> registrant.firstName,
          "lastName" -> registrant.lastName,
          "dateOfBirth" -> Json.toJson(registrant.dateOfBirth)
        ),
        "address" -> Json.obj(
          "addressLine1" -> registrant.address.addressLine1,
          "addressLine2" -> registrant.address.addressLine2,
          "addressLine3" -> registrant.address.addressLine3.map(jsString).getOrElse[JsValue](JsNull),
          "addressLine4" -> registrant.address.addressLine4.map(jsString).getOrElse[JsValue](JsNull),
          "postalCode" -> registrant.address.postcode.map(jsString).getOrElse[JsValue](JsNull),
          "countryCode" -> registrant.address.country
        ),
        "contactDetails" -> Json.obj()
      )
    }

  }
}
