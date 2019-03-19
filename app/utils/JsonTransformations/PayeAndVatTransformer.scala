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

package utils.JsonTransformations

import com.google.inject.Inject
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{__, _}

class PayeAndVatTransformer @Inject()() extends JsonTransformer {
  val getPayeAndVat: Reads[JsObject] = {
    val vatRegistrationNumber = __ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'vatRegistrationNumber
    val paye = __ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'payeReference

    (__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "legalStatus").read[String].flatMap {
      case "Limited Company" =>
        ((__ \ 'companyDetails \ 'vatRegistrationNumber).json.copyFrom(vatRegistrationNumber.json.pick)
          orElse doNothing) and
          ((__ \ 'companyDetails \ 'payeEmployerReferenceNumber).json.copyFrom(paye.json.pick)
            orElse doNothing) reduce
      case "Partnership" =>
        val vatReads = (__ \ "psaSubscriptionDetails" \ "organisationOrPartnerDetails" \ "vatRegistrationNumber").read[String].flatMap { _ =>
          (__ \ 'partnershipVat \ 'vat).json.copyFrom(vatRegistrationNumber.json.pick) and
            (__ \ 'partnershipVat \ 'hasVat).json.put(JsBoolean(true)) reduce
        } orElse (__ \ 'partnershipVat \ 'hasVat).json.put(JsBoolean(false))

        val payeReads = (__ \ "psaSubscriptionDetails" \ "organisationOrPartnerDetails" \ "payeReference").read[String].flatMap { _ =>
          (__ \ 'partnershipPaye \ 'paye).json.copyFrom(paye.json.pick) and
            (__ \ 'partnershipPaye \ 'hasPaye).json.put(JsBoolean(true)) reduce
        } orElse (__ \ 'partnershipPaye \ 'hasPaye).json.put(JsBoolean(false))

        vatReads and payeReads reduce
      case "Individual" => doNothing
    }
  }
}
