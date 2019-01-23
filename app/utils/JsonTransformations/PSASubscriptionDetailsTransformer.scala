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

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{__, _}

object PSASubscriptionDetailsTransformer {
  val doNothing: Reads[JsObject] = __.json.put(Json.obj())

  def transformToUserAnswers(jsonFromDES: JsValue): Reads[JsObject] =
    getRegistrationInfo and
      getNinoOrUtr and
      getCrn and
      getOrganisationOrPartnerDetails.orElse(getIndividualDetails) and
      getPayeAndVat and
      getCorrespondenceAddress and
      getContactDetails and
      getAddressYearsBasedOnLegalStatus and
      getPreviousAddressBasedOnLegalStatus and
      getAdviser and
      getDirectorsOrPartners reduce


  private val getOrganisationOrPartnerDetails: Reads[JsObject] = {
    returnPathBasedOnLegalStatus(__, __ \ 'businessDetails, __ \ 'partnershipDetails).flatMap { orgPath =>
      (orgPath \ 'companyName).json.copyFrom((__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'name).json.pick)
    }
  }

  private val getRegistrationInfo: Reads[JsObject] = {
    (__ \ "psaSubscriptionDetails" \ "correspondenceAddressDetails" \ "nonUKAddress").read[Boolean].flatMap { flag =>
      (__ \ 'registrationInfo \ 'legalStatus).json.copyFrom((__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "legalStatus").json.pick) and
        (__ \ 'registrationInfo \ 'sapNumber).json.put(JsString("")) and
        (__ \ 'registrationInfo \ 'noIdentifier).json.put(JsBoolean(false)) and
        (__ \ 'registrationInfo \ 'customerType).json.put(if (flag) JsString("NON-UK") else JsString("UK")) and
        (__ \ 'registrationInfo \ 'idType).json.copyFrom((__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "idType").json.pick) and
        (__ \ 'registrationInfo \ 'idNumber).json.copyFrom((__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "idNumber").json.pick) reduce
    }
  }

  private val getCrn = ((__ \ 'companyRegistrationNumber).json.copyFrom((__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'crnNumber).json.pick)
    orElse doNothing)

  private val getNinoOrUtr: Reads[JsObject] = {
    returnPathBasedOnLegalStatus(__ \ 'individualNino, __ \ 'businessDetails, __ \ 'partnershipDetails).flatMap { userAnswersPath =>
      (__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "idType").read[String].flatMap { value =>
        if (value.contains("UTR")) {
          (userAnswersPath \ 'uniqueTaxReferenceNumber).json.copyFrom((__ \ 'psaSubscriptionDetails \ 'customerIdentificationDetails \ 'idNumber).json.pick)
        } else {
          userAnswersPath.json.copyFrom((__ \ 'psaSubscriptionDetails \ 'customerIdentificationDetails \ 'idNumber).json.pick)
        }
      }
    }
  }

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

  val getCorrespondenceAddress: Reads[JsObject] = {
    val inputAddressPath = __ \ 'psaSubscriptionDetails \ 'correspondenceAddressDetails

    (__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "legalStatus").read[String].flatMap {
      case "Individual" => getAddress(__ \ 'individualContactAddress, inputAddressPath)
      case "Limited Company" => getDifferentAddress(__ \ 'companyContactAddress, inputAddressPath)
      case "Partnership" => getDifferentAddress(__ \ 'partnershipContactAddress, inputAddressPath)
    }
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

  def returnPathBasedOnLegalStatus(individualPath: JsPath, companyPath: JsPath, partnershipPath: JsPath): Reads[JsPath] = {
    (__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "legalStatus").read[String].map {
      case "Individual" =>
        individualPath
      case "Limited Company" =>
        companyPath
      case "Partnership" =>
        partnershipPath
    }
  }

  private val getContactDetails: Reads[JsObject] =
    returnPathBasedOnLegalStatus(__ \ 'individualContactDetails, __ \ 'contactDetails, __ \ 'partnershipContactDetails).flatMap(getContact)

  val getAddressYearsBasedOnLegalStatus: Reads[JsObject] = {
    returnPathBasedOnLegalStatus(__ \ 'individualAddressYears, __ \ 'companyAddressYears, __ \ 'partnershipAddressYears).flatMap { addressYearsPath =>
      getAddressYears(__ \ "psaSubscriptionDetails", addressYearsPath)
    }
  }

  private def getAddressYears(path: JsPath = __, addressYearsPath: JsPath = __): Reads[JsObject] = {
    (path \ "previousAddressDetails" \ "isPreviousAddressLast12Month").read[Boolean].flatMap { addressYearsValue =>
      val value = if (addressYearsValue) {
        JsString("under_a_year")
      } else {
        JsString("over_a_year")
      }
      addressYearsPath.json.put(value)
    }
  }

  private val getPreviousAddressBasedOnLegalStatus: Reads[JsObject] = {
    returnPathBasedOnLegalStatus(__ \ 'individualPreviousAddress, __ \ 'companyPreviousAddress, __ \ 'partnershipPreviousAddress).flatMap {
      getDifferentAddress(_, __ \ 'psaSubscriptionDetails \ 'previousAddressDetails \ 'previousAddress)
    }
  }

  private def getPreviousAddress(path: JsPath) : Reads[JsObject] = {
    (__ \ 'previousAddressDetails \ 'previousAddress).read[JsObject].flatMap { _ =>
      getDifferentAddress(path , __ \ 'previousAddressDetails \ 'previousAddress)
    } orElse doNothing
  }

  private val getAdviser: Reads[JsObject] = {
    ((__ \ 'adviserDetails \ 'name).json.copyFrom((__ \ 'psaSubscriptionDetails \ 'declarationDetails \ 'pensionAdvisorDetails \ 'name).json.pick)
      orElse doNothing) and
      ((__ \ 'adviserDetails \ 'email).json.copyFrom(
        (__ \ 'psaSubscriptionDetails \ 'declarationDetails \ 'pensionAdvisorDetails \ 'contactDetails \ 'email).json.pick)
        orElse doNothing) and
      ((__ \ 'adviserDetails \ 'phone).json.copyFrom(
        (__ \ 'psaSubscriptionDetails \ 'declarationDetails \ 'pensionAdvisorDetails \ 'contactDetails \ 'telephone).json.pick)
        orElse doNothing) and
      (getDifferentAddress(__ \ 'adviserAddress, __ \ 'psaSubscriptionDetails \ 'declarationDetails \ 'pensionAdvisorDetails \ 'addressDetails)
        orElse doNothing) reduce
  }

  private val getDirectorNino: Reads[JsObject] = {
    (__ \ "nino").read[String].flatMap {
      _ =>
        (__ \ 'directorNino \ 'nino).json.copyFrom((__ \ 'nino).json.pick) and
          (__ \ 'directorNino \ 'hasNino).json.put(JsBoolean(true)) reduce
    } orElse {
      (__ \ 'directorNino \ 'reason).json.copyFrom((__ \ 'noNinoReason).json.pick) and
        (__ \ 'directorNino \ 'hasNino).json.put(JsBoolean(false)) reduce
    }
  }

  private val getDirectorUtr: Reads[JsObject] = {
    (__ \ "utr").read[String].flatMap {
      _ =>
        (__ \ 'directorUtr \ 'utr).json.copyFrom((__ \ 'utr).json.pick) and
          (__ \ 'directorUtr \ 'hasUtr).json.put(JsBoolean(true)) reduce
    } orElse {
      (__ \ 'directorUtr \ 'reason).json.copyFrom((__ \ 'noUtrReason).json.pick) and
        (__ \ 'directorUtr \ 'hasUtr).json.put(JsBoolean(false)) reduce
    }
  }

  private val getDirectorcontactDetails: Reads[JsObject] = {
    (__ \ 'directorContactDetails \ 'phone).json.copyFrom((__ \ 'correspondenceCommonDetails \ 'contactDetails \ 'telephone).json.pick) and
      (__ \ 'directorContactDetails \ 'email).json.copyFrom((__ \ 'correspondenceCommonDetails \ 'contactDetails \ 'email).json.pick) reduce
  }

  val getDirector = (__ \ 'directorDetails \ 'firstName).json.copyFrom((__ \ 'firstName).json.pick) and
    ((__ \ 'directorDetails \ 'middleName).json.copyFrom((__ \ 'middleName).json.pick) orElse doNothing) and
    (__ \ 'directorDetails \ 'lastName).json.copyFrom((__ \ 'lastName).json.pick) and
    (__ \ 'directorDetails \ 'dateOfBirth).json.copyFrom((__ \ 'dateOfBirth).json.pick) and
    getDirectorNino and
    getDirectorUtr and
    PSASubscriptionDetailsTransformer.getAddress(__ \ "directorAddress", __ \ "correspondenceCommonDetails" \ "addressDetails") and
    getDirectorcontactDetails and
    PSASubscriptionDetailsTransformer.getAddressYears(addressYearsPath = __ \ 'directorAddressYears) and
    PSASubscriptionDetailsTransformer.getPreviousAddress(__ \ "directorPreviousAddress") reduce


  val getDirectors: Reads[JsArray] = __.read(Reads.seq(getDirector)).map(JsArray(_))


  private val getPartnerNino: Reads[JsObject] = {
    (__ \ "nino").read[String].flatMap {
      _ =>
        (__ \ 'partnerNino \ 'nino).json.copyFrom((__ \ 'nino).json.pick) and
          (__ \ 'partnerNino \ 'hasNino).json.put(JsBoolean(true)) reduce
    } orElse {
      (__ \ 'partnerNino \ 'reason).json.copyFrom((__ \ 'noNinoReason).json.pick) and
        (__ \ 'partnerNino \ 'hasNino).json.put(JsBoolean(false)) reduce
    }
  }

  private val getPartnerUtr: Reads[JsObject] = {
    (__ \ "utr").read[String].flatMap {
      _ =>
        (__ \ 'partnerUtr \ 'utr).json.copyFrom((__ \ 'utr).json.pick) and
          (__ \ 'partnerUtr \ 'hasUtr).json.put(JsBoolean(true)) reduce
    } orElse {
      (__ \ 'partnerUtr \ 'reason).json.copyFrom((__ \ 'noUtrReason).json.pick) and
        (__ \ 'partnerUtr \ 'hasUtr).json.put(JsBoolean(false)) reduce
    }
  }

  private val getPartnercontactDetails: Reads[JsObject] = {
    (__ \ 'partnerContactDetails \ 'phone).json.copyFrom((__ \ 'correspondenceCommonDetails \ 'contactDetails \ 'telephone).json.pick) and
      (__ \ 'partnerContactDetails \ 'email).json.copyFrom((__ \ 'correspondenceCommonDetails \ 'contactDetails \ 'email).json.pick) reduce
  }

  val getPartner = (__ \ 'partnerDetails \ 'firstName).json.copyFrom((__ \ 'firstName).json.pick) and
    ((__ \ 'partnerDetails \ 'middleName).json.copyFrom((__ \ 'middleName).json.pick) orElse doNothing) and
    (__ \ 'partnerDetails \ 'lastName).json.copyFrom((__ \ 'lastName).json.pick) and
    (__ \ 'partnerDetails \ 'dateOfBirth).json.copyFrom((__ \ 'dateOfBirth).json.pick) and
    getPartnerNino and
    getPartnerUtr and
    PSASubscriptionDetailsTransformer.getAddress(__ \ "partnerAddress", __ \ "correspondenceCommonDetails" \ "addressDetails") and
    getPartnercontactDetails and
    PSASubscriptionDetailsTransformer.getAddressYears(addressYearsPath = __ \ 'partnerAddressYears) and
    PSASubscriptionDetailsTransformer.getPreviousAddress(__ \ "partnerPreviousAddress") reduce


  val getPartners: Reads[JsArray] = __.read(Reads.seq(getPartner)).map(JsArray(_))


  val getDirectorsOrPartners: Reads[JsObject] = {
    (__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "legalStatus").read[String].flatMap {
      case "Limited Company" => (__ \ 'directors).json.copyFrom((__ \ 'psaSubscriptionDetails \ 'directorOrPartnerDetails).read(getDirectors))
      case "Partnership" => doNothing
      case _ => doNothing
    }
  }
}
