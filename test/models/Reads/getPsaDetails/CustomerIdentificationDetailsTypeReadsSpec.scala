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

package models.Reads.getPsaDetails

import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._

class CustomerIdentificationDetailsTypeReadsSpec extends WordSpec with MustMatchers with OptionValues {

  import CustomerIdentificationDetailsTypeReadsSpec._

  "organisation Or PartnerDetailsType" must {

    "map correctly into user answers" when {

      "we have company name" in {
        val jsonTransformer = (__ \ 'businessDetails \ 'companyName).json.
          copyFrom((__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'name).json.pick)

        val transformedJson = inputJson.transform(jsonTransformer).asOpt.value

        (transformedJson \ "businessDetails" \ "companyName").as[String] mustBe "Acme Ltd"
      }

      "we have a utr" in {
        val jsonTransformer = (__ \ 'businessDetails \ 'uniqueTaxReferenceNumber).json.
          copyFrom((__ \ 'psaSubscriptionDetails \ 'customerIdentificationDetails \ 'idNumber).json.pick)

        val transformedJson = inputJson.transform(jsonTransformer).asOpt.value

        (transformedJson \ "businessDetails" \ "uniqueTaxReferenceNumber").as[String] mustBe "0123456789"
      }

      "we have a crn" in {
        val jsonTransformer = (__ \ 'companyRegistrationNumber).json.
          copyFrom((__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'crnNumber).json.pick)

        val transformedJson = inputJson.transform(jsonTransformer).asOpt.value

        (transformedJson \ "companyRegistrationNumber").as[String] mustBe "AB123456"
      }

      "we have a vat" in {
        val jsonTransformer = (__ \ 'companyDetails \ 'vatRegistrationNumber).json.
          copyFrom((__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'vatRegistrationNumber).json.pick)

        val transformedJson = inputJson.transform(jsonTransformer).asOpt.value

        (transformedJson \ "companyDetails" \ "vatRegistrationNumber").as[String] mustBe "123456789"
      }

      "we have a paye" in {
        val jsonTransformer = (__ \ 'companyDetails \ 'payeEmployerReferenceNumber).json.
          copyFrom((__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'payeReference).json.pick)

        val transformedJson = inputJson.transform(jsonTransformer).asOpt.value

        userAnswersTransformer(inputJson)
        (transformedJson \ "companyDetails" \ "payeEmployerReferenceNumber").as[String] mustBe "123AB45678"
      }

      "transform the input json to user answers" in {
        val result = userAnswersTransformer(inputJson).asOpt.value

        result mustBe expectedJson
      }
    }
  }

  private def userAnswersTransformer(inputJsValue: JsValue) = {

    val jsonTransformer = ((__ \ 'businessDetails \ 'companyName).json.copyFrom(
      (__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'name).json.pick) and
      (__ \ 'businessDetails \ 'uniqueTaxReferenceNumber).json.
        copyFrom((__ \ 'psaSubscriptionDetails \ 'customerIdentificationDetails \ 'idNumber).json.pick) and
      (__ \ 'companyRegistrationNumber).json.
        copyFrom((__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'crnNumber).json.pick) and
      (__ \ 'companyDetails \ 'vatRegistrationNumber).json.
        copyFrom((__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'vatRegistrationNumber).json.pick) and
      (__ \ 'companyDetails \ 'payeEmployerReferenceNumber).json.
        copyFrom((__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'payeReference).json.pick)
      ).reduce

    inputJsValue.transform(jsonTransformer)
  }

}

object CustomerIdentificationDetailsTypeReadsSpec {

  val expectedJson = Json.obj(
    "businessDetails" -> Json.obj(
      "companyName" -> "Acme Ltd",
      "uniqueTaxReferenceNumber" -> "0123456789"
    ),
    "companyRegistrationNumber" -> "AB123456",
    "companyDetails" -> Json.obj(
      "vatRegistrationNumber" -> "123456789",
      "payeEmployerReferenceNumber" -> "123AB45678"
    )
  )

  val inputJson = Json.obj(
    "psaSubscriptionDetails" -> Json.obj(
      "isPSASuspension" -> false,
      "customerIdentificationDetails" -> Json.obj(
        "legalStatus" -> "Limited Company",
        "idType" -> "UTR",
        "idNumber" -> "0123456789",
        "noIdentifier" -> false
      ),
      "organisationOrPartnerDetails" -> Json.obj(
        "name" -> "Acme Ltd",
        "crnNumber" -> "AB123456",
        "vatRegistrationNumber" -> "123456789",
        "payeReference" -> "123AB45678"
      )
    )
  )
}
