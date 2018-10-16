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
import play.api.libs.json.{JsPath, Json, Reads, Writes}

case class PSAMinimalDetails(
                              email: String,
                              isPsaSuspended: Boolean,
                              organisationName: Option[String],
                              individualDetails: Option[IndividualDetails]
                            ) {

  def name: Option[String] = {
    individualDetails
      .map(_.fullName)
      .orElse(organisationName)
  }

}

object PSAMinimalDetails {
  implicit val psaMinimalDetailsReads: Reads[PSAMinimalDetails] = (
    (JsPath \ "email").read[String] and
      (JsPath \ "psaSuspensionFlag").read[Boolean] and
      (JsPath \ "psaMinimalDetails" \ "individualDetails").readNullable[IndividualDetails](IndividualDetails.individualDetailReads) and
      (JsPath \ "psaMinimalDetails" \ "organisationOrPartnershipName").readNullable[String]
    ) ((email, isPsaSuspended, indvDetails, orgName) =>
    PSAMinimalDetails(email, isPsaSuspended, orgName, indvDetails)
  )

  implicit val defaultWrites : Writes[PSAMinimalDetails] = Json.writes[PSAMinimalDetails]
}

case class IndividualDetails(
                              firstName: String,
                              middleName: Option[String],
                              lastName: String
                            ) {

  def fullName: String = middleName match {
    case Some(middle) => s"$firstName $middle $lastName"
    case _ => name
  }

  def name: String = s"$firstName $lastName"

}

object IndividualDetails {
  implicit val individualDetailReads: Reads[IndividualDetails] = (
    (JsPath \ "firstName").read[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "lastName").read[String]
    ) ((firstName, middleName, lastName) =>
    IndividualDetails(firstName, middleName, lastName)
  )

  implicit val defaultWrites : Writes[IndividualDetails] = Json.writes[IndividualDetails]
}
