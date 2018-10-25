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

package models

import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class RegistrationNoIdIndividualRequest(firstName: String, lastName: String, dateOfBirth: LocalDate, address: InternationalAddress)

object RegistrationNoIdIndividualRequest {

  implicit val formats: Format[RegistrationNoIdIndividualRequest] = Json.format[RegistrationNoIdIndividualRequest]

  def apiWrites(acknowledgementReference: String): OWrites[RegistrationNoIdIndividualRequest] = {

    new OWrites[RegistrationNoIdIndividualRequest] {

      override def writes(registrant: RegistrationNoIdIndividualRequest): JsObject = {
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
            "addressLine2" -> registrant.address.addressLine2.getOrElse[String](" "),
            "addressLine3" -> registrant.address.addressLine3.map(JsString).getOrElse[JsValue](JsNull),
            "addressLine4" -> registrant.address.addressLine4.map(JsString).getOrElse[JsValue](JsNull),
            "postalCode" -> registrant.address.postalCode.map(JsString).getOrElse[JsValue](JsNull),
            "countryCode" -> registrant.address.countryCode
          ),
          "contactDetails" -> Json.obj()
        )
      }

    }

  }

}

case class RegistrationNoIdIndividualResponse(sapNumber: String, safeId: String)

object RegistrationNoIdIndividualResponse {

  implicit val formats: Format[RegistrationNoIdIndividualResponse] = Json.format[RegistrationNoIdIndividualResponse]

  val apiReads: Reads[RegistrationNoIdIndividualResponse] = (
    (__ \ "sapNumber").read[String] and
    (__ \ "safeId").read[String]
  )((sapNumber, safeId) => RegistrationNoIdIndividualResponse(sapNumber, safeId))

}
