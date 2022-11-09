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

package utils.JsonTransformations

import com.google.inject.Inject
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.language.postfixOps

class AddressTransformer @Inject()(legalStatusTransformer: LegalStatusTransformer) extends JsonTransformer {

  private def getCommonAddressElements(userAnswersPath: JsPath, desAddressPath: JsPath): Reads[JsObject] = {
    (userAnswersPath \ Symbol("addressLine1")).json.copyFrom((desAddressPath \ Symbol("line1")).json.pick) and
      (userAnswersPath \ Symbol("addressLine2")).json.copyFrom((desAddressPath \ Symbol("line2")).json.pick) and
      ((userAnswersPath \ Symbol("addressLine3")).json.copyFrom((desAddressPath \ Symbol("line3")).json.pick)
        orElse doNothing) and
      ((userAnswersPath \ Symbol("addressLine4")).json.copyFrom((desAddressPath \ Symbol("line4")).json.pick)
        orElse doNothing) reduce
  }

  def getAddress(userAnswersPath: JsPath, desAddressPath: JsPath): Reads[JsObject] = {
    getCommonAddressElements(userAnswersPath, desAddressPath) and
      ((userAnswersPath \ Symbol("postalCode")).json.copyFrom((desAddressPath \ Symbol("postalCode")).json.pick)
        orElse doNothing) and
      (userAnswersPath \ Symbol("countryCode")).json.copyFrom((desAddressPath \ Symbol("countryCode")).json.pick) reduce
  }

  def getDifferentAddress(userAnswersPath: JsPath, desAddressPath: JsPath): Reads[JsObject] = {
    getCommonAddressElements(userAnswersPath, desAddressPath) and
      ((userAnswersPath \ Symbol("postcode")).json.copyFrom((desAddressPath \ Symbol("postalCode")).json.pick)
        orElse doNothing) and
      (userAnswersPath \ Symbol("country")).json.copyFrom((desAddressPath \ Symbol("countryCode")).json.pick) reduce
  }

  def getAddressYears(path: JsPath = __, addressYearsPath: JsPath = __): Reads[JsObject] = {
    (path \ "previousAddressDetails" \ "isPreviousAddressLast12Month").read[Boolean].flatMap { addressYearsValue =>
      val value = if (addressYearsValue) {
        JsString("under_a_year")
      } else {
        JsString("over_a_year")
      }
      addressYearsPath.json.put(value)
    }
  }

  def getPreviousAddress(path: JsPath): Reads[JsObject] = {
    (__ \ Symbol("previousAddressDetails") \ Symbol("previousAddress")).read[JsObject].flatMap { _ =>
      getDifferentAddress(path, __ \ Symbol("previousAddressDetails") \ Symbol("previousAddress"))
    } orElse doNothing
  }

  val getAddressYearsBasedOnLegalStatus: Reads[JsObject] = {
    legalStatusTransformer
      .returnPathBasedOnLegalStatus(__ \ Symbol("individualAddressYears"), __ \ Symbol("companyAddressYears"), __ \ Symbol("partnershipAddressYears"))
      .flatMap { addressYearsPath =>
        getAddressYears(__ \ "psaSubscriptionDetails", addressYearsPath)
      }
  }

  val getPreviousAddressBasedOnLegalStatus: Reads[JsObject] = {
    legalStatusTransformer
      .returnPathBasedOnLegalStatus(__ \ Symbol("individualPreviousAddress"), __ \ Symbol("companyPreviousAddress"), __ \ Symbol("partnershipPreviousAddress"))
      .flatMap {
        getDifferentAddress(_, __ \ Symbol("psaSubscriptionDetails") \ Symbol("previousAddressDetails") \ Symbol("previousAddress"))
      } orElse doNothing
  }

  val getCorrespondenceAddress: Reads[JsObject] = {
    val inputAddressPath = __ \ Symbol("psaSubscriptionDetails") \ Symbol("correspondenceAddressDetails")

    (__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "legalStatus").read[String].flatMap {
      case "Individual" => getDifferentAddress(__ \ Symbol("individualContactAddress"), inputAddressPath)
      case "Limited Company" => getDifferentAddress(__ \ Symbol("companyContactAddress"), inputAddressPath)
      case "Partnership" => getDifferentAddress(__ \ Symbol("partnershipContactAddress"), inputAddressPath)
    }
  }
}
