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

import play.api.libs.functional.syntax._
import play.api.libs.json
import play.api.libs.json._

case class DirectorOrPartnerDetailTypeItem(sequenceId: String, entityType: String, title: Option[String] = None,
                                           firstName: String, middleName: Option[String] = None, lastName: String,
                                           dateOfBirth: LocalDate, referenceOrNino: Option[String] = None,
                                           noNinoReason: Option[String] = None, utr: Option[String] = None,
                                           noUtrReason: Option[String] = None,
                                           correspondenceCommonDetail: CorrespondenceCommonDetail,
                                           previousAddressDetail: PreviousAddressDetails)

object DirectorOrPartnerDetailTypeItem {
  implicit val formats = Json.format[DirectorOrPartnerDetailTypeItem]

  val psaSubmissionWrites: Writes[DirectorOrPartnerDetailTypeItem] = (
    (JsPath \ "sequenceId").write[String] and
      (JsPath \ "entityType").write[String] and
      (JsPath \ "title").writeNullable[String] and
      (JsPath \ "firstName").write[String] and
      (JsPath \ "middleName").writeNullable[String] and
      (JsPath \ "lastName").write[String] and
      (JsPath \ "dateOfBirth").write[LocalDate] and
      (JsPath \ "referenceOrNino").writeNullable[String] and
      (JsPath \ "noNinoReason").writeNullable[String] and
      (JsPath \ "utr").writeNullable[String] and
      (JsPath \ "noUtrReason").writeNullable[String] and
      (JsPath \ "correspondenceCommonDetail").write[CorrespondenceCommonDetail] and
      (JsPath \ "previousAddressDetail").write(PreviousAddressDetails.psaSubmissionWrites)
    ) (directorOrPartner => (directorOrPartner.sequenceId,
    directorOrPartner.entityType,
    directorOrPartner.title,
    directorOrPartner.firstName,
    directorOrPartner.middleName,
    directorOrPartner.lastName,
    directorOrPartner.dateOfBirth,
    directorOrPartner.referenceOrNino,
    directorOrPartner.noNinoReason,
    directorOrPartner.utr,
    directorOrPartner.noUtrReason,
    directorOrPartner.correspondenceCommonDetail,
    directorOrPartner.previousAddressDetail))

  def apiReads(personType: String): Reads[List[DirectorOrPartnerDetailTypeItem]] = json.Reads {
    json =>
      json.validate[Seq[JsValue]].flatMap(elements => {
        val directorsOrPartners: Seq[JsResult[DirectorOrPartnerDetailTypeItem]] =
          filterDeletedDirectorOrPartner(personType, elements).zipWithIndex.map { directorOrPartner =>
            val (directorOrPartnerDetails, index) = directorOrPartner
            directorOrPartnerDetails.validate[DirectorOrPartnerDetailTypeItem](DirectorOrPartnerDetailTypeItem.directorOrPartnerReads(index, personType))
          }
        directorsOrPartners.foldLeft[JsResult[List[DirectorOrPartnerDetailTypeItem]]](JsSuccess(List.empty)) {
          (directors, currentDirector) => {
            for {
              sequenceOfDirectors <- directors
              director <- currentDirector
            } yield sequenceOfDirectors :+ director
          }
        }
      })
  }

  private def filterDeletedDirectorOrPartner(personType: String, jsValueSeq: Seq[JsValue]): Seq[JsValue] = {
    jsValueSeq.filterNot { json =>
      (json \ s"${personType}Details" \ "isDeleted").validate[Boolean] match {
        case JsSuccess(isDeleted, _) => isDeleted
        case _ => false
      }
    }
  }

  def directorOrPartnerReferenceReads(referenceFlag: String, referenceName: String): Reads[(Option[String], Option[String])] = (
    (JsPath \ referenceName).readNullable[String] and
      (JsPath \ "reason").readNullable[String]
    ) ((referenceNumber, reason) => (referenceNumber, reason))

  def directorOrPartnerReads(index: Int, personType: String): Reads[DirectorOrPartnerDetailTypeItem] = (
    JsPath.read(IndividualDetailType.apiReads(personType)) and
      (JsPath \ s"${personType}Nino").readNullable(directorOrPartnerReferenceReads("hasNino", "nino")) and
      (JsPath \ s"${personType}Utr").readNullable(directorOrPartnerReferenceReads("hasUtr", "utr")) and
      JsPath.read(PreviousAddressDetails.apiReads(personType)) and
      JsPath.read(CorrespondenceCommonDetail.apiReads(personType))
    ) (
    (directorOrPartnerPersonalDetails, ninoDetails, utrDetails, previousAddress, addressCommonDetails) =>
      DirectorOrPartnerDetailTypeItem(sequenceId = f"${index}%03d",
        entityType = personType.capitalize,
        title = None,
        firstName = directorOrPartnerPersonalDetails.firstName,
        middleName = directorOrPartnerPersonalDetails.middleName,
        lastName = directorOrPartnerPersonalDetails.lastName,
        dateOfBirth = directorOrPartnerPersonalDetails.dateOfBirth,
        referenceOrNino = ninoDetails.flatMap(_._1),
        noNinoReason = ninoDetails.flatMap(_._2),
        utr = utrDetails.flatMap(_._1),
        noUtrReason = utrDetails.flatMap(_._2),
        correspondenceCommonDetail = addressCommonDetails,
        previousAddressDetail = previousAddress))
}
