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

class AdviserTransformer @Inject()(addressTransformer: AddressTransformer) extends JsonTransformer {
  val getAdviser: Reads[JsObject] = {
    (((__ \ Symbol("adviserName")).json
      .copyFrom((__ \ Symbol("psaSubscriptionDetails") \ Symbol("declarationDetails") \ Symbol("pensionAdvisorDetails") \ Symbol("name")).json.pick)
      orElse doNothing) and
      ((__ \ Symbol("adviserEmail")).json
        .copyFrom(
          (__ \ Symbol("psaSubscriptionDetails") \ Symbol("declarationDetails") \ Symbol("pensionAdvisorDetails") \ Symbol("contactDetails")
            \ Symbol("email")).json.pick)
        orElse doNothing) and
      ((__ \ Symbol("adviserPhone")).json
        .copyFrom(
          (__ \ Symbol("psaSubscriptionDetails") \ Symbol("declarationDetails") \ Symbol("pensionAdvisorDetails") \ Symbol("contactDetails")
            \ Symbol("telephone")).json.pick)
        orElse doNothing) and
      (addressTransformer.getDifferentAddress(__ \ Symbol("adviserAddress"), __ \ Symbol("psaSubscriptionDetails") \ Symbol("declarationDetails")
        \ Symbol("pensionAdvisorDetails") \ Symbol("addressDetails"))
        orElse doNothing)) reduce
  }

  val getWorkingKnowledge: Reads[JsObject] = {
    (__ \ Symbol("psaSubscriptionDetails") \ Symbol("declarationDetails") \ Symbol("pensionAdvisorDetails") \ Symbol("name")).read[String].flatMap {
      _ =>
        (__ \ "declarationWorkingKnowledge").json.put(JsBoolean(false))
    } orElse {
      (__ \ "declarationWorkingKnowledge").json.put(JsBoolean(true))
    }
  }
}
