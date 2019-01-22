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
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.JsonTransformations.PSASubscriptionDetailsTransformer

class CustomerIdentificationDetailsTypeTransformationSpec extends WordSpec with MustMatchers with OptionValues {

  import CustomerIdentificationDetailsTypeTransformationSpec._

  "organisation Or PartnerDetailsType" must {

    "map correctly into user answers" when {

      lazy val transformedJson = inputJson.transform(PSASubscriptionDetailsTransformer.transformToUserAnswers(inputJson)).asOpt.value

      "we have company name" in {
        (transformedJson \ "businessDetails" \ "companyName").as[String] mustBe "Acme Ltd"
      }

      "we have idType UTR we correctly map the UTR number" in {
        (transformedJson \ "businessDetails" \ "uniqueTaxReferenceNumber").asOpt[String] mustBe Some("0123456789")
      }

      "we don't have an idtype of UTR" in {
        val inputWithIdTypeNino = inputJson.transform(
          updateJson(__ \ 'psaSubscriptionDetails \ 'customerIdentificationDetails,"idType","NINO")).asOpt.value

        val idTransformJson = inputWithIdTypeNino.transform(PSASubscriptionDetailsTransformer.transformToUserAnswers(inputWithIdTypeNino)).asOpt.value

        (idTransformJson \ "businessDetails" \ "uniqueTaxReferenceNumber").asOpt[String] mustBe None
      }

      "we have a crn" in {
        (transformedJson \ "companyRegistrationNumber").as[String] mustBe "AB123456"
      }

      "we don't have vat" in {
        (transformedJson \ "companyDetails" \ "vatRegistrationNumber").asOpt[String] mustBe None
      }

      "we have a vat" in {
        val vatJson = inputJson.transform(
          updateJson(__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails,"vatRegistrationNumber","123456789")).asOpt.value

        val transformedJson = vatJson.transform(PSASubscriptionDetailsTransformer.transformToUserAnswers(inputJson)).asOpt.value

        (transformedJson \ "companyDetails" \ "vatRegistrationNumber").as[String] mustBe "123456789"
      }

      "we have a paye" in {
        (transformedJson \ "companyDetails" \ "payeEmployerReferenceNumber").as[String] mustBe "123AB45678"
      }

      "we have first name" in {
        (transformedJson \ "individualDetails" \ "firstName").as[String] mustBe "John"
      }

      "we have last name" in {
        (transformedJson \ "individualDetails" \ "lastName").as[String] mustBe "Doe"
      }

      "we don't have middle name" in {
        (transformedJson \ "individualDetails" \ "middleName").asOpt[String] mustBe None
      }

      "we have a middle name" in {
        val vatJson = inputJson.transform(
          updateJson(__ \ 'psaSubscriptionDetails \ 'individualDetails,"middleName","A")).asOpt.value

        val transformedJson = vatJson.transform(PSASubscriptionDetailsTransformer.transformToUserAnswers(inputJson)).asOpt.value

        (transformedJson \ "individualDetails" \ "middleName").as[String] mustBe "A"
      }

      "we have date of birth" in {
        (transformedJson \ "individualDateOfBirth").as[String] mustBe "1947-03-29"
      }

      "we have telephone" in {
        (transformedJson \ "contactDetails" \ "phone").as[String] mustBe "12345"
      }

      "we don't have email" in {
        (transformedJson \ "contactDetails" \ "email").asOpt[String] mustBe None
      }

      "we have email" in {
        val vatJson = inputJson.transform(
          updateJson(__ \ 'psaSubscriptionDetails \ 'correspondenceContactDetails,"email","test@test.com")).asOpt.value

        val transformedJson = vatJson.transform(PSASubscriptionDetailsTransformer.transformToUserAnswers(inputJson)).asOpt.value

        (transformedJson \ "contactDetails" \ "email").as[String] mustBe "test@test.com"
      }

      "transform the input json to user answers" in {
        transformedJson mustBe expectedJson
      }
    }
  }
}

object CustomerIdentificationDetailsTypeTransformationSpec {

  val expectedJson: JsValue = Json.parse(
    """{
        "businessDetails": {
          "companyName": "Acme Ltd",
          "uniqueTaxReferenceNumber": "0123456789"
        },
        "companyRegistrationNumber": "AB123456",
        "companyDetails": {
          "payeEmployerReferenceNumber": "123AB45678"
        },
        "companyAddressId": {
          "addressLine1": "100 SuttonStreet",
          "addressLine2": "Wokingham",
          "addressLine3": "Surrey",
          "addressLine4": "London",
          "postalCode": "DH14EJ",
          "countryCode": "GB"
        },
        "contactDetails" : {
          "phone" : "12345"
        },
        "individualDetails": {
          "firstName": "John",
          "lastName": "Doe"
        },
        "individualDateOfBirth": "1947-03-29"
      }"""
  )

  val inputJson: JsValue = Json.parse(
    """{
        "processingDate": "2001-12-17T09:30:47Z",
        "psaSubscriptionDetails": {
          "isPSASuspension": false,
          "customerIdentificationDetails": {
            "legalStatus": "Limited Company",
            "idType": "UTR",
            "idNumber": "0123456789",
            "noIdentifier": false
          },
          "individualDetails": {
            "title": "Mr",
            "firstName": "John",
            "lastName": "Doe",
            "dateOfBirth": "1947-03-29"
          },
          "organisationOrPartnerDetails": {
            "name": "Acme Ltd",
            "crnNumber": "AB123456",
            "payeReference": "123AB45678"
          },
          "correspondenceContactDetails": {
            "telephone": "12345",
            "mobileNumber": " ",
            "fax": " "
          },
          "correspondenceAddressDetails": {
            "nonUKAddress": false,
            "line1": "100 SuttonStreet",
            "line2": "Wokingham",
            "line3": "Surrey",
            "line4": "London",
            "postalCode": "DH14EJ",
            "countryCode": "GB"
          }
        }
      }"""
  )

  def updateJson(path: JsPath, name: String, value: String): Reads[JsObject] = {
    path.json.update(__.read[JsObject].map(o => o ++ Json.obj(name -> value)))
  }
}
