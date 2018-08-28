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

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ContactDetailsType(phoneNumber: Option[String],
                              emailAddress: Option[String])

object ContactDetailsType {

  implicit val format: Format[ContactDetailsType] = Json.format[ContactDetailsType]
}

case class OrganisationName(organisationName: String)

object OrganisationName {

  implicit val format: Format[OrganisationName] = Json.format[OrganisationName]
}

case class OrganisationRegistrant(
                                   acknowledgementReference: String,
                                   organisation: OrganisationName,
                                   address: Address,
                                   contactDetails: ContactDetailsType
                                 )

object OrganisationRegistrant {

  implicit val format: Format[OrganisationRegistrant] = Json.format[OrganisationRegistrant]

  val apiWrites: Writes[OrganisationRegistrant] = {
    (
      (__ \ "regime").write[String] and
        (__ \ "acknowledgementReference").write[String] and
        (__ \ "isAnAgent").write[Boolean] and
        (__ \ "isAGroup").write[Boolean] and
        (__ \ "organisation").write[OrganisationName] and
        (__ \ "address").write[Address] and
        (__ \ "contactDetails").write[ContactDetailsType]
      ) { o =>
      (
        "PODA",
        o.acknowledgementReference,
        false,
        false,
        o.organisation,
        o.address,
        o.contactDetails
      )
    }
  }
}






