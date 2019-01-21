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
        val transformedJson = inputJson.transform(jsonTransformer()).asOpt.value

        (transformedJson \ "businessDetails" \ "companyName").as[String] mustBe "Acme Ltd"
      }

      "we have idType UTR we correctly map the UTR number" in {

        val transformedJson = inputJson.transform(jsonTransformer()).asOpt.value

        (transformedJson \ "businessDetails" \ "uniqueTaxReferenceNumber").asOpt[String] mustBe Some("0123456789")
      }

      "we don't have an idtype of UTR" in {
        val inputWithIdTypeNino = inputJson.transform(
          updateJson(__ \ 'psaSubscriptionDetails \ 'customerIdentificationDetails,"idType","NINO")).asOpt.value

        val idTransformJson = inputWithIdTypeNino.transform(jsonTransformer(inputWithIdTypeNino)).asOpt.value

        (idTransformJson \ "businessDetails" \ "uniqueTaxReferenceNumber").asOpt[String] mustBe None
      }

      "we have a crn" in {
        val transformedJson = inputJson.transform(jsonTransformer()).asOpt.value

        (transformedJson \ "companyRegistrationNumber").as[String] mustBe "AB123456"
      }

      "we don't have vat" in {
        val transformedJson = inputJson.transform(jsonTransformer()).asOpt.value
        (transformedJson \ "companyDetails" \ "vatRegistrationNumber").asOpt[String] mustBe None
      }

      "we have a vat" in {
        val vatJson = inputJson.transform(
          updateJson(__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails,"vatRegistrationNumber","123456789")).asOpt.value

        val transformedJson = vatJson.transform(jsonTransformer()).asOpt.value

        (transformedJson \ "companyDetails" \ "vatRegistrationNumber").as[String] mustBe "123456789"
      }

      "we have a paye" in {
        val transformedJson = inputJson.transform(jsonTransformer()).asOpt.value

        (transformedJson \ "companyDetails" \ "payeEmployerReferenceNumber").as[String] mustBe "123AB45678"
      }

      "transform the input json to user answers" in {
        val transformedJson = inputJson.transform(jsonTransformer()).asOpt.value

        transformedJson mustBe expectedJson
      }
    }
  }

  def doNothing: Reads[JsObject] = __.json.put(Json.obj())

  def jsonTransformer(jsonFromDES: JsValue = inputJson): Reads[JsObject] =
    (if ((jsonFromDES \ "psaSubscriptionDetails" \ "customerIdentificationDetails" \ "idType").asOpt[String].contains("UTR")) {
      (__ \ 'businessDetails \ 'uniqueTaxReferenceNumber).json.copyFrom((__ \ 'psaSubscriptionDetails \ 'customerIdentificationDetails \ 'idNumber).json.pick)
    } else doNothing) and
      getOrganisationOrPartnerDetails reduce

  private def getOrganisationOrPartnerDetails: Reads[JsObject] = {
    val organisationOrPartnerDetailsPath = __ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails

    (__ \ 'companyRegistrationNumber).json.copyFrom((organisationOrPartnerDetailsPath \ 'crnNumber).json.pick) and
    (__ \ 'businessDetails \ 'companyName).json.copyFrom((organisationOrPartnerDetailsPath \ 'name).json.pick) and
      ((__ \ 'companyDetails \ 'vatRegistrationNumber).json.copyFrom((organisationOrPartnerDetailsPath \ 'vatRegistrationNumber).json.pick)
        orElse doNothing) and
      (__ \ 'companyDetails \ 'payeEmployerReferenceNumber).json.copyFrom((organisationOrPartnerDetailsPath \ 'payeReference).json.pick) reduce
  }
}

object CustomerIdentificationDetailsTypeReadsSpec {

  val expectedJson: JsValue = Json.parse(
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

  val inputJson: JsValue = Json.parse(
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

  def updateJson(path: JsPath, name: String, value: String): Reads[JsObject] = {
    path.json.update(__.read[JsObject].map(o => o ++ Json.obj(name -> value)))
  }
}
