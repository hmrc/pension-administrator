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

import models.{OrganisationDetailType, Samples}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsBoolean, JsString, JsValue, Json}

class OrganisationDetailTypeReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {

  import OrganisationDetailTypeReadsSpec._

  "A JSON Payload containing organisation detials" must {

    "Reading optional keys from Json for partnershipPaye " must {

      "We dont have optional parent of VAT registration number" in {

        val partnershipDetailsWithoutOption = Json.obj(
          "partnershipPaye" -> Json.obj("paye" -> JsString("PAYE11111"), "hasPaye" -> JsBoolean(true)),
          "businessName" -> JsString("Company Test"))

        val result = partnershipDetailsWithoutOption.as[OrganisationDetailType](OrganisationDetailType.partnershipApiReads)

        result.vatRegistrationNumber mustBe None
      }

      "We have optional parent and dont have VAT registration number" in {

        val partnershipDetailsWithoutOption = Json.obj(
          "partnershipVat" -> Json.obj("hasPaye" -> JsBoolean(false)),
          "partnershipPaye" -> Json.obj("paye" -> JsString("PAYE11111"), "hasPaye" -> JsBoolean(true)),
          "businessName" -> JsString("Company Test"))
        val result = partnershipDetailsWithoutOption.as[OrganisationDetailType](OrganisationDetailType.partnershipApiReads)

        result.vatRegistrationNumber mustBe None
      }

      "We have VAT registration number" in {
        val result = partnershipDetails.as[OrganisationDetailType](OrganisationDetailType.partnershipApiReads)

        result.vatRegistrationNumber mustBe companySample.vatRegistrationNumber
      }

    }

    "Reading optional keys from Json for partnershipPaye " must {

      "We dont Paye number" in {

        val partnershipDetailsWithoutOption = Json.obj("partnershipVat" -> Json.obj("vat" -> JsString("VAT11111"), "hasVat" -> JsBoolean(true)),
          "businessName" -> JsString("Company Test"))

        val result = partnershipDetailsWithoutOption.as[OrganisationDetailType](OrganisationDetailType.partnershipApiReads)

        result.payeReference mustBe None
      }

      "We have Paye number" in {
        val result = partnershipDetails.as[OrganisationDetailType](OrganisationDetailType.partnershipApiReads)

        result.payeReference mustBe companySample.payeReference
      }

    }

    Seq(("Company", companyDetails), ("Partnership", partnershipDetails)).foreach { entityType =>
      val (orgType, orgData) = entityType
      s"Map correctly to a $orgType OrganisationDetailType model" when {
        val apiReads = if (orgType == "Company") OrganisationDetailType.companyApiReads else OrganisationDetailType.partnershipApiReads

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
          val result = companyDetails.as[OrganisationDetailType](OrganisationDetailType.companyApiReads)

          result.crnNumber mustBe companySample.crnNumber
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
  private val companyDetails = Json.obj("vat" -> JsString("VAT11111"), "paye" -> JsString("PAYE11111"),
    "companyRegistrationNumber" -> JsString("CRN11111"), "businessName" -> JsString("Test Name"))

  private def orgDetailWithoutVat(entityType: String): JsValue = {
    if (entityType == "Partnership") {
      partnershipDetails - "vat"
    } else {
      companyDetails - "vat"
    }
  }

  private def orgDetailWithoutPaye(entityType: String): JsValue = {
    if (entityType == "Partnership") {
      partnershipDetails - "paye"
    } else {
      companyDetails - "paye"
    }
  }

  private val partnershipDetails = Json.obj("vat" -> JsString("VAT11111"), "hasVat" -> JsBoolean(true),
    "paye" -> JsString("PAYE11111"),
    "businessName" -> JsString("Test Name"))
}
