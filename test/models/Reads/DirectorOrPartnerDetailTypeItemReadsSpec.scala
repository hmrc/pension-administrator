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

package models.Reads

import models.{InternationalAddress, PreviousAddressDetails, Samples, Reads as _, *}
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*


class DirectorOrPartnerDetailTypeItemReadsSpec extends AnyWordSpec with Matchers with OptionValues with Samples {

  import DirectorOrPartnerDetailTypeItemReadsSpec.*

  "JSON Payload of a Director" should {
    "Map correctly into a DirectorOrPartnerDetailTypeItem" when {

      Seq(("director", directors), ("partner", partners)).foreach { entity =>
        val (personType, personDetails) = entity
        val directorsOrPartners = JsArray(personDetails)
        s"We have $personType user details" when {
          s"We have a list of $personType" in {
            val result = directorsOrPartners.as[List[DirectorOrPartnerDetailTypeItem]](using DirectorOrPartnerDetailTypeItem.apiReads(personType))
            result.head.lastName mustBe directorOrPartnerSample(personType).lastName
          }

          "We have a sequence id" in {
            val result = directorsOrPartners.as[List[DirectorOrPartnerDetailTypeItem]](using DirectorOrPartnerDetailTypeItem.apiReads(personType))
            result.head.sequenceId mustBe directorOrPartnerSample(personType).sequenceId
          }

          "We have 10 directors" in {
            val directorsOrPartners = JsArray(Seq.tabulate(10)(_ => personDetails.head))
            val result = directorsOrPartners.as[List[DirectorOrPartnerDetailTypeItem]](using DirectorOrPartnerDetailTypeItem.apiReads(personType))
            result.last.sequenceId mustBe "009"
          }

          "We have 100 directors" in {
            val directorsOrPartners = JsArray(Seq.tabulate(100)(_ => personDetails.head))
            val result = directorsOrPartners.as[List[DirectorOrPartnerDetailTypeItem]](using DirectorOrPartnerDetailTypeItem.apiReads(personType))
            result.last.sequenceId mustBe "099"
          }

          "We have 101 directors" in {
            val directorsOrPartners = JsArray(Seq.tabulate(101)(_ => personDetails.head))
            val result = directorsOrPartners.as[List[DirectorOrPartnerDetailTypeItem]](using DirectorOrPartnerDetailTypeItem.apiReads(personType))
            result.last.sequenceId mustBe "100"
          }

          "We have individual details" in {
            val result = directorsOrPartners.as[List[DirectorOrPartnerDetailTypeItem]](using DirectorOrPartnerDetailTypeItem.apiReads(personType))
            result.head.firstName mustBe directorOrPartnerSample(personType).firstName
          }
        }

        s"We have $personType NINO details" when {
          s"We have a $personType nino and reason" in {
            val result = directorsOrPartners.as[List[DirectorOrPartnerDetailTypeItem]](using DirectorOrPartnerDetailTypeItem.apiReads(personType))

            result.head.referenceOrNino mustBe directorOrPartnerSample(personType).referenceOrNino
            result.head.noNinoReason mustBe directorOrPartnerSample(personType).noNinoReason
          }

          "We don't have a nino or reason" in {
            val directorsNoNino = directorsOrPartners.value :+ (directorsOrPartners.head.as[JsObject] - "nino" - "noNinoReason")
            val result = JsArray(directorsNoNino).as[List[DirectorOrPartnerDetailTypeItem]](using DirectorOrPartnerDetailTypeItem.apiReads(personType))

            result.last.referenceOrNino mustBe None
            result.last.noNinoReason mustBe None
          }
        }

        s"We have $personType UTR details" when {
          s"We have a $personType utr and reason" in {
            val result = directorsOrPartners.as[List[DirectorOrPartnerDetailTypeItem]](using DirectorOrPartnerDetailTypeItem.apiReads(personType))

            result.head.utr mustBe Some("0123456789")
            result.head.noUtrReason mustBe directorOrPartnerSample(personType).noUtrReason
          }

          "We don't have a utr or reason" in {
            val directorsNoUtr = directorsOrPartners.value :+ (directorsOrPartners.head.as[JsObject] - "utr" - "noUtrReason")
            val result = JsArray(directorsNoUtr).as[List[DirectorOrPartnerDetailTypeItem]](using DirectorOrPartnerDetailTypeItem.apiReads(personType))

            result.last.utr mustBe None
            result.last.noUtrReason mustBe None
          }
        }

        s"We have entity type as $personType" in {
          val result = directorsOrPartners.as[List[DirectorOrPartnerDetailTypeItem]](using DirectorOrPartnerDetailTypeItem.apiReads(personType))

          result.head.entityType mustBe directorOrPartnerSample(personType).entityType
        }

        s"We have a $personType previous address detail" in {
          val directorWithPreviousAddress = directorsOrPartners.value :+ (directorsOrPartners.head.as[JsObject] +
            (s"${personType}AddressYears" -> JsString("under_a_year")) +
            (s"${personType}PreviousAddress" -> Json.obj("addressLine1" -> JsString("line1"),
              "addressLine2" -> JsString("line2"), "country" -> JsString("IT"))))


          val result = JsArray(directorWithPreviousAddress).as[List[DirectorOrPartnerDetailTypeItem]](using DirectorOrPartnerDetailTypeItem.apiReads(personType))
          val expectedDirector = directorOrPartnerSample(personType).copy(previousAddressDetail =
            PreviousAddressDetails(isPreviousAddressLast12Month = true, Some(InternationalAddress("line1", Some("line2"), countryCode = "IT"))))

          result.last.previousAddressDetail mustBe expectedDirector.previousAddressDetail
        }

        s"We have a $personType correspondence common detail" in {
          val directorWithCorrespondenceCommonDetail = directorsOrPartners.value :+ (directorsOrPartners.head.as[JsObject] +
            (s"${personType}ContactDetails" -> Json.obj("email" -> "test@test.com", "phone" -> "07592113")) + (s"${personType}Address" ->
            Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"),
              "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"), "postcode" -> JsString("NE1"), "country" -> JsString("IT"))))

          val result = JsArray(directorWithCorrespondenceCommonDetail).as[List[DirectorOrPartnerDetailTypeItem]](
            DirectorOrPartnerDetailTypeItem.apiReads(personType))
          val expectedDirector = directorOrPartnerSample(personType).copy(correspondenceCommonDetail = correspondenceCommonDetails)

          result.last.correspondenceCommonDetail mustBe expectedDirector.correspondenceCommonDetail
        }
      }
    }
  }
}
object DirectorOrPartnerDetailTypeItemReadsSpec {

  private def directorOrPartner(personType: String): JsObject = Json.obj(
    s"${personType}Details" -> Json.obj("firstName" -> JsString("John"),
    "lastName" -> JsString("Doe")),
    "dateOfBirth" -> JsString("2019-01-31"),
    "nino" -> Json.obj("value" -> JsString("SL211111A")),
    "noNinoReason" -> JsString("he can't find it"),
    "utr" -> Json.obj("value" -> JsString("0123456789")),
    "noUtrReason" -> JsString("he can't find it"),
    s"${personType}Utr" -> Json.obj("hasUtr" -> JsBoolean(true), "utr" -> JsString("0123456789")),
    s"${personType}AddressYears" -> JsString("over_a_year")
  ) + (s"${personType}ContactDetails" -> Json.obj("email" -> "test@test.com", "phone" -> "07592113")) + (s"${personType}Address" ->
    Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"), "country" -> JsString("IT")))

  val directors: Seq[JsObject] = Seq(directorOrPartner("director"), directorOrPartner("director"))
  val partners: Seq[JsObject] = Seq(directorOrPartner("partner"), directorOrPartner("partner"))
}


