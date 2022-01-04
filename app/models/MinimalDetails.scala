/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.libs.json.{Writes, Json, JsPath, Reads}

case class MinimalDetails(
                              email: String,
                              isPsaSuspended: Boolean,
                              organisationName: Option[String],
                              individualDetails: Option[IndividualDetails],
                              rlsFlag: Boolean,
                              deceasedFlag: Boolean
                            ) {

  def name: Option[String] = {
    individualDetails
      .map(_.fullName)
      .orElse(organisationName)
  }

}

object MinimalDetails {
  implicit val minimalDetailsIFReads: Reads[MinimalDetails] = (
    (JsPath \ "email").read[String] and
      (JsPath \ "psaSuspensionFlag").readNullable[Boolean] and
      (JsPath \ "minimalDetails" \ "individualDetails").readNullable[IndividualDetails](IndividualDetails.individualDetailReads) and
      (JsPath \ "minimalDetails" \ "organisationOrPartnershipName").readNullable[String] and
      (JsPath \ "rlsFlag").read[Boolean] and
      (JsPath \ "deceasedFlag").read[Boolean]
    ) ((email, isPsaSuspended, indvDetails, orgName, rlsFlag, deceasedFlag) =>
    MinimalDetails(email, isPsaSuspended.getOrElse(false), orgName, indvDetails, rlsFlag, deceasedFlag)
  )

  implicit val defaultWrites : Writes[MinimalDetails] = Json.writes[MinimalDetails]
  implicit val defaultReads :  Reads[MinimalDetails] = Json.reads[MinimalDetails]
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
