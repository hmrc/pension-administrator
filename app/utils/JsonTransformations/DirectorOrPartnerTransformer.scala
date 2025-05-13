/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.libs.json.Reads.JsObjectReducer
import play.api.libs.json._

import scala.language.postfixOps

class DirectorOrPartnerTransformer @Inject()(addressTransformer: AddressTransformer) extends JsonTransformer {

  private def getDirectorOrPartnerContactDetails(directorOrPartner: String): Reads[JsObject] = {
    (__ \ s"${directorOrPartner}ContactDetails" \ Symbol("phone")).json
      .copyFrom((__ \ Symbol("correspondenceCommonDetails") \ Symbol("contactDetails") \ Symbol("telephone")).json.pick) and
      (__ \ s"${directorOrPartner}ContactDetails" \ Symbol("email")).json
        .copyFrom((__ \ Symbol("correspondenceCommonDetails") \ Symbol("contactDetails") \ Symbol("email")).json.pick) reduce
  }

  private def getNino: Reads[JsObject] =
    (__ \ "nino").read[String].flatMap { _ =>
      (__ \ Symbol("hasNino")).json.put(JsBoolean(true)) and
        (__ \ "nino" \ Symbol("value")).json.copyFrom((__ \ Symbol("nino")).json.pick) reduce
    } orElse {
      (__ \ Symbol("hasNino")).json.put(JsBoolean(false)) and
        (__ \ Symbol("noNinoReason")).json.copyFrom((__ \ Symbol("noNinoReason")).json.pick) reduce
    } orElse {
      doNothing
    }

  private def getUtr: Reads[JsObject] =
    (__ \ "utr").read[String].flatMap { _ =>
      (__ \ Symbol("hasUtr")).json.put(JsBoolean(true)) and
        (__ \ "utr" \ Symbol("value")).json.copyFrom((__ \ Symbol("utr")).json.pick) reduce
    } orElse {
      (__ \ Symbol("hasUtr")).json.put(JsBoolean(false)) and
        (__ \ Symbol("noUtrReason")).json.copyFrom((__ \ Symbol("noUtrReason")).json.pick) reduce
    } orElse {
      doNothing
    }

  def getDirectorOrPartner(directorOrPartner: String): Reads[JsObject] = (__ \ s"${directorOrPartner}Details" \ Symbol("firstName")).json
    .copyFrom((__ \ Symbol("firstName")).json.pick) and
    ((__ \ s"${directorOrPartner}Details" \ Symbol("middleName")).json.copyFrom((__ \ Symbol("middleName")).json.pick) orElse doNothing) and
    (__ \ s"${directorOrPartner}Details" \ Symbol("lastName")).json.copyFrom((__ \ Symbol("lastName")).json.pick) and
    (__ \ Symbol("dateOfBirth")).json.copyFrom((__ \ Symbol("dateOfBirth")).json.pick) and
    getNino and
    getUtr and
    addressTransformer.getDifferentAddress(__ \ s"${directorOrPartner}Address", __ \ "correspondenceCommonDetails" \ "addressDetails") and
    getDirectorOrPartnerContactDetails(directorOrPartner) and
    addressTransformer.getAddressYears(addressYearsPath = __ \ s"${directorOrPartner}AddressYears") and
    addressTransformer.getPreviousAddress(__ \ s"${directorOrPartner}PreviousAddress") reduce

  def getDirectorsOrPartners(directorOrPartner: String): Reads[JsArray] = __.read(Reads.seq(getDirectorOrPartner(directorOrPartner))).map(JsArray(_))

  val getDirectorsOrPartners: Reads[JsObject] = {
    (__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "legalStatus").read[String].flatMap {
      case "Limited Company" => (__ \ Symbol("directors")).json.copyFrom((__ \ Symbol("psaSubscriptionDetails") \ Symbol("directorOrPartnerDetails"))
        .read(getDirectorsOrPartners("director"))) and
        ((__ \ Symbol("moreThanTenDirectors")).json
          .copyFrom((__ \ Symbol("psaSubscriptionDetails") \ Symbol("numberOfDirectorsOrPartnersDetails") \ Symbol("isMorethanTenDirectors")).json.pick) orElse doNothing) reduce
      case "Partnership" => (__ \ Symbol("partners")).json.copyFrom((__ \ Symbol("psaSubscriptionDetails") \ Symbol("directorOrPartnerDetails"))
        .read(getDirectorsOrPartners("partner"))) and
        ((__ \ Symbol("moreThanTenPartners")).json
          .copyFrom((__ \ Symbol("psaSubscriptionDetails") \ Symbol("numberOfDirectorsOrPartnersDetails") \ Symbol("isMorethanTenPartners")).json.pick) orElse doNothing) reduce
      case _ => doNothing
    }
  }
}
