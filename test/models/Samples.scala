/*
 * Copyright 2025 HM Revenue & Customs
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

package models

import play.api.libs.json.{JsBoolean, JsObject, JsString, Json}

import java.time.LocalDate

trait Samples {

  val ukAddressSampleWithTwoLines: UkAddress = UkAddress("line1", Some("line2"), None, None, "GB", "NE1")
  val nonUkAddressSample: InternationalAddress = InternationalAddress("line1", Some("line2"), Some("line3"), Some("line4"), "IT", Some("NE1"))
  val ukAddressSample: UkAddress = UkAddress("line1", Some("line2"), Some("line3"), Some("line4"), "GB", "NE1")
  val numberOfDirectorOrPartnersSample: NumberOfDirectorOrPartnersType = NumberOfDirectorOrPartnersType(
    isMorethanTenDirectors = Some(true), isMorethanTenPartners = Some(true))
  val previousAddressDetailsSample: PreviousAddressDetails = PreviousAddressDetails(isPreviousAddressLast12Month = false)
  val previousAddressDetailsSampleTwo: PreviousAddressDetails = PreviousAddressDetails(isPreviousAddressLast12Month = true, Some(ukAddressSample))
  val contactDetailsSample: ContactDetails = ContactDetails("07592113", email = "test@test.com")

  val pensionAdvisorDetail: PensionAdvisorDetail = PensionAdvisorDetail(name = "xyz",
    addressDetail = nonUkAddressSample,
    contactDetail = contactDetailsSample)

  val declarationSample: PensionSchemeAdministratorDeclarationType = PensionSchemeAdministratorDeclarationType(box1 = true,
    box2 = true, box3 = true, box4 = true, Some(true), None, box7 = true, None)
  val declarationSampleTwo: PensionSchemeAdministratorDeclarationType = PensionSchemeAdministratorDeclarationType(box1 = true,
    box2 = true, box3 = true, box4 = true, Some(true), None, box7 = true, Some(pensionAdvisorDetail))
  val pensionSchemeAdministratorSample: PensionSchemeAdministrator = PensionSchemeAdministrator(customerType = "TestCustomer",
    legalStatus = "Limited Company",
    sapNumber = "NumberTest",
    noIdentifier = true,
    idType = Some("TestId"),
    idNumber = Some("TestIdNumber"),
    organisationDetail = None,
    individualDetail = None,
    pensionSchemeAdministratorIdentifierStatus = PensionSchemeAdministratorIdentifierStatusType(isExistingPensionSchemaAdministrator = false),
    correspondenceAddressDetail = ukAddressSample,
    correspondenceContactDetail = contactDetailsSample,
    previousAddressDetail = previousAddressDetailsSample,
    numberOfDirectorOrPartners = Some(numberOfDirectorOrPartnersSample),
    directorOrPartnerDetail = None, declaration = declarationSample)

  val pensionSchemeAdministratorSamplePartnership: PensionSchemeAdministrator = PensionSchemeAdministrator(customerType = "TestCustomer",
    legalStatus = "Partnership",
    sapNumber = "NumberTest",
    noIdentifier = true,
    idType = Some("TestId"),
    idNumber = Some("TestIdNumber"),
    organisationDetail = None,
    individualDetail = None,
    pensionSchemeAdministratorIdentifierStatus = PensionSchemeAdministratorIdentifierStatusType(isExistingPensionSchemaAdministrator = false),
    correspondenceAddressDetail = ukAddressSample,
    correspondenceContactDetail = contactDetailsSample,
    previousAddressDetail = previousAddressDetailsSample,
    numberOfDirectorOrPartners = Some(numberOfDirectorOrPartnersSample),
    directorOrPartnerDetail = None, declaration = declarationSample)

  val pensionSchemeAdministratorSampleIndividual: PensionSchemeAdministrator = PensionSchemeAdministrator(customerType = "TestCustomer",
    legalStatus = "Individual",
    sapNumber = "NumberTest",
    noIdentifier = true,
    idType = Some("TestId"),
    idNumber = Some("TestIdNumber"),
    organisationDetail = None,
    individualDetail = Some(IndividualDetailType(
      title = None, firstName = "John", middleName = Some("Does Does"),
      lastName = "Doe", dateOfBirth = LocalDate.parse("2019-01-31"))),
    pensionSchemeAdministratorIdentifierStatus = PensionSchemeAdministratorIdentifierStatusType(isExistingPensionSchemaAdministrator = false),
    correspondenceAddressDetail = ukAddressSample,
    correspondenceContactDetail = contactDetailsSample,
    previousAddressDetail = previousAddressDetailsSample,
    numberOfDirectorOrPartners = None,
    directorOrPartnerDetail = None, declaration = declarationSample)

  val correspondenceCommonDetails: CorrespondenceCommonDetail = CorrespondenceCommonDetail(nonUkAddressSample, contactDetailsSample)

  def pensionSchemeAdministratorSampleTwo(personType: String): PensionSchemeAdministrator = {
    val item1 = directorOrPartnerSample(personType).copy(previousAddressDetail = previousAddressDetailsSampleTwo)
    val item2 = item1.copy(firstName = "Paul", middleName = None, lastName = "Stephens", previousAddressDetail = previousAddressDetailsSampleTwo)
    
    pensionSchemeAdministratorSample.copy(
      directorOrPartnerDetail = Some(List(item1, item2)
      ),
      declaration = declarationSampleTwo,
      individualDetail = Some(individualSample))
  }

  def directorOrPartnerSample(personType: String): DirectorOrPartnerDetailTypeItem = DirectorOrPartnerDetailTypeItem(sequenceId = "000",
    entityType = personType.capitalize,
    title = None,
    firstName = "John",
    middleName = Some("Does Does"),
    lastName = "Doe",
    dateOfBirth = LocalDate.parse("2019-01-31"),
    referenceOrNino = Some("SL211111A"),
    noNinoReason = Some("he can't find it"),
    utr = Some("123456789K"),
    noUtrReason = Some("he can't find it"),
    correspondenceCommonDetail = correspondenceCommonDetails,
    previousAddressDetail = PreviousAddressDetails(isPreviousAddressLast12Month = false))

  val companySample: OrganisationDetailType = OrganisationDetailType("Test Name", vatRegistrationNumber = Some("VAT11111"),
    payeReference = Some("PAYE11111"), crnNumber = Some("CRN11111"))

  val partnershipSample: OrganisationDetailType = OrganisationDetailType("Test Partnership", crnNumber = Some("CRN11111"))

  val individualSample: IndividualDetailType = IndividualDetailType(firstName = "John", middleName = Some("Does Does"),
    lastName = "Doe", dateOfBirth = LocalDate.parse("2019-01-31"))

  val pensionAdviserSample: PensionAdvisorDetail = PensionAdvisorDetail("John", ukAddressSample, contactDetailsSample)

  def testDirectorOrPartner(personType: String): JsObject = Json.obj(s"${personType}Details" -> Json.obj(
    "firstName" -> JsString("John"),
    "lastName" -> JsString("Doe"),
    "isDeleted" -> JsBoolean(false)
  ),
    "dateOfBirth" -> JsString("2019-01-31"),
    s"${personType}Nino" -> Json.obj("hasNino" -> JsBoolean(true), "nino" -> JsString("SL211111A")),
    s"${personType}Utr" -> Json.obj("hasUtr" -> JsBoolean(true), "utr" -> JsString("123456789")),
    s"${personType}AddressYears" -> JsString("over_a_year")) +
    (s"${personType}ContactDetails" -> Json.obj("email" -> "test@test.com", "phone" -> "07592113")) +
    (s"${personType}Address" -> Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"),
      "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"), "postcode" -> JsString("NE1"), "country" -> JsString("IT")))
}
