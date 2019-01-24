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

class RegistrationInfoTransformer @Inject()() extends JsonTransformer {
  val getRegistrationInfo: Reads[JsObject] = {
    (__ \ "psaSubscriptionDetails" \ "correspondenceAddressDetails" \ "nonUKAddress").read[Boolean].flatMap { flag =>
      (__ \ 'registrationInfo \ 'legalStatus).json.copyFrom((__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "legalStatus").json.pick) and
        (__ \ 'registrationInfo \ 'sapNumber).json.put(JsString("")) and
        (__ \ 'registrationInfo \ 'noIdentifier).json.put(JsBoolean(false)) and
        (__ \ 'registrationInfo \ 'customerType).json.put(if (flag) JsString("NON-UK") else JsString("UK")) and
        (__ \ 'registrationInfo \ 'idType).json.copyFrom((__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "idType").json.pick) and
        (__ \ 'registrationInfo \ 'idNumber).json.copyFrom((__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "idNumber").json.pick) reduce
    }
  }
}
