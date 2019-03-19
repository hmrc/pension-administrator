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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed trait Address

object Address {
  implicit val reads: Reads[Address] = ((__ \ "countryCode").read[String] orElse (__ \ "country").read[String]).flatMap(countryCode =>
    getReadsBasedOnCountry[UkAddress, InternationalAddress](UkAddress.apiReads, InternationalAddress.apiReads, countryCode))

  implicit val writes: Writes[Address] = Writes {
    case address: UkAddress =>
      UkAddress.writes.writes(address)
    case address: InternationalAddress =>
      InternationalAddress.writes.writes(address)
  }

  val defaultWrites: Writes[Address] = Writes {
    case address: UkAddress =>
      UkAddress.defaultWrites.writes(address)
    case address: InternationalAddress =>
      InternationalAddress.defaultWrites.writes(address)
  }

  val updateWrites: Writes[Address] = Writes {
    case address: UkAddress =>
      UkAddress.updateWrites.writes(address)
    case address: InternationalAddress =>
      InternationalAddress.updateWrites.writes(address)
  }

  val updatePreviousAddressWrites: Writes[Address] = Writes {
    case address: UkAddress =>
      UkAddress.updatePreviousAddressWrites.writes(address)
    case address: InternationalAddress =>
      InternationalAddress.updatePreviousAddressWrites.writes(address)
  }

  val commonAddressElementsReads: Reads[(String, Option[String], Option[String], Option[String], String, Option[Boolean])] = (
    (JsPath \ "addressLine1").read[String] and
      (JsPath \ "addressLine2").readNullable[String] and
      (JsPath \ "addressLine3").readNullable[String] and
      (JsPath \ "addressLine4").readNullable[String] and
      ((JsPath \ "countryCode").read[String] orElse (JsPath \ "country").read[String]) and
      (JsPath \ "isChanged").readNullable[Boolean]
    ) ((line1, line2, line3, line4, countryCode, isChanged) => (line1, line2, line3, line4, getCountryOrTerritoryCode(countryCode), isChanged))

  val commonAddressWrites: Writes[(String, Option[String], Option[String], Option[String])] = (
    (JsPath \ "line1").write[String] and
      (JsPath \ "line2").writeNullable[String] and
      (JsPath \ "line3").writeNullable[String] and
      (JsPath \ "line4").writeNullable[String]) (elements => (elements._1, elements._2, elements._3, elements._4))

  private def getCountryOrTerritoryCode(countryCode: String) = {
    if (countryCode.contains("territory")) countryCode.split(":").last.trim() else countryCode
  }

  private def getReadsBasedOnCountry[T, B](ukAddressReads: Reads[T], nonUkAddressReads: Reads[B], countryCode: String) = {
    if (countryCode == "GB") ukAddressReads.map(c => c.asInstanceOf[Address]) else nonUkAddressReads.map(c => c.asInstanceOf[Address])
  }
}

case class UkAddress(addressLine1: String, addressLine2: Option[String] = None, addressLine3: Option[String] = None,
                     addressLine4: Option[String] = None, countryCode: String, postalCode: String,
                     isChanged: Option[Boolean] = None) extends Address

object UkAddress {
  implicit val format: Reads[UkAddress] = Json.reads[UkAddress]

  implicit val writes: Writes[UkAddress] = (
    JsPath.write(Address.commonAddressWrites) and
      (JsPath \ "countryCode").write[String] and
      (JsPath \ "postalCode").write[String] and
      (JsPath \ "addressType").write[String]
    ) (ukAddress => ((ukAddress.addressLine1, ukAddress.addressLine2, ukAddress.addressLine3, ukAddress.addressLine4),
    ukAddress.countryCode,
    ukAddress.postalCode,
    "UK"))

  val commonUpdateWrites: Writes[(String, String, Boolean)] = (
    (JsPath \ "countryCode").write[String] and
      (JsPath \ "postalCode").write[String] and
      (JsPath \ "nonUKAddress").write[Boolean]) (elements => (elements._1, elements._2, elements._3))

