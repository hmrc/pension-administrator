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
import play.api.libs.json.{JsPath, Reads}

case class PSAMinimalDetails(
                              email: String,
                              isPsaSuspended: Boolean,
                              organisationName: Option[String],
                              individualDetails: Option[IndividualDetails]
                            )

object PSAMinimalDetails {
  implicit val psaMinimalDetailsReads: Reads[PSAMinimalDetails] = (
    (JsPath \ "email").read[String] and
      (JsPath \ "psaSuspensionFlag").read[Boolean] and
      (JsPath \ "psaMinimalDetails" \ "individualDetails").readNullable[IndividualDetails](IndividualDetails.individualDetailReads) and
      (JsPath \ "psaMinimalDetails" \ "organisationOrPartnershipName").readNullable[String]
    ) ((email, isPsaSuspended, indvDetails, orgName) =>
    PSAMinimalDetails(email, isPsaSuspended, orgName, indvDetails)
  )
}

case class IndividualDetails(
                              firstName: String,
                              middleName: Option[String],
                              lastName: String
                            )

object IndividualDetails {
  val individualDetailReads: Reads[IndividualDetails] = (
    (JsPath \ "firstName").read[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "lastName").read[String]
    ) ((firstName, middleName, lastName) =>
    IndividualDetails(firstName, middleName, lastName)
  )
}
