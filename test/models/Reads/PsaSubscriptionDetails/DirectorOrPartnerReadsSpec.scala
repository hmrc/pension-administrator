/*
 * Copyright 2021 HM Revenue & Customs
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

import models.{CorrespondenceAddress, CorrespondenceDetails, DirectorOrPartner}
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._


class DirectorOrPartnerReadsSpec extends AnyWordSpec with Matchers with OptionValues with PsaSubscriptionDetailsGenerators {
  "A payload containing details for a Director or a Partner" should {
    "parse correctly to a PsaDirectorOrPartnerDetails object" when {
      "we have an entity type" in {
        forAll(psaDirectorOrPartnerDetailsGenerator){
          directorOrPartner => directorOrPartner.as[DirectorOrPartner].isDirectorOrPartner mustBe (directorOrPartner \ "entityType").as[String]
        }
      }

      "we have an optional title" in {
        forAll(psaDirectorOrPartnerDetailsGenerator){
          directorOrPartner => directorOrPartner.as[DirectorOrPartner].title mustBe (directorOrPartner \ "title").asOpt[String]
        }
      }

      "we have a first name" in {
        forAll(psaDirectorOrPartnerDetailsGenerator){
          directorOrPartner => directorOrPartner.as[DirectorOrPartner].firstName mustBe (directorOrPartner \ "firstName").as[String]
        }
      }

      "we have an optional middle name" in {
        forAll(psaDirectorOrPartnerDetailsGenerator){
          directorOrPartner => directorOrPartner.as[DirectorOrPartner].middleName mustBe (directorOrPartner \ "middleName").asOpt[String]
        }
      }

      "we have a surname" in {
        forAll(psaDirectorOrPartnerDetailsGenerator){
          directorOrPartner => directorOrPartner.as[DirectorOrPartner].lastName mustBe (directorOrPartner \ "lastName").as[String]
        }
      }

      "we have a dob" in {
        forAll(psaDirectorOrPartnerDetailsGenerator){
          directorOrPartner => directorOrPartner.as[DirectorOrPartner].dateOfBirth.toString() mustBe (directorOrPartner \ "dateOfBirth").as[String]
        }
      }

      "we have an optional nino" in {
        forAll(psaDirectorOrPartnerDetailsGenerator){
          directorOrPartner => directorOrPartner.as[DirectorOrPartner].nino mustBe (directorOrPartner \ "nino").asOpt[String]
        }
      }

      "we have an optional utr" in {
        forAll(psaDirectorOrPartnerDetailsGenerator){
          directorOrPartner => directorOrPartner.as[DirectorOrPartner].utr mustBe (directorOrPartner \ "utr").asOpt[String]
        }
      }

      "we have a flag to say whether if they have been in the same previous address in last 12 months" in {
        forAll(psaDirectorOrPartnerDetailsGenerator){
          directorOrPartner => directorOrPartner.as[DirectorOrPartner].isSameAddressForLast12Months mustBe (directorOrPartner \ "previousAddressDetails" \ "isPreviousAddressLast12Month").as[Boolean]
        }
      }

      "we have an optional previous address" in {
        forAll(psaDirectorOrPartnerDetailsGenerator){
          directorOrPartner => directorOrPartner.as[DirectorOrPartner].previousAddress mustBe (directorOrPartner \ "previousAddressDetails" \ "previousAddress").asOpt[CorrespondenceAddress]
        }
      }

      "we have an optional correspondence common details" in {
        forAll(psaDirectorOrPartnerDetailsGenerator){
          directorOrPartner => directorOrPartner.as[DirectorOrPartner].correspondenceDetails mustBe (directorOrPartner \ "correspondenceCommonDetails").asOpt[CorrespondenceDetails]
        }
      }
    }
  }
}


