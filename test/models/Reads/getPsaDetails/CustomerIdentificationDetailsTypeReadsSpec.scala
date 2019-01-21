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
        val transformedJson = inputJson.transform(jsonTransformer).asOpt.value

        (transformedJson \ "businessDetails" \ "companyName").as[String] mustBe "Acme Ltd"
      }

      "we have idType utr we correctly map the utr number" ignore {
        println("\n\n\n id type : " + ((JsPath \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "idType").json.pick))
        val transform = (__ \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "idType").json.pick

        val jsonTransformer = (if ((__ \ 'psaSubscriptionDetails \ 'customerIdentificationDetails \ 'idType).read[String] == "UTR") {
          (__ \ 'businessDetails \ 'uniqueTaxReferenceNumber).json.
            copyFrom((__ \ 'psaSubscriptionDetails \ 'customerIdentificationDetails \ 'idNumber).json.pick)
        } else {
          (__).json.put(Json.obj())
        })

        val transformedJson = inputJson.transform(jsonTransformer).asOpt.value

        (transformedJson \ "businessDetails" \ "uniqueTaxReferenceNumber").asOpt[String] mustBe "0123456789"
      }

      "we don't have an idtype utr" in {
        val inputWithIdTypeNino = inputJson.transform(
          updateJson(__ \ 'psaSubscriptionDetails \ 'customerIdentificationDetails,"idType","NINO")).asOpt.value

        val idTransformJson = inputWithIdTypeNino.transform(jsonTransformer).asOpt.value

        (idTransformJson \ "businessDetails" \ "uniqueTaxReferenceNumber").asOpt[String] mustBe None
      }

      "we have a crn" in {
        val transformedJson = inputJson.transform(jsonTransformer).asOpt.value

        (transformedJson \ "companyRegistrationNumber").as[String] mustBe "AB123456"
      }

      "we don't have vat" in {
        val transformedJson = inputJson.transform(jsonTransformer).asOpt.value
        (transformedJson \ "companyDetails" \ "vatRegistrationNumber").asOpt[String] mustBe None
      }

      "we have a vat" in {
        val vatJson = inputJson.transform(
          updateJson(__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails,"vatRegistrationNumber","123456789")).asOpt.value

        val transformedJson = vatJson.transform(jsonTransformer).asOpt.value

        (transformedJson \ "companyDetails" \ "vatRegistrationNumber").as[String] mustBe "123456789"
      }

      "we have a paye" in {
        val transformedJson = inputJson.transform(jsonTransformer).asOpt.value

        (transformedJson \ "companyDetails" \ "payeEmployerReferenceNumber").as[String] mustBe "123AB45678"
      }

      "transform the input json to user answers" ignore {
        val transformedJson = inputJson.transform(jsonTransformer).asOpt.value

        transformedJson mustBe expectedJson
      }
    }
  }

  def doNothing: Reads[JsObject] = __.json.put(Json.obj())

  val jsonTransformer: Reads[JsObject] = ((__ \ 'businessDetails \ 'companyName).json.copyFrom(
    (__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'name).json.pick) and
    (if ((__ \ 'psaSubscriptionDetails \ 'customerIdentificationDetails \ 'idType).read[String] == "UTR") {
      (__ \ 'businessDetails \ 'uniqueTaxReferenceNumber).json.
        copyFrom((__ \ 'psaSubscriptionDetails \ 'customerIdentificationDetails \ 'idNumber).json.pick)
    } else {
      doNothing
    }) and
    (__ \ 'companyRegistrationNumber).json.
      copyFrom((__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'crnNumber).json.pick) and
    ((__ \ 'companyDetails \ 'vatRegistrationNumber).json.
      copyFrom((__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'vatRegistrationNumber).json.pick) orElse doNothing) and
    (__ \ 'companyDetails \ 'payeEmployerReferenceNumber).json.
      copyFrom((__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'payeReference).json.pick)
    ).reduce
}

object CustomerIdentificationDetailsTypeReadsSpec {

  val expectedJson = Json.parse(
    """{
        "businessDetails": {
          "companyName": "Acme Ltd",
          "uniqueTaxReferenceNumber": "0123456789"
        },
        "companyRegistrationNumber": "AB123456",
        "companyDetails": {
          "payeEmployerReferenceNumber": "123AB45678"
        }
      }"""
  )

  val inputJson = Json.parse(
    """{
        "processingDate":"2001-12-17T09:30:47Z",
        "psaSubscriptionDetails":{
          "isPSASuspension":false,
          "customerIdentificationDetails":{
            "legalStatus":"Limited Company",
            "idType":"UTR",
            "idNumber":"0123456789",
            "noIdentifier":false
          },
          "organisationOrPartnerDetails":{
            "name":"Acme Ltd",
            "crnNumber":"AB123456",
            "payeReference":"123AB45678"
          }
        }
      }"""
  )

  def updateJson(path: JsPath, name: String, value: String) = {
    path.json.update(__.read[JsObject].map(o => o ++ Json.obj(name -> value)))
  }
}
