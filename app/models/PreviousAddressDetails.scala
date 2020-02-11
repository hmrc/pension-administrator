/*
 * Copyright 2020 HM Revenue & Customs
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

case class PreviousAddressDetails(isPreviousAddressLast12Month: Boolean,
                                  address: Option[Address] = None,
                                  isChanged: Option[Boolean] = None)

object PreviousAddressDetails {
  implicit val formats: Format[PreviousAddressDetails] = Json.format[PreviousAddressDetails]

  val psaSubmissionWrites: Writes[PreviousAddressDetails] = (
    (JsPath \ "isPreviousAddressLast12Month").write[Boolean] and
      (JsPath \ "previousAddressDetail").writeNullable[Address]
    ) (previousAddress => (previousAddress.isPreviousAddressLast12Month, previousAddress.address))

  val psaUpdateWrites: Writes[PreviousAddressDetails] = (
    (JsPath \ "isPreviousAddressLast12Month").write[Boolean] and
      (JsPath \ "previousAddressDetails").writeNullable[Address](Address.updatePreviousAddressWrites) and
      (JsPath \ "changeFlag").write[Boolean]
    ) (previousAddress => (previousAddress.isPreviousAddressLast12Month, previousAddress.address, previousAddress.isChanged.fold(false)(identity)))

  val psaUpdateWritesWithNoUpdateFlag: Writes[PreviousAddressDetails] = (
    (JsPath \ "isPreviousAddressLast12Month").write[Boolean] and
      (JsPath \ "previousAddressDetails").writeNullable[Address](Address.updateWrites)
    ) (previousAddress => (previousAddress.isPreviousAddressLast12Month, previousAddress.address))

  def apiReads(typeOfAddressDetail: String): Reads[PreviousAddressDetails] = (
    (JsPath \ s"${typeOfAddressDetail}AddressYears").read[String] and
      (JsPath \ s"${typeOfAddressDetail}PreviousAddress").readNullable[Address] and
      (JsPath \ s"${typeOfAddressDetail}PreviousAddressIsChanged").readNullable[Boolean] and
      (JsPath \ s"${typeOfAddressDetail}TradingOverAYear").readNullable[Boolean]
    ) ((addressYears, previousAddress, isChanged, tradingTime) => {
    PreviousAddressDetails(
      isPreviousAddressLast12Month = addressYears == "under_a_year" && (tradingTime getOrElse true),
      address = previousAddress,
      isChanged = isChanged
    )
  })
}
