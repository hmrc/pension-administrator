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

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._

object PSASubscriptionDetailsTransformer {
  def doNothing: Reads[JsObject] = __.json.put(Json.obj())
  
  def transformToUserAnswers(jsonFromDES: JsValue): Reads[JsObject] =
    (if ((jsonFromDES \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "idType").asOpt[String].contains("UTR")) {
      (__ \ 'businessDetails \ 'uniqueTaxReferenceNumber).json.copyFrom((__ \ 'psaSubscriptionDetails \ 'customerIdentificationDetails \ 'idNumber).json.pick)
    } else doNothing) and
      getOrganisationOrPartnerDetails and
      getIndividualDetails and
      getCorrespondenceAddress and
      getContactDetails reduce

  private def getOrganisationOrPartnerDetails: Reads[JsObject] = {
    val organisationOrPartnerDetailsPath = __ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails

    (__ \ 'companyRegistrationNumber).json.copyFrom((organisationOrPartnerDetailsPath \ 'crnNumber).json.pick) and
      (__ \ 'businessDetails \ 'companyName).json.copyFrom((organisationOrPartnerDetailsPath \ 'name).json.pick) and
      ((__ \ 'companyDetails \ 'vatRegistrationNumber).json.copyFrom((organisationOrPartnerDetailsPath \ 'vatRegistrationNumber).json.pick)
        orElse doNothing) and
      (__ \ 'companyDetails \ 'payeEmployerReferenceNumber).json.copyFrom((organisationOrPartnerDetailsPath \ 'payeReference).json.pick) reduce
  }

  def getAddress(userAnswersPath: JsPath, desAddressPath: JsPath): Reads[JsObject] = {
    (userAnswersPath \ 'addressLine1).json.copyFrom((desAddressPath \ 'line1).json.pick) and
      (userAnswersPath \ 'addressLine2).json.copyFrom((desAddressPath \ 'line2).json.pick) and
      ((userAnswersPath \ 'addressLine3).json.copyFrom((desAddressPath \ 'line3).json.pick)
        orElse doNothing) and
      ((userAnswersPath \ 'addressLine4).json.copyFrom((desAddressPath \ 'line4).json.pick)
        orElse doNothing) and
      ((userAnswersPath \ 'postalCode).json.copyFrom((desAddressPath \ 'postalCode).json.pick)
        orElse doNothing) and
      (userAnswersPath \ 'countryCode).json.copyFrom((desAddressPath \ 'countryCode).json.pick) reduce
  }


  val getCorrespondenceAddress: Reads[JsObject] = {
    (__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "legalStatus").read[String].flatMap(
      legalStatus => {
        val path: JsPath = legalStatus match {
          case "Individual" => __ \ 'individualAddress
          case "Limited Company" => __ \ 'companyAddressId
          case "Partnership" => __ \ 'partnershipContactAddress
        }
        getAddress(path, __ \ 'psaSubscriptionDetails \ 'correspondenceAddressDetails)
      }
    )
  }

  private def getIndividualDetails: Reads[JsObject] = {
    val individualDetailsPath = __ \ 'psaSubscriptionDetails \ 'individualDetails
    (__ \ 'individualDetails \ 'firstName).json.copyFrom((individualDetailsPath \ 'firstName).json.pick) and
      ((__ \ 'individualDetails \ 'middleName).json.copyFrom((individualDetailsPath \ 'middleName).json.pick)
        orElse doNothing) and
      (__ \ 'individualDetails \ 'lastName).json.copyFrom((individualDetailsPath \ 'lastName).json.pick) and
      (__ \ 'individualDateOfBirth).json.copyFrom((individualDetailsPath \ 'dateOfBirth).json.pick) reduce
  }

  private def getContactDetails: Reads[JsObject] = {
    val contactAddressPath = __ \ 'psaSubscriptionDetails \ 'correspondenceContactDetails
    (__ \ 'contactDetails \ 'phone).json.copyFrom((contactAddressPath \ 'telephone).json.pick) and
      ((__ \ 'contactDetails \ 'email).json.copyFrom((contactAddressPath \ 'email).json.pick)
        orElse doNothing) reduce
  }
}
