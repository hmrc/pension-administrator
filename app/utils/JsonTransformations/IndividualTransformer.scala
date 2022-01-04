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
import play.api.libs.json.{__, _}

class IndividualTransformer @Inject()(legalStatusTransformer: LegalStatusTransformer) extends JsonTransformer {
  val getNinoOrUtr: Reads[JsObject] =
      (__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "idType").readNullable[String].flatMap { value =>
        if (value.contains("UTR"))
            ( (__ \ 'utr).json.copyFrom((__ \ 'psaSubscriptionDetails \ 'customerIdentificationDetails \ 'idNumber).json.pick)
              orElse doNothing)
        else
            ((__ \ 'individualNino).json.copyFrom((__ \ 'psaSubscriptionDetails \ 'customerIdentificationDetails \ 'idNumber).json.pick)
              orElse doNothing)
      }

  val getIndividualDetails: Reads[JsObject] = {
    val individualDetailsPath = __ \ 'psaSubscriptionDetails \ 'individualDetails
    (__ \ 'individualDetails \ 'firstName).json.copyFrom((individualDetailsPath \ 'firstName).json.pick) and
      ((__ \ 'individualDetails \ 'middleName).json.copyFrom((individualDetailsPath \ 'middleName).json.pick)
        orElse doNothing) and
      (__ \ 'individualDetails \ 'lastName).json.copyFrom((individualDetailsPath \ 'lastName).json.pick) and
      (__ \ 'individualDateOfBirth).json.copyFrom((individualDetailsPath \ 'dateOfBirth).json.pick) reduce
  }

  private def getContact(userAnswersPath: JsPath): Reads[JsObject] = {
    val contactAddressPath = __ \ 'psaSubscriptionDetails \ 'correspondenceContactDetails
    (userAnswersPath \ 'phone).json.copyFrom((contactAddressPath \ 'telephone).json.pick) and
      ((userAnswersPath \ 'email).json.copyFrom((contactAddressPath \ 'email).json.pick) //TODO: Mandatory in frontend but optional in DES
        orElse doNothing) reduce
  }

  val getContactDetails: Reads[JsObject] =
    legalStatusTransformer.returnPathBasedOnLegalStatus(
      individualPath = __ \ 'individualContactDetails,
      companyPath = __ \ 'contactDetails,
      partnershipPath = __ \ 'partnershipContactDetails
    ).flatMap(getContact)
}