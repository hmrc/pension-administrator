/*
 * Copyright 2018 HM Revenue & Customs
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

import java.time.LocalDate

import models.enumeration.RegistrationLegalStatus
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, JsValue, Json, OFormat, Reads, Writes}

trait PSADetail

object PSADetail {
  val companyReads: Reads[PSADetail] = JsPath.read[OrganisationDetailType](OrganisationDetailType.CompanyApiReads).map(c => c.asInstanceOf[PSADetail])
  val individualDetailsReads: Reads[PSADetail] = JsPath.read[IndividualDetailType](IndividualDetailType.apiReads("individual")).map(
    c => c.asInstanceOf[PSADetail])
  val partnershipReads: Reads[PSADetail] = JsPath.read[OrganisationDetailType](
    OrganisationDetailType.partnershipApiReads).map(c => c.asInstanceOf[PSADetail])

  val apiReads: Reads[PSADetail] = companyReads orElse individualDetailsReads orElse partnershipReads
}

case class IndividualDetailType(title: Option[String] = None, firstName: String, middleName: Option[String] = None,
                                lastName: String, dateOfBirth: LocalDate) extends PSADetail

object IndividualDetailType {
  implicit val formats: OFormat[IndividualDetailType] = Json.format[IndividualDetailType]

  def apiReads(individualType: String): Reads[IndividualDetailType] = (
    (JsPath \ s"${individualType}Details" \ "firstName").read[String] and
      (JsPath \ s"${individualType}Details" \ "lastName").read[String] and
      (JsPath \ s"${individualType}Details" \ "middleName").readNullable[String] and
      ((JsPath \ "individualDateOfBirth").read[LocalDate] orElse (JsPath \ s"${individualType}Details" \ "dateOfBirth").read[LocalDate])
    ) ((name, lastName, middleName, dateOfBirth) => IndividualDetailType(None, name, middleName, lastName, dateOfBirth))
}

case class PensionSchemeAdministratorIdentifierStatusType(isExistingPensionSchemaAdministrator: Boolean,
                                                          existingPensionSchemaAdministratorReference: Option[String] = None)

object PensionSchemeAdministratorIdentifierStatusType {
  implicit val formats: OFormat[PensionSchemeAdministratorIdentifierStatusType] = Json.format[PensionSchemeAdministratorIdentifierStatusType]

  val apiReads: Reads[PensionSchemeAdministratorIdentifierStatusType] = (
    (JsPath \ "isExistingPSA").read[Boolean] and
      (JsPath \ "existingPSAId").readNullable[String]
    ) ((isExistingPSA, existingPSAId) => PensionSchemeAdministratorIdentifierStatusType(isExistingPSA, existingPSAId))
}

case class NumberOfDirectorOrPartnersType(isMorethanTenDirectors: Option[Boolean] = None,
                                          isMorethanTenPartners: Option[Boolean] = None)

object NumberOfDirectorOrPartnersType {
  implicit val formats: OFormat[NumberOfDirectorOrPartnersType] = Json.format[NumberOfDirectorOrPartnersType]
}

case class CorrespondenceCommonDetail(addressDetail: Address, contactDetail: ContactDetails)

object CorrespondenceCommonDetail {
  implicit val formats: OFormat[CorrespondenceCommonDetail] = Json.format[CorrespondenceCommonDetail]

  def apiReads(personType: String): Reads[CorrespondenceCommonDetail] = (
    (JsPath \ s"${personType}ContactDetails").read(ContactDetails.apiReads) and
      (JsPath \ s"${personType}Address").read[Address]
    ) ((contactDetails, address) => CorrespondenceCommonDetail(address, contactDetails))
}

case class PensionSchemeAdministrator(customerType: String, legalStatus: String, idType: Option[String] = None,
                                      idNumber: Option[String] = None, sapNumber: String, noIdentifier: Boolean,
                                      organisationDetail: Option[OrganisationDetailType] = None,
                                      individualDetail: Option[IndividualDetailType] = None,
                                      pensionSchemeAdministratoridentifierStatus: PensionSchemeAdministratorIdentifierStatusType,
                                      correspondenceAddressDetail: Address,
                                      correspondenceContactDetail: ContactDetails,
                                      previousAddressDetail: PreviousAddressDetails,
                                      numberOfDirectorOrPartners: Option[NumberOfDirectorOrPartnersType] = None,
                                      directorOrPartnerDetail: Option[List[DirectorOrPartnerDetailTypeItem]] = None,
                                      declaration: PensionSchemeAdministratorDeclarationType)

object PensionSchemeAdministrator {
  implicit val formats: OFormat[PensionSchemeAdministrator] = Json.format[PensionSchemeAdministrator]

  val psaSubmissionWrites: Writes[PensionSchemeAdministrator] = (
    (JsPath \ "customerType").write[String] and
      (JsPath \ "legalStatus").write[String] and
      (JsPath \ "idType").writeNullable[String] and
      (JsPath \ "idNumber").writeNullable[String] and
      (JsPath \ "sapNumber").write[String] and
      (JsPath \ "noIdentifier").write[Boolean] and
      (JsPath \ "organisationDetail").writeNullable[OrganisationDetailType] and
      (JsPath \ "individualDetail").writeNullable[IndividualDetailType] and
      (JsPath \ "pensionSchemeAdministratoridentifierStatus").write[PensionSchemeAdministratorIdentifierStatusType] and
      (JsPath \ "correspondenceAddressDetail").write[Address] and
      (JsPath \ "correspondenceContactDetail").write[ContactDetails] and
      (JsPath \ "previousAddressDetail").write(PreviousAddressDetails.psaSubmissionWrites) and
      (JsPath \ "numberOfDirectorOrPartners").writeNullable[NumberOfDirectorOrPartnersType] and
      (JsPath \ "directorOrPartnerDetail").writeNullable[List[JsValue]] and
      (JsPath \ "declaration").write[PensionSchemeAdministratorDeclarationType]
    ) (psaSubmission => (psaSubmission.customerType,
    psaSubmission.legalStatus,
    psaSubmission.idType,
    psaSubmission.idNumber,
    psaSubmission.sapNumber,
    psaSubmission.noIdentifier,
    psaSubmission.organisationDetail,
    psaSubmission.individualDetail,
    psaSubmission.pensionSchemeAdministratoridentifierStatus,
    psaSubmission.correspondenceAddressDetail,
    psaSubmission.correspondenceContactDetail,
    psaSubmission.previousAddressDetail,
    psaSubmission.numberOfDirectorOrPartners,
    psaSubmission.directorOrPartnerDetail.map(directors => directors.map(director =>
      Json.toJson(director)(DirectorOrPartnerDetailTypeItem.psaSubmissionWrites))),
    psaSubmission.declaration))

  val registrationInfoReads: Reads[(String, String, Boolean, String, Option[String], Option[String])] = (
    (JsPath \ "legalStatus").read[String] and
      (JsPath \ "sapNumber").read[String] and
      (JsPath \ "noIdentifier").read[Boolean] and
      (JsPath \ "customerType").read[String] and
      (JsPath \ "idType").readNullable[String] and
      (JsPath \ "idNumber").readNullable[String]
    ) ((legalStatus, sapNumber, noIdentifier, customerType, idType, idNumber) => (legalStatus, sapNumber, noIdentifier, customerType, idType, idNumber))

  private val contactDetailsReads: Reads[ContactDetails] = {
    ((JsPath \ "contactDetails").read(ContactDetails.apiReads) orElse (JsPath \ "individualContactDetails").read(ContactDetails.apiReads)
      orElse (JsPath \ "partnershipContactDetails").read(ContactDetails.apiReads))
  }

  private val previousAddressReads: Reads[PreviousAddressDetails] = {
    (JsPath.read(PreviousAddressDetails.apiReads("company"))
      orElse JsPath.read(PreviousAddressDetails.apiReads("individual"))
      orElse JsPath.read(PreviousAddressDetails.apiReads("partnership")))
  }

  private val contactAddressReads: Reads[Address] = {
    (JsPath \ "companyContactAddress").read[Address] orElse
      (JsPath \ "individualContactAddress").read[Address] orElse
      (JsPath \ "partnershipContactAddress").read[Address]
  }

  private val organisationLegalStatus = Seq("Limited Company", "Partnership")

  private def numberOfDirectorsOrPartners(isThereMoreThanTenDirectors: Option[Boolean],
                                          isThereMoreThanTenPartners: Option[Boolean]): Option[NumberOfDirectorOrPartnersType] =
    (isThereMoreThanTenDirectors, isThereMoreThanTenPartners) match {
      case (None, None) => None
      case _ => Some(NumberOfDirectorOrPartnersType(isThereMoreThanTenDirectors, isThereMoreThanTenPartners))
    }

  private def directorOrPartnerDetail(legalStatus: String, directorsOrPartners: Seq[Option[scala.List[DirectorOrPartnerDetailTypeItem]]]) = {
    legalStatus match {
      case RegistrationLegalStatus.Company.name => directorsOrPartners.head
      case RegistrationLegalStatus.Partnership.name => directorsOrPartners(1)
      case _ => None
    }
  }

  val apiReads: Reads[PensionSchemeAdministrator] = (
    (JsPath \ "registrationInfo").read(registrationInfoReads) and
      (JsPath \ "moreThanTenDirectors").readNullable[Boolean] and
      (JsPath \ "moreThanTenPartners").readNullable[Boolean] and
      contactDetailsReads and
      previousAddressReads and
      contactAddressReads and
      (JsPath \ "directors").readNullable(DirectorOrPartnerDetailTypeItem.apiReads("director")) and
      (JsPath \ "partners").readNullable(DirectorOrPartnerDetailTypeItem.apiReads("partner")) and
      JsPath.read(PSADetail.apiReads) and
      (JsPath \ "existingPSA").read(PensionSchemeAdministratorIdentifierStatusType.apiReads) and
      JsPath.read(PensionSchemeAdministratorDeclarationType.apiReads)
    ) ((registrationInfo,
        isThereMoreThanTenDirectors,
        isThereMoreThanTenPartners,
        contactDetails,
        previousAddressDetails,
        correspondenceAddress,
        directors,
        partners,
        transactionDetails,
        isExistingPSA,
        declaration) => {

    PensionSchemeAdministrator(
      customerType = registrationInfo._4,
      legalStatus = registrationInfo._1,
      sapNumber = registrationInfo._2,
      noIdentifier = registrationInfo._3,
      idType = registrationInfo._5,
      idNumber = registrationInfo._6,
      numberOfDirectorOrPartners = numberOfDirectorsOrPartners(isThereMoreThanTenDirectors, isThereMoreThanTenPartners),
      pensionSchemeAdministratoridentifierStatus = isExistingPSA,
      correspondenceAddressDetail = correspondenceAddress,
      correspondenceContactDetail = contactDetails,
      previousAddressDetail = previousAddressDetails,
      directorOrPartnerDetail = directorOrPartnerDetail(registrationInfo._1, Seq(directors, partners)),
      organisationDetail = if (organisationLegalStatus.contains(registrationInfo._1))
        Some(transactionDetails.asInstanceOf[OrganisationDetailType]) else None,
      individualDetail = if (registrationInfo._1 == RegistrationLegalStatus.Individual.name)
        Some(transactionDetails.asInstanceOf[IndividualDetailType]) else None,
      declaration = declaration)
  }
  )
}
