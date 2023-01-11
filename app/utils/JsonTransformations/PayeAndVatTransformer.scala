/*
 * Copyright 2023 HM Revenue & Customs
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

class PayeAndVatTransformer @Inject()() extends JsonTransformer {
  val getPayeAndVat: Reads[JsObject] = {
    (__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "legalStatus").read[String].flatMap {
      case "Limited Company" =>
        (getVat and
          getPaye).reduce
      case "Partnership" =>
        (getVat and
          getPaye).reduce
      case "Individual" => doNothing
    }
  }

  private def getVat: Reads[JsObject] = {
    val vatRegistrationNumber = __ \ Symbol("psaSubscriptionDetails") \ Symbol("organisationOrPartnerDetails") \ Symbol("vatRegistrationNumber")
    vatRegistrationNumber.read[String].flatMap { _ =>
      (__ \ Symbol("hasVat")).json.put(JsBoolean(true)) and
        (__ \ "vat").json.copyFrom(vatRegistrationNumber.json.pick) reduce
    } orElse {
      (__ \ Symbol("hasVat")).json.put(JsBoolean(false))
    } orElse {
      doNothing
    }
  }

  private def getPaye: Reads[JsObject] = {
    val paye = __ \ Symbol("psaSubscriptionDetails") \ Symbol("organisationOrPartnerDetails") \ Symbol("payeReference")
    paye.read[String].flatMap { _ =>
      (__ \ Symbol("hasPaye")).json.put(JsBoolean(true)) and
        (__ \ "paye").json.copyFrom(paye.json.pick) reduce
    } orElse {
      (__ \ Symbol("hasPaye")).json.put(JsBoolean(false))
    } orElse {
      doNothing
    }
  }
}
