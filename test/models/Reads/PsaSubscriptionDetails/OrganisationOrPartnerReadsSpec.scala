/*
 * Copyright 2022 HM Revenue & Customs
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

import models.OrganisationOrPartner
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec


class OrganisationOrPartnerReadsSpec extends AnyWordSpec with Matchers with OptionValues with PsaSubscriptionDetailsGenerators {
  "A valid payload with Organisation or Partner Details" should {
    "validate to a Organisation or Partner Details object" when {
      "we have a name" in {
        forAll(orgOrPartnerDetailsGenerator){
          orgOrPartner => orgOrPartner.as[OrganisationOrPartner].name mustBe (orgOrPartner \ "name").as[String]
        }
      }

      "we have an optional crnNumber" in {
        forAll(orgOrPartnerDetailsGenerator){
          orgOrPartner => orgOrPartner.as[OrganisationOrPartner].crn mustBe (orgOrPartner \ "crnNumber").asOpt[String]
        }
      }

      "we have an optional vatRegistrationNumber" in {
        forAll(orgOrPartnerDetailsGenerator){
          orgOrPartner => orgOrPartner.as[OrganisationOrPartner].vatRegistration mustBe (orgOrPartner \ "vatRegistrationNumber").asOpt[String]
        }
      }

      "we have an optional payeReference" in {
        forAll(orgOrPartnerDetailsGenerator){
          orgOrPartner => orgOrPartner.as[OrganisationOrPartner].paye mustBe (orgOrPartner \ "payeReference").asOpt[String]
        }
      }
    }
  }
}