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

class IndividualTransformer @Inject()(legalStatusTransformer: LegalStatusTransformer) extends JsonTransformer {
  val getNinoOrUtr: Reads[JsObject] =
    (__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "idType").readNullable[String].flatMap { value =>
      if (value.contains("UTR"))
        ((__ \ Symbol("utr")).json.copyFrom((__ \ Symbol("psaSubscriptionDetails") \ Symbol("customerIdentificationDetails") \ Symbol("idNumber")).json.pick)
          orElse doNothing)
      else
        ((__ \ Symbol("individualNino")).json.copyFrom((__ \ Symbol("psaSubscriptionDetails") \ Symbol("customerIdentificationDetails")
          \ Symbol("idNumber")).json.pick)
          orElse doNothing)
    }

  val getIndividualDetails: Reads[JsObject] = {
    val individualDetailsPath = __ \ Symbol("psaSubscriptionDetails") \ Symbol("individualDetails")
    (__ \ Symbol("individualDetails") \ Symbol("firstName")).json.copyFrom((individualDetailsPath \ Symbol("firstName")).json.pick) and
      ((__ \ Symbol("individualDetails") \ Symbol("middleName")).json.copyFrom((individualDetailsPath \ Symbol("middleName")).json.pick)
        orElse doNothing) and
      (__ \ Symbol("individualDetails") \ Symbol("lastName")).json.copyFrom((individualDetailsPath \ Symbol("lastName")).json.pick) and
      (__ \ Symbol("individualDateOfBirth")).json.copyFrom((individualDetailsPath \ Symbol("dateOfBirth")).json.pick) reduce
  }

  private def getContact(userAnswersPath: JsPath): Reads[JsObject] = {
    val contactAddressPath = __ \ Symbol("psaSubscriptionDetails") \ Symbol("correspondenceContactDetails")
    (userAnswersPath \ Symbol("phone")).json.copyFrom((contactAddressPath \ Symbol("telephone")).json.pick) and
      ((userAnswersPath \ Symbol("email")).json.copyFrom((contactAddressPath \ Symbol("email")).json.pick) //TODO: Mandatory in frontend but optional in DES
        orElse doNothing) reduce
  }

  val getContactDetails: Reads[JsObject] =
    legalStatusTransformer.returnPathBasedOnLegalStatus(
      individualPath = __ \ Symbol("individualContactDetails"),
      companyPath = __ \ Symbol("contactDetails"),
      partnershipPath = __ \ Symbol("partnershipContactDetails")
    ).flatMap(getContact)
}
