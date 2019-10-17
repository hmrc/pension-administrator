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

package models.Reads

import models.{Address, PensionSchemeAdministrator, Samples, UkAddress, Reads => _}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{Json, _}

class PensionSchemeAdministratorReadsIndividualSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "JSON Payload of an individual PSA" should {
    "Map to a valid PensionSchemeAdministrator object" when {
      val input = Json.obj(
        "registrationInfo" -> Json.obj(
          "legalStatus" -> "Individual",
          "sapNumber" -> "NumberTest",
          "noIdentifier" -> JsBoolean(true),
          "customerType" -> "TestCustomer",
          "idType" -> JsString("TestId"),
          "idNumber" -> JsString("TestIdNumber")
        ),
        "individualContactDetails" -> Json.obj("phone" -> "07592113", "email" -> "test@test.com"),
        "individualAddressYears" -> JsString("over_a_year"),
        //  "individualPreviousAddress" -> JsObject(Map("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"))),
        "individualContactAddress" -> JsObject(Map("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"),
          "addressLine4" -> JsString("line4"), "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))),
        "individualDetails" -> Json.obj(
          "firstName" -> JsString("John"),
          "middleName" -> JsString("Does Does"),
          "lastName" -> JsString("Doe")
        ),
        "individualDateOfBirth" -> JsString("2019-01-31"),
        "existingPSA" -> Json.obj("isExistingPSA" -> JsBoolean(false)),
        "declaration" -> JsBoolean(true),
        "declarationFitAndProper" -> JsBoolean(true),
        "declarationWorkingKnowledge" -> "workingKnowledge"
      )

      "We have a valid legalStatus" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.legalStatus mustEqual pensionSchemeAdministratorSampleIndividual.legalStatus
      }

      "We have a valid sapNumber" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.sapNumber mustEqual pensionSchemeAdministratorSample.sapNumber
      }

      "We have a valid noIdentifier" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.noIdentifier mustEqual pensionSchemeAdministratorSample.noIdentifier
      }

      "We have valid customerType" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.customerType mustEqual pensionSchemeAdministratorSample.customerType
      }

      "We have a valid idType" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.idType mustEqual pensionSchemeAdministratorSample.idType
      }

      "We have a valid idNumber" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value


        result.idNumber mustEqual pensionSchemeAdministratorSample.idNumber
      }

      "We have contact details" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.correspondenceContactDetail.telephone mustBe pensionSchemeAdministratorSample.correspondenceContactDetail.telephone
      }

      "We have previous address details" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.previousAddressDetail.isPreviousAddressLast12Month mustBe pensionSchemeAdministratorSample.previousAddressDetail.isPreviousAddressLast12Month
      }

      "We have correspondence address when the contact Address toggle is on" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.correspondenceAddressDetail mustBe ukAddressSample
      }

      "We have individual details" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value
        result.individualDetail.value.dateOfBirth mustBe individualSample.dateOfBirth
      }

      "We have individual details but no organisation details" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value
        result.organisationDetail mustBe None
      }

      "We have individual with Individual Contact Details" in {
        val expectedContactDetails = contactDetailsSample.copy(telephone = "11111")
        val individiualContactDetails = "individualContactDetails" -> Json.obj("phone" -> "11111", "email" -> "test@test.com")
        val result =
          Json.fromJson[PensionSchemeAdministrator](input + individiualContactDetails - "contactDetails")(PensionSchemeAdministrator.apiReads).asOpt.value

        result.correspondenceContactDetail.telephone mustBe expectedContactDetails.telephone
      }

      "We have an individual contact address" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value
        result.correspondenceAddressDetail.asInstanceOf[UkAddress] mustBe ukAddressSample
      }

      "We have an individual previous address > 1 year" in {
        val expectedIndividualPreviousAddress = previousAddressDetailsSample.copy(isPreviousAddressLast12Month = false, previousAddressDetails = None)
        val result =
          Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.previousAddressDetail.isPreviousAddressLast12Month mustBe expectedIndividualPreviousAddress.isPreviousAddressLast12Month
      }

      "We have an individual previous address < 1 year" in {
        val expectedIndividualPreviousAddress = previousAddressDetailsSample.copy(isPreviousAddressLast12Month = true, previousAddressDetails = Some(ukAddressSample))
        val individualAddressYears = "individualAddressYears" -> JsString("under_a_year")
        val individualPreviousAddress = "individualPreviousAddress" -> JsObject(Map(
          "addressLine1" -> JsString("line1"),
          "addressLine2" -> JsString("line2"),
          "addressLine3" -> JsString("line3"),
          "addressLine4" -> JsString("line4"),
          "postalCode" -> JsString("NE1"),
          "countryCode" -> JsString("GB")
        ))

        val input2 = input + individualAddressYears + individualPreviousAddress

        val result =
          Json.fromJson[PensionSchemeAdministrator](input2)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.previousAddressDetail.isPreviousAddressLast12Month mustBe expectedIndividualPreviousAddress.isPreviousAddressLast12Month
      }

      "The user is not an existing PSA user" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.pensionSchemeAdministratoridentifierStatus.isExistingPensionSchemaAdministrator mustBe
          pensionSchemeAdministratorSample.pensionSchemeAdministratoridentifierStatus.isExistingPensionSchemaAdministrator
      }

      "The user is an existing PSA user with no previous reference" in {
        val existingPSA = "existingPSA" -> Json.obj("isExistingPSA" -> JsBoolean(true))
        val result = Json.fromJson[PensionSchemeAdministrator](input + existingPSA)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.pensionSchemeAdministratoridentifierStatus.isExistingPensionSchemaAdministrator mustBe true
      }

      "The user is an existing PSA user with previous reference number" in {
        val existingPSA = "existingPSA" -> Json.obj("isExistingPSA" -> JsBoolean(true), "existingPSAId" -> JsString("TestId"))
        val result = Json.fromJson[PensionSchemeAdministrator](input + existingPSA)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.pensionSchemeAdministratoridentifierStatus.existingPensionSchemaAdministratorReference mustBe Some("TestId")
      }

      "We have a declaration" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.declaration mustBe pensionSchemeAdministratorSample.declaration
      }
    }
  }
}
