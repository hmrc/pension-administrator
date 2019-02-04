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

import models.{PensionSchemeAdministrator, Samples, UkAddress}
import models.{Reads => _}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{Json, _}
import utils.JsonUtils._

class PensionSchemeAdministratorReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {

  implicit val contactAddressEnabled: Boolean = true

  "JSON Payload of a PSA" should {
    "Map to a valid PensionSchemeAdministrator object" when {
      val input = Json.obj("existingPSA" -> Json.obj("isExistingPSA" -> JsBoolean(false)), "registrationInfo" -> Json.obj("legalStatus" -> "Limited Company",
        "sapNumber" -> "NumberTest",
        "noIdentifier" -> JsBoolean(true),
        "customerType" -> "TestCustomer",
        "idType" -> JsString("TestId"),
        "idNumber" -> JsString("TestIdNumber")),
        "contactDetails" -> Json.obj("phone" -> "07592113", "email" -> "test@test.com"),
        "companyAddressYears" -> JsString("over_a_year"),
        "companyContactAddress" -> JsObject(Map("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"),
          "addressLine4" -> JsString("line4"), "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))),
        "companyDetails" -> Json.obj("vatRegistrationNumber" -> JsString("VAT11111"), "payeEmployerReferenceNumber" -> JsString("PAYE11111")),
        "companyRegistrationNumber" -> JsString("CRN11111"),
        "businessDetails" -> Json.obj("companyName" -> JsString("Company Test")),
        "declaration" -> JsBoolean(true),
        "declarationFitAndProper" -> JsBoolean(true),
        "declarationWorkingKnowledge" -> "workingKnowledge") + ("directors" -> JsArray(Seq(testDirectorOrPartner("director"))))

      "We have a valid legalStatus" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.legalStatus mustEqual pensionSchemeAdministratorSample.legalStatus
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

      "We have a moreThanTenDirectors flag" in {
        val result =
          Json.fromJson[PensionSchemeAdministrator](input + ("moreThanTenDirectors" -> JsBoolean(true)))(PensionSchemeAdministrator.apiReads).asOpt.value

        result.numberOfDirectorOrPartners.value.isMorethanTenDirectors mustEqual
          pensionSchemeAdministratorSample.numberOfDirectorOrPartners.value.isMorethanTenDirectors
      }

      "We don't have moreThanTenDirectors flag" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.numberOfDirectorOrPartners mustBe None
      }

      "We have a flag for isMoreThanTenDirectorsOrPartnersChanged" in {
        val psaWithUpdatedMoreThan10Directors = input + ("isMoreThanTenDirectorsOrPartnersChanged" -> JsBoolean(true)) + ("moreThanTenDirectors" -> JsBoolean(true))
        val result = psaWithUpdatedMoreThan10Directors.as[PensionSchemeAdministrator](PensionSchemeAdministrator.apiReads)

        result.numberOfDirectorOrPartners.value.isChanged.value mustBe true
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

      "We have a director" in {
        val directors = JsArray(Seq(testDirectorOrPartner("director"), testDirectorOrPartner("director")))
        val pensionSchemeAdministrator = input + ("directors" -> directors)
        val result = Json.fromJson[PensionSchemeAdministrator](pensionSchemeAdministrator)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.directorOrPartnerDetail.value.head.sequenceId mustBe directorOrPartnerSample("director").sequenceId
      }

      "We have two directors one of which is deleted" in {
        val deletedDirector = testDirectorOrPartner("director") ++ Json.obj("directorDetails" -> Json.obj("firstName" -> JsString("Joe"),
          "lastName" -> JsString("Bloggs"),
          "dateOfBirth" -> JsString("2019-01-31"),
          "isDeleted" -> JsBoolean(true)))

        val directors = JsArray(Seq(testDirectorOrPartner("director"), deletedDirector))
        val pensionSchemeAdministrator = input + ("directors" -> directors)
        val result = Json.fromJson[PensionSchemeAdministrator](pensionSchemeAdministrator)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.directorOrPartnerDetail.value.size mustEqual 1
        result.directorOrPartnerDetail.value.head.lastName mustEqual "Doe"
      }

      "We have organisation details" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.organisationDetail.value.crnNumber mustBe companySample.crnNumber
      }

      "We have individual details" in {
        val inputWithIndividualDetails = input + ("individualDetails" -> Json.obj("firstName" -> JsString("John"),
          "lastName" -> JsString("Doe"),
          "middleName" -> JsString("Does Does"),
          "dateOfBirth" -> JsString("2019-01-31"))) + ("registrationInfo" -> Json.obj("legalStatus" -> "Individual",
          "sapNumber" -> "NumberTest",
          "noIdentifier" -> JsBoolean(true),
          "customerType" -> "TestCustomer",
          "idType" -> JsString("TestId"),
          "idNumber" -> JsString("TestIdNumber"))) - "businessDetails" - "companyDetails" - "companyRegistrationNumber"

        val result = Json.fromJson[PensionSchemeAdministrator](inputWithIndividualDetails)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.individualDetail.value.dateOfBirth mustBe individualSample.dateOfBirth
      }

      "We have organisation details but no individual details" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.individualDetail mustBe None
      }

      "We have individual details but no organisation details" in {
        val inputWithIndividualDetails = input + ("individualDetails" -> Json.obj("firstName" -> JsString("John"),
          "lastName" -> JsString("Doe"),
          "middleName" -> JsString("Does Does"),
          "dateOfBirth" -> JsString("2019-01-31"))) + ("registrationInfo" -> Json.obj("legalStatus" -> "Individual",
          "sapNumber" -> "NumberTest",
          "noIdentifier" -> JsBoolean(true),
          "customerType" -> "TestCustomer",
          "idType" -> JsString("TestId"),
          "idNumber" -> JsString("TestIdNumber"))) - "businessDetails" - "companyDetails" - "companyRegistrationNumber"

        val result = Json.fromJson[PensionSchemeAdministrator](inputWithIndividualDetails)(PensionSchemeAdministrator.apiReads).asOpt.value

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
        renameElement(input, "companyContactAddress", "individualContactAddress").fold(
          invalid => throw JsResultException(invalid),
          json => {
            val result = Json.fromJson[PensionSchemeAdministrator](json)(PensionSchemeAdministrator.apiReads).asOpt.value

            result.correspondenceAddressDetail.asInstanceOf[UkAddress] mustBe ukAddressSample
          }
        )
      }

      "We have an individual previous address" in {
        val expectedIndividualPreviousAddress = previousAddressDetailsSample.copy(isPreviousAddressLast12Month = false, None)
        val individualPreviousAddress = "individualAddressYears" -> JsString("over_a_year")
        val result =
          Json.fromJson[PensionSchemeAdministrator](input + individualPreviousAddress - "companyAddressYears")(PensionSchemeAdministrator.apiReads).asOpt.value

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
