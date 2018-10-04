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

package models.Reads.PsaSubscriptionDetails

import models.PsaSubscription.CorrespondenceDetails
import models.{CorrespondenceAddress, CorrespondenceCommonDetail, Samples}
import org.joda.time.LocalDate
import org.scalacheck.Gen
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._
import play.api.libs.functional.syntax._

class PsaDirectorOrPartnerDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with PsaSubscriptionDetailsGenerators {
  "A payload containing details for a Director or a Partner" should {
    "parse correctly to a PsaDirectorOrPartnerDetails object" when {
      val output = psaDirectorOrPartnerDetailsGenerator.as[PsaDirectorOrPartnerDetails]

      "we have an entity type" in {
        output.isDirectorOrPartner mustBe (psaDirectorOrPartnerDetailsGenerator \ "entityType").as[String]
      }

      "we have an optional title" in {
        output.title mustBe (psaDirectorOrPartnerDetailsGenerator \ "title").asOpt[String]
      }

      "we have a first name" in {
        output.firstName mustBe (psaDirectorOrPartnerDetailsGenerator \ "firstName").as[String]
      }

      "we have an optional middle name" in {
        output.middleName mustBe (psaDirectorOrPartnerDetailsGenerator \ "middleName").asOpt[String]
      }

      "we have a surname" in {
        output.lastName mustBe (psaDirectorOrPartnerDetailsGenerator \ "lastName").as[String]
      }

      "we have a dob" in {
        output.dateOfBirth.toString() mustBe (psaDirectorOrPartnerDetailsGenerator \ "dateOfBirth").as[String]
      }

      "we have an optional nino" in {
        output.nino mustBe (psaDirectorOrPartnerDetailsGenerator \ "nino").asOpt[String]
      }

      "we have an optional utr" in {
        output.utr mustBe (psaDirectorOrPartnerDetailsGenerator \ "utr").asOpt[String]
      }

      "we have a flag to say whether if they have been in the same prevoius address in last 12 months" in {
        output.isSameAddressForLast12Months mustBe (psaDirectorOrPartnerDetailsGenerator \ "previousAddressDetails" \ "isPreviousAddressLast12Month").as[Boolean]
      }

      "we have an optional previous address" in {
        output.previousAddress mustBe (psaDirectorOrPartnerDetailsGenerator \ "previousAddressDetails" \ "previousAddress").asOpt[CorrespondenceAddress]
      }

      "we have an optional correspondence common details" in {
        output.correspondenceDetails mustBe (psaDirectorOrPartnerDetailsGenerator \ "correspondenceCommonDetails").asOpt[CorrespondenceDetails]
      }
    }
  }
}



case class PsaDirectorOrPartnerDetails(isDirectorOrPartner: String,
                                       title: Option[String],
                                       firstName: String,
                                       middleName: Option[String],
                                       lastName: String,
                                       dateOfBirth: LocalDate,
                                       nino: Option[String],
                                       utr: Option[String],
                                       isSameAddressForLast12Months: Boolean,
                                       previousAddress: Option[CorrespondenceAddress],
                                       correspondenceDetails: Option[CorrespondenceDetails])

object PsaDirectorOrPartnerDetails {
  implicit val writes : Writes[PsaDirectorOrPartnerDetails] = Json.writes[PsaDirectorOrPartnerDetails]
  implicit val reads : Reads[PsaDirectorOrPartnerDetails] = (
    (JsPath \ "entityType").read[String] and
      (JsPath \ "title").readNullable[String] and
      (JsPath \ "firstName").read[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "dateOfBirth").read[LocalDate] and
      (JsPath \ "nino").readNullable[String] and
      (JsPath \ "utr").readNullable[String] and
      (JsPath \ "previousAddressDetails" \ "isPreviousAddressLast12Month").read[Boolean] and
      (JsPath \ "previousAddressDetails" \ "previousAddress").readNullable[CorrespondenceAddress] and
      (JsPath \ "correspondenceCommonDetails").readNullable[CorrespondenceDetails]
  )((entityType,title,name,middleName,surname,dob,nino,utr,isSameAddress,previousAddress,correspondence) =>
    PsaDirectorOrPartnerDetails(entityType,title,name,middleName,surname,dob,nino,utr,isSameAddress,previousAddress,correspondence))
}


