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

package models.registrationnoid

import connectors.RegistrationConnectorImpl
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class OrganisationName(organisationName: String)

object OrganisationName {

  implicit val format: Format[OrganisationName] = Json.format[OrganisationName]
}

case class OrganisationRegistrant(
                                   organisation: OrganisationName,
                                   address: Address
                                 )

object OrganisationRegistrant {
  implicit val format: Format[OrganisationRegistrant] = Json.format[OrganisationRegistrant]

  private val writesAddress: OWrites[Address] = (
    (JsPath \ "addressLine1").write[String] and
      (JsPath \ "addressLine2").write[String] and
      (JsPath \ "addressLine3").writeNullable[String] and
      (JsPath \ "addressLine4").writeNullable[String] and
      (JsPath \ "postalCode").writeNullable[String] and
      (JsPath \ "countryCode").write[String]
    ) (address => (address.addressLine1, address.addressLine2, address.addressLine3, address.addressLine4, address.postcode, address.country))

  private val writesOrganisationRegistrant: Writes[OrganisationRegistrant] = {
    (
      (__ \ "organisation").write[OrganisationName] and
        (__ \ "address").write[Address](writesAddress)
      ) { o =>
      (
        o.organisation,
        o.address
      )
    }
  }

  def writesOrganisationRegistrantRequest(acknowledgementReference: String): OWrites[OrganisationRegistrant] = {

    new OWrites[OrganisationRegistrant] {

      override def writes(registrant: OrganisationRegistrant): JsObject = {
        Json.obj("regime" -> "PODA",
          "acknowledgementReference" -> acknowledgementReference,
          "isAnAgent" -> false,
          "isAGroup" -> false,
          "contactDetails" -> Json.obj(
            "phoneNumber" -> JsNull,
            "mobileNumber" -> JsNull,
            "faxNumber" -> JsNull,
            "emailAddress" -> JsNull
          )
        ) ++ Json.toJson(registrant)(writesOrganisationRegistrant).as[JsObject]
      }
    }
  }
}
