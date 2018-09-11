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

import org.joda.time.DateTime
import play.api.libs.json.{JsPath, Json, Reads}
import play.api.libs.functional.syntax._

case class PSAMinimalDetails(
                              processingDate: DateTime,
                              email: String,
                              psaSuspensionFlag: Boolean,
                              psaMinimalDetails: PSAMinimalDetailsObject
                            )

object PSAMinimalDetails {

  val customReads : Reads[PSAMinimalDetails] = (
    (JsPath \ "processingDate").read[String] map DateTime.parse and
      (JsPath \ "_").readNullable[String]
    )((date, _) =>
    PSAMinimalDetails(date, "", true, PSAMinimalDetailsObject(None, None))
  )
}

case class PSAMinimalDetailsObject(
                                    organisationDetails: Option[PSAOrganisationDetails],
                                    individualDetails: Option[PSAIndividualDetails]
                                  )

case class PSAOrganisationDetails(
                                   organisationOrPartnershipName: String
                                 )

object PSAOrganisationDetails {
  implicit val formats = Json.format[PSAOrganisationDetails]
}

case class PSAIndividualDetails(
                                 firstName: String,
                                 middleName: Option[String],
                                 lastName: String
                               )

object PSAIndividualDetails {
  implicit val formats = Json.format[PSAIndividualDetails]
}
