/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.libs.json.{__, _}

class DirectorOrPartnerTransformer @Inject()(addressTransformer: AddressTransformer) extends JsonTransformer {

  private def getDirectorOrPartnerContactDetails(directorOrPartner: String): Reads[JsObject] = {
    (__ \ s"${directorOrPartner}ContactDetails" \ 'phone).json
      .copyFrom((__ \ 'correspondenceCommonDetails \ 'contactDetails \ 'telephone).json.pick) and
      (__ \ s"${directorOrPartner}ContactDetails" \ 'email).json
        .copyFrom((__ \ 'correspondenceCommonDetails \ 'contactDetails \ 'email).json.pick) reduce
  }

  private def getNino: Reads[JsObject] =
    (__ \ "nino").read[String].flatMap { _ =>
      (__ \ 'hasNino).json.put(JsBoolean(true)) and
        (__ \ "nino" \ 'value).json.copyFrom((__ \ 'nino).json.pick) reduce
    } orElse {
      (__ \ 'hasNino).json.put(JsBoolean(false)) and
        (__ \ 'noNinoReason).json.copyFrom((__ \ 'noNinoReason).json.pick) reduce
    } orElse {
      doNothing
    }

  private def getUtr: Reads[JsObject] =
    (__ \ "utr").read[String].flatMap { _ =>
      (__ \ 'hasUtr).json.put(JsBoolean(true)) and
        (__ \ "utr" \ 'value).json.copyFrom((__ \ 'utr).json.pick) reduce
    } orElse {
      (__ \ 'hasUtr).json.put(JsBoolean(false)) and
        (__ \ 'noUtrReason).json.copyFrom((__ \ 'noUtrReason).json.pick) reduce
    } orElse {
      doNothing
    }

  def getDirectorOrPartner(directorOrPartner: String): Reads[JsObject] = (__ \ s"${directorOrPartner}Details" \ 'firstName).json
    .copyFrom((__ \ 'firstName).json.pick) and
    ((__ \ s"${directorOrPartner}Details" \ 'middleName).json.copyFrom((__ \ 'middleName).json.pick) orElse doNothing) and
    (__ \ s"${directorOrPartner}Details" \ 'lastName).json.copyFrom((__ \ 'lastName).json.pick) and
    (__ \ 'dateOfBirth).json.copyFrom((__ \ 'dateOfBirth).json.pick) and
    getNino and
    getUtr and
    addressTransformer.getDifferentAddress(__ \ s"${directorOrPartner}Address", __ \ "correspondenceCommonDetails" \ "addressDetails") and
    getDirectorOrPartnerContactDetails(directorOrPartner) and
    addressTransformer.getAddressYears(addressYearsPath = __ \ s"${directorOrPartner}AddressYears") and
    addressTransformer.getPreviousAddress(__ \ s"${directorOrPartner}PreviousAddress") reduce

  def getDirectorsOrPartners(directorOrPartner: String): Reads[JsArray] = __.read(Reads.seq(getDirectorOrPartner(directorOrPartner))).map(JsArray(_))

  val getDirectorsOrPartners: Reads[JsObject] = {
    (__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "legalStatus").read[String].flatMap {
      case "Limited Company" => (__ \ 'directors).json.copyFrom((__ \ 'psaSubscriptionDetails \ 'directorOrPartnerDetails)
        .read(getDirectorsOrPartners("director"))) and
        ((__ \ 'moreThanTenDirectors).json
          .copyFrom((__ \ 'psaSubscriptionDetails \ 'numberOfDirectorsOrPartnersDetails \ 'isMorethanTenDirectors).json.pick) orElse doNothing) reduce
      case "Partnership" => (__ \ 'partners).json.copyFrom((__ \ 'psaSubscriptionDetails \ 'directorOrPartnerDetails)
        .read(getDirectorsOrPartners("partner"))) and
        ((__ \ 'moreThanTenPartners).json
          .copyFrom((__ \ 'psaSubscriptionDetails \ 'numberOfDirectorsOrPartnersDetails \ 'isMorethanTenPartners).json.pick) orElse doNothing) reduce
      case _ => doNothing
    }
  }
}