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

package models.Reads

import models.{OrganisationDetailType, Samples}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsBoolean, JsString, Json}

class OrganisationDetailTypeReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {

  import OrganisationDetailTypeReadsSpec._

  "A JSON Payload containing organisation detials" must {

    Seq(("Company", companyDetails), ("Partnership", partnershipDetails)).foreach { entityType =>
      val (orgType, orgData) = entityType
      s"Map correctly to a $orgType OrganisationDetailType model" when {
        val apiReads = if (orgType == "Company") OrganisationDetailType.CompanyApiReads else OrganisationDetailType.partnershipApiReads

        "We have a name" in {
          val result = orgData.as[OrganisationDetailType](apiReads)

          result.name mustBe companySample.name
        }

        "We have VAT registration number" in {
          val result = orgData.as[OrganisationDetailType](apiReads)

          result.vatRegistrationNumber mustBe companySample.vatRegistrationNumber
        }

        "We have a PAYE employer reference number" in {
          val result = orgData.as[OrganisationDetailType](apiReads)

          result.payeReference mustBe companySample.payeReference
        }

        "We have a Company Registration Number" in {
          val result = companyDetails.as[OrganisationDetailType](OrganisationDetailType.CompanyApiReads)

          result.crnNumber mustBe companySample.crnNumber
        }

        "We have no company details" in {
          val orgDetailsWithNoCompanyDetails = companyDetails - "companyDetails"

          val result = orgDetailsWithNoCompanyDetails.as[OrganisationDetailType](OrganisationDetailType.CompanyApiReads)

          result.vatRegistrationNumber mustBe None
        }

        "We have no VAT registration number" in {
          val companyDetails = orgDetailWithoutVat(orgType)

          val result = companyDetails.as[OrganisationDetailType](apiReads)

          result.vatRegistrationNumber mustBe None
        }

        "We have no payeEmployerReferenceNumber" in {
          val companyDetails = orgDetailWithoutPaye(orgType)

          val result = companyDetails.as[OrganisationDetailType](apiReads)

          result.payeReference mustBe None
        }
      }
    }
  }
}

object OrganisationDetailTypeReadsSpec {
  private val companyDetails = Json.obj("companyDetails" -> Json.obj("vatRegistrationNumber" -> JsString("VAT11111"),
    "payeEmployerReferenceNumber" -> JsString("PAYE11111")),
    "companyRegistrationNumber" -> JsString("CRN11111"), "businessDetails" -> Json.obj("companyName" -> JsString("Company Test")))

  private def orgDetailWithoutVat(entityType: String) = {
    if (entityType == "Partnership") {
      partnershipDetails + ("partnershipVat" -> Json.obj("hasVat" -> JsBoolean(false)))
    } else {
      companyDetails + ("companyDetails" -> Json.obj("payeEmployerReferenceNumber" -> JsString("PAYE11111")))
    }
  }

  private def orgDetailWithoutPaye(entityType: String) = {
    if (entityType == "Partnership") {
      partnershipDetails + ("partnershipPaye" -> Json.obj("hasPaye" -> JsBoolean(false)))
    } else {
      companyDetails + ("companyDetails" -> Json.obj("vatRegistrationNumber" -> JsString("VAT11111")))
    }
  }

  private val partnershipDetails = Json.obj("partnershipVat" -> Json.obj("vat" -> JsString("VAT11111"), "hasVat" -> JsBoolean(true)),
    "partnershipPaye" -> Json.obj("paye" -> JsString("PAYE11111"), "hasPaye" -> JsBoolean(true)),
    "partnershipDetails" -> Json.obj("companyName" -> JsString("Company Test")))
}