  implicit val updateWrites: Writes[UkAddress] = (
    JsPath.write(Address.commonAddressWrites) and
      JsPath.write(commonUpdateWrites) and
      (JsPath \ "changeFlag").writeNullable[Boolean]
    ) (ukAddress => ((ukAddress.addressLine1, ukAddress.addressLine2, ukAddress.addressLine3, ukAddress.addressLine4),
    (ukAddress.countryCode,
      ukAddress.postalCode, false), ukAddress.isChanged))

  implicit val updatePreviousAddressWrites: Writes[UkAddress] = (
    JsPath.write(Address.commonAddressWrites) and
      JsPath.write(commonUpdateWrites)
    ) (ukAddress => ((ukAddress.addressLine1, ukAddress.addressLine2, ukAddress.addressLine3, ukAddress.addressLine4),
    (ukAddress.countryCode,
      ukAddress.postalCode, false)))

  val defaultWrites: Writes[UkAddress] = Json.writes[UkAddress]

  val apiReads: Reads[UkAddress] = (
    JsPath.read(Address.commonAddressElementsReads) and
      ((JsPath \ "postalCode").read[String] orElse (JsPath \ "postcode").read[String])
    ) ((common, postalCode) => UkAddress(common._1, common._2, common._3, common._4, common._5, postalCode, common._6))
}

case class InternationalAddress(addressLine1: String, addressLine2: Option[String] = None, addressLine3: Option[String] = None,
                                addressLine4: Option[String] = None, countryCode: String,
                                postalCode: Option[String] = None, isChanged: Option[Boolean] = None) extends Address

object InternationalAddress {
  implicit val format: Format[InternationalAddress] = Json.format[InternationalAddress]

  implicit val writes: Writes[InternationalAddress] = (
    JsPath.write(Address.commonAddressWrites) and
      (JsPath \ "countryCode").write[String] and
      (JsPath \ "postalCode").writeNullable[String] and
      (JsPath \ "addressType").write[String]
    ) (ia => (
    (ia.addressLine1, ia.addressLine2, ia.addressLine3, ia.addressLine4),
    ia.countryCode, ia.postalCode, "NON-UK"))

  val commonUpdateWrites: Writes[(String, Option[String], Boolean)] = (
    (JsPath \ "countryCode").write[String] and
      (JsPath \ "postalCode").writeNullable[String] and
      (JsPath \ "nonUKAddress").write[Boolean]) (elements => (elements._1, elements._2, elements._3))

  implicit val updateWrites: Writes[InternationalAddress] = (
    JsPath.write(Address.commonAddressWrites) and
      JsPath.write(commonUpdateWrites) and
      (JsPath \ "changeFlag").writeNullable[Boolean]
    ) (ia => (
    (ia.addressLine1, ia.addressLine2, ia.addressLine3, ia.addressLine4),
    (ia.countryCode, ia.postalCode, true), ia.isChanged))

  implicit val updatePreviousAddressWrites: Writes[InternationalAddress] = (
    JsPath.write(Address.commonAddressWrites) and
      JsPath.write(commonUpdateWrites)
    ) (ia => (
    (ia.addressLine1, ia.addressLine2, ia.addressLine3, ia.addressLine4),
    (ia.countryCode, ia.postalCode, true)))


  val defaultWrites: Writes[InternationalAddress] = Json.writes[InternationalAddress]

  val apiReads: Reads[InternationalAddress] = (
    JsPath.read(Address.commonAddressElementsReads) and
      (JsPath \ "postalCode").readNullable[String] and
      (JsPath \ "postcode").readNullable[String]
    ) ((common, postCodeFormat1, postCodeFormat2) => {

    val postCode: Option[String] = (postCodeFormat1, postCodeFormat2) match {
      case (Some(postCodeFormat1), None) => Some(postCodeFormat1)
      case (None, Some(postCodeFormat2)) => Some(postCodeFormat2)
      case _ => None
    }

    InternationalAddress(common._1, common._2, common._3, common._4, common._5, postCode, common._6)
  })

}

