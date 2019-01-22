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
    getUtr(jsonFromDES) and
      ((__ \ 'companyRegistrationNumber).json.copyFrom((__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'crnNumber).json.pick)
        orElse doNothing) and
      getOrganisationOrPartnerDetails(jsonFromDES).orElse(getIndividualDetails) and
      getPayeAndVat(jsonFromDES) and
      getCorrespondenceAddress(jsonFromDES) and
      getContactDetails(jsonFromDES) and
      getAddressYears(jsonFromDES) and
      getPreviousAddress(jsonFromDES) reduce

  private def getOrganisationOrPartnerDetails(jsonFromDES: JsValue): Reads[JsObject] = {
    val orgPath = returnPathBasedOnLegalStatus(jsonFromDES, __, __ \ 'businessDetails, __ \ 'partnershipDetails)
    (orgPath \ 'companyName).json.copyFrom((__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'name).json.pick)
  }

  private def getUtr(jsonFromDES: JsValue): Reads[JsObject] = {
    val userAnswersPath = returnPathBasedOnLegalStatus(jsonFromDES, __, __ \ 'businessDetails, __ \ 'partnershipDetails)
    if ((jsonFromDES \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "idType").asOpt[String].contains("UTR")) {
      (userAnswersPath \ 'uniqueTaxReferenceNumber).json.copyFrom((__ \ 'psaSubscriptionDetails \ 'customerIdentificationDetails \ 'idNumber).json.pick)
    } else doNothing
  }

  private def getPayeAndVat(jsonFromDES: JsValue): Reads[JsObject] = {
    val legalStatus = (jsonFromDES \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "legalStatus").as[String]
    val vatRegistrationNumber = __ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'vatRegistrationNumber
    val paye = __ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'payeReference
    legalStatus match {
      case "Limited Company" =>
        (__ \ 'companyDetails \ 'vatRegistrationNumber).json.copyFrom(vatRegistrationNumber.json.pick) and
          (__ \ 'companyDetails \ 'payeEmployerReferenceNumber).json.copyFrom(paye.json.pick) reduce
      case "Partnership" =>
        val vatValue = (jsonFromDES \ "psaSubscriptionDetails" \ "organisationOrPartnerDetails" \ "vatRegistrationNumber").asOpt[String]
        val payeValue = (jsonFromDES \ "psaSubscriptionDetails" \ "organisationOrPartnerDetails" \ "payeReference").asOpt[String]
        val vatReads = if(vatValue.isEmpty){
            (__ \ 'partnershipVat \ 'hasVat).json.put(JsBoolean(false))
        } else {
          (__ \ 'partnershipVat \ 'vat).json.copyFrom(vatRegistrationNumber.json.pick) and
            (__ \ 'partnershipVat \ 'hasVat).json.put(JsBoolean(true)) reduce
        }
        val payeReads = if(payeValue.isEmpty){
          (__ \ 'partnershipPaye \ 'hasPaye).json.put(JsBoolean(false))
        } else {
          (__ \ 'partnershipPaye \ 'paye).json.copyFrom(paye.json.pick) and
            (__ \ 'partnershipPaye \ 'hasPaye).json.put(JsBoolean(true)) reduce
        }
        vatReads and payeReads reduce
      case "Individual" => doNothing
    }
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

  def getDifferentAddress(userAnswersPath: JsPath, desAddressPath: JsPath): Reads[JsObject] = {
    (userAnswersPath \ 'addressLine1).json.copyFrom((desAddressPath \ 'line1).json.pick) and
      (userAnswersPath \ 'addressLine2).json.copyFrom((desAddressPath \ 'line2).json.pick) and
      ((userAnswersPath \ 'addressLine3).json.copyFrom((desAddressPath \ 'line3).json.pick)
        orElse doNothing) and
      ((userAnswersPath \ 'addressLine4).json.copyFrom((desAddressPath \ 'line4).json.pick)
        orElse doNothing) and
      ((userAnswersPath \ 'postcode).json.copyFrom((desAddressPath \ 'postalCode).json.pick)
        orElse doNothing) and
      (userAnswersPath \ 'country).json.copyFrom((desAddressPath \ 'countryCode).json.pick) reduce
  }

  private def getCorrespondenceAddress(jsonFromDES: JsValue): Reads[JsObject] = {
    val legalStatus = (jsonFromDES \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "legalStatus").as[String]
    val inputAddressPath = __ \ 'psaSubscriptionDetails \ 'correspondenceAddressDetails

    legalStatus match {
      case "Individual" =>
        getAddress(__ \ 'individualContactAddress, inputAddressPath)
      case "Limited Company" =>
        getDifferentAddress(__ \ 'companyContactAddress, inputAddressPath)
      case "Partnership" =>
        getDifferentAddress(__ \ 'partnershipContactAddress, inputAddressPath)
    }
  }

  private def getIndividualDetails: Reads[JsObject] = {
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

  private def returnPathBasedOnLegalStatus(jsonFromDES: JsValue, individualPath: JsPath, companyPath: JsPath, partnershipPath: JsPath): JsPath = {
    val legalStatus = (jsonFromDES \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "legalStatus").as[String]
    legalStatus match {
      case "Individual" =>
        individualPath
      case "Limited Company" =>
        companyPath
      case "Partnership" =>
        partnershipPath
    }
  }

  private def getContactDetails(jsonFromDES: JsValue): Reads[JsObject] = {
    val contactPath = returnPathBasedOnLegalStatus(jsonFromDES, __ \ 'individualContactDetails, __ \ 'contactDetails, __ \ 'partnershipContactDetails)
    getContact(contactPath)
  }

  private def getAddressYears(jsonFromDES: JsValue): Reads[JsObject] = {
    val isPreviousAddressLast12Month = (jsonFromDES \ "psaSubscriptionDetails" \ "previousAddressDetails" \ "isPreviousAddressLast12Month").as[Boolean]
    val addressYearsValue = if (isPreviousAddressLast12Month) {
      JsString("under_a_year")
    } else {
      JsString("over_a_year")
    }

    returnPathBasedOnLegalStatus(jsonFromDES,
      __ \ 'individualAddressYears, __ \ 'companyAddressYears, __ \ 'partnershipAddressYears).json.put(addressYearsValue)
  }

  private def getPreviousAddress(jsonFromDES: JsValue): Reads[JsObject] = {
    val previousAddressPath: JsPath = returnPathBasedOnLegalStatus(jsonFromDES,
      __ \ 'individualPreviousAddress, __ \ 'companyPreviousAddress, __ \ 'partnershipPreviousAddress)
    getDifferentAddress(previousAddressPath, __ \ 'psaSubscriptionDetails \ 'previousAddressDetails \ 'previousAddress)
  }
}
