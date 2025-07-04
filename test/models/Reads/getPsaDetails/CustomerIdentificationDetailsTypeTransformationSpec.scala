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

package models.Reads.getPsaDetails

import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*
import play.api.libs.json.Reads.*
import utils.JsonTransformations.*

class CustomerIdentificationDetailsTypeTransformationSpec extends AnyWordSpec with Matchers with OptionValues {

  import CustomerIdentificationDetailsTypeTransformationSpec.*

  lazy val transformedJson: JsObject = individualInputJson.transform(transformer.transformToUserAnswers).asOpt.value

  "registrationInfo" must {
    "map correctly into user answers" when {

      "we have legal status" in {
        (transformedJson \ "registrationInfo" \ "legalStatus").as[String].mustBe("Individual")
      }

      "we have customerType" in {
        (transformedJson \ "registrationInfo" \ "customerType").as[String].mustBe("UK")
      }

      "we have idType" in {
        (transformedJson \ "registrationInfo" \ "idType").as[String].mustBe("NINO")
      }

      "we don't have idType" in {
        val jsonUpdated = individualInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("customerIdentificationDetails")
          \ Symbol("idType")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "registrationInfo" \ "idType").asOpt[String].mustBe(None)
      }

      "we have idNumber" in {
        (transformedJson \ "registrationInfo" \ "idNumber").as[String].mustBe("AB123456C")
      }

      "we don't have idNumber" in {
        val jsonUpdated = individualInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("customerIdentificationDetails")
          \ Symbol("idNumber")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "registrationInfo" \ "idNumber").asOpt[String].mustBe(None)
      }
    }
  }

  "individual" must {
    "map correctly into individual user answers" when {

      "we have nino" in {
        (transformedJson \ "individualNino").as[String].mustBe("AB123456C")
      }

      "we have first name" in {
        (transformedJson \ "individualDetails" \ "firstName").as[String].mustBe("John")
      }

      "we have last name" in {
        (transformedJson \ "individualDetails" \ "lastName").as[String].mustBe("Doe")
      }

      "we have middle name" in {
        (transformedJson \ "individualDetails" \ "middleName").as[String].mustBe("Robert")
      }

      "we don't have a middle name" in {
        val jsonUpdated = individualInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("individualDetails")
          \ Symbol("middleName")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value

        (transformedJson \ "individualDetails" \ "middleName").asOpt[String].mustBe(None)
      }

      "we have date of birth" in {
        (transformedJson \ "individualDateOfBirth").as[String].mustBe("1947-03-29")
      }

      "we have address line 1" in {
        (transformedJson \ "individualContactAddress" \ "addressLine1").as[String].mustBe("Flat 4")
      }

      "we have address line 2" in {
        (transformedJson \ "individualContactAddress" \ "addressLine2").as[String].mustBe("Central Tower")
      }

      "we don't have address line 3" in {
        val jsonUpdated = individualInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("correspondenceAddressDetails")
          \ Symbol("line3")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "individualContactAddress" \ "addressLine3").asOpt[String].mustBe(None)
      }

      "we have address line 3" in {
        (transformedJson \ "individualContactAddress" \ "addressLine3").as[String].mustBe("Telford")
      }

      "we don't have address line 4" in {
        val jsonUpdated = individualInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("correspondenceAddressDetails")
          \ Symbol("line4")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "individualContactAddress" \ "addressLine4").asOpt[String].mustBe(None)
      }

      "we have address line 4" in {
        (transformedJson \ "individualContactAddress" \ "addressLine4").as[String].mustBe("Shropshire")
      }

      "we don't have address postal code" in {
        val jsonUpdated = individualInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("correspondenceAddressDetails")

          \ Symbol("postalCode")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "individualContactAddress" \ "postalCode").asOpt[String].mustBe(None)
      }

      "we have address postal code" in {
        (transformedJson \ "individualContactAddress" \ "postcode").as[String].mustBe("TF1 4ER")
      }

      "we have address country code" in {
        (transformedJson \ "individualContactAddress" \ "country").as[String].mustBe("GB")
      }

      "we have individual address years" in {
        (transformedJson \ "individualAddressYears").as[String].mustBe("under_a_year")
      }

      "we have previous address line 1" in {
        (transformedJson \ "individualPreviousAddress" \ "addressLine1").as[String].mustBe("1 The Avenue")
      }

      "we have previous address line 2" in {
        (transformedJson \ "individualPreviousAddress" \ "addressLine2").as[String].mustBe("Central Park")
      }

      "we don't have previous address line 3" in {
        val jsonUpdated = individualInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("previousAddressDetails") \ Symbol("previousAddress")
          \ Symbol("line3")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "individualPreviousAddress" \ "addressLine3").asOpt[String].mustBe(None)
      }

      "we have previous address line 3" in {
        (transformedJson \ "individualPreviousAddress" \ "addressLine3").as[String].mustBe("Telford")
      }

      "we don't have previous address line 4" in {
        val jsonUpdated =
          individualInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("previousAddressDetails") \ Symbol("previousAddress")
            \ Symbol("line4")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "individualPreviousAddress" \ "addressLine4").asOpt[String].mustBe(None)
      }

      "we have previous address line 4" in {
        (transformedJson \ "individualPreviousAddress" \ "addressLine4").as[String].mustBe("Shrop")
      }

      "we don't have previous address postal code" in {
        val jsonUpdated =
          individualInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("previousAddressDetails") \ Symbol("previousAddress")
            \ Symbol("postalCode")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "individualPreviousAddress" \ "postcode").asOpt[String].mustBe(None)
      }

      "we have previous address postal code" in {
        (transformedJson \ "individualPreviousAddress" \ "postcode").as[String].mustBe("TF3 4DC")
      }

      "we have previous address country code" in {
        (transformedJson \ "individualPreviousAddress" \ "country").as[String].mustBe("GB")
      }

      "we have telephone" in {
        (transformedJson \ "individualContactDetails" \ "phone").as[String].mustBe("0151 6551234 ")
      }

      "we have email" in {
        (transformedJson \ "individualContactDetails" \ "email").as[String].mustBe("robert@test.com")
      }

      "we don't have email" in {
        val jsonUpdated = individualInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("correspondenceContactDetails")
          \ Symbol("email")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "individualContactDetails" \ "email").asOpt[String].mustBe(None)
      }

      "transform the individual input json to user answers" in {
        transformedJson.mustBe(expectedIndividualJson)
      }
    }
  }

  "organisation" must {

    "map correctly into user answers" when {

      lazy val transformedJson = companyInputJson.transform(transformer.transformToUserAnswers).asOpt.value

      "we have company name" in {
        (transformedJson \ "businessName").as[String].mustBe("Global Pensions Ltd")
      }

      "we have idType UTR we correctly map the UTR number" in {
        (transformedJson \ "utr").asOpt[String].mustBe(Some("0123456789"))
      }

      "we don't have an idtype of UTR" in {
        val inputWithIdTypeNino = companyInputJson.transform(
          updateJson(__ \ Symbol("psaSubscriptionDetails") \ Symbol("customerIdentificationDetails"), "idType", "NINO")).asOpt.value

        val idTransformJson = inputWithIdTypeNino.transform(transformer.transformToUserAnswers).asOpt.value

        (idTransformJson \ "utr").asOpt[String].mustBe(None)
      }

      "we have a crn" in {
        (transformedJson \ "companyRegistrationNumber").as[String].mustBe("AB123456")
      }

      "we have vat" in {
        (transformedJson \ "vat").as[String].mustBe("123456789")
      }

      "we don't have a vat" in {
        val jsonUpdated =
          companyInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("organisationOrPartnerDetails")
            \ Symbol("vatRegistrationNumber")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value

        (transformedJson \ "vat").asOpt[String].mustBe(None)
      }

      "we have a paye" in {
        (transformedJson \ "paye").as[String].mustBe("123AB45678")
      }

      "we have address line 1" in {
        (transformedJson \ "companyContactAddress" \ "addressLine1").as[String].mustBe("Flat 4")
      }

      "we have address line 2" in {
        (transformedJson \ "companyContactAddress" \ "addressLine2").as[String].mustBe("Central Tower")
      }

      "we don't have address line 3" in {
        val jsonUpdated = companyInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("correspondenceAddressDetails")
          \ Symbol("line3")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "companyContactAddress" \ "addressLine3").asOpt[String].mustBe(None)
      }

      "we have address line 3" in {
        (transformedJson \ "companyContactAddress" \ "addressLine3").as[String].mustBe("Telford")
      }

      "we don't have address line 4" in {
        val jsonUpdated = companyInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("correspondenceAddressDetails")
          \ Symbol("line4")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "companyContactAddress" \ "addressLine4").asOpt[String].mustBe(None)
      }

      "we have address line 4" in {
        (transformedJson \ "companyContactAddress" \ "addressLine4").as[String].mustBe("Shropshire")
      }

      "we don't have address postal code" in {
        val jsonUpdated = companyInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("correspondenceAddressDetails")
          \ Symbol("postalCode")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "companyContactAddress" \ "postalCode").asOpt[String].mustBe(None)
      }

      "we have address postal code" in {
        (transformedJson \ "companyContactAddress" \ "postcode").as[String].mustBe("TF1 4ER")
      }

      "we have address country code" in {
        (transformedJson \ "companyContactAddress" \ "country").as[String].mustBe("GB")
      }

      "we have telephone" in {
        (transformedJson \ "contactDetails" \ "phone").as[String].mustBe("0191 644595 ")
      }

      "we have email" in {
        (transformedJson \ "contactDetails" \ "email").as[String].mustBe("global.pensions@test.com")
      }

      "we don't have email" in {
        val jsonUpdated = companyInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("correspondenceContactDetails")
          \ Symbol("email")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "contactDetails" \ "email").asOpt[String].mustBe(None)
      }

      "we have company address years" in {
        (transformedJson \ "companyAddressYears").as[String].mustBe("under_a_year")
      }

      "we have previous address line 1" in {
        (transformedJson \ "companyPreviousAddress" \ "addressLine1").as[String].mustBe("3 Other Place")
      }

      "we have previous address line 2" in {
        (transformedJson \ "companyPreviousAddress" \ "addressLine2").as[String].mustBe("Some District")
      }

      "we don't have previous address line 3" in {
        val jsonUpdated =
          companyInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("previousAddressDetails") \ Symbol("previousAddress")
            \ Symbol("line3")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "companyPreviousAddress" \ "addressLine3").asOpt[String].mustBe(None)
      }

      "we have previous address line 3" in {
        (transformedJson \ "companyPreviousAddress" \ "addressLine3").as[String].mustBe("Anytown")
      }

      "we don't have previous address line 4" in {
        val jsonUpdated = companyInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("previousAddressDetails") \ Symbol("previousAddress")
          \ Symbol("line4")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "companyPreviousAddress" \ "addressLine4").asOpt[String].mustBe(None)
      }

      "we have previous address line 4" in {
        (transformedJson \ "companyPreviousAddress" \ "addressLine4").as[String].mustBe("Somerset")
      }

      "we don't have previous address postal code" in {
        val jsonUpdated =
          companyInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("previousAddressDetails") \ Symbol("previousAddress")
            \ Symbol("postalCode")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "companyPreviousAddress" \ "postcode").asOpt[String].mustBe(None)
      }

      "we have previous address postal code" in {
        (transformedJson \ "companyPreviousAddress" \ "postcode").as[String].mustBe("MN21 EF")
      }

      "we have previous address country code" in {
        (transformedJson \ "companyPreviousAddress" \ "country").as[String].mustBe("GB")
      }

      "we have directors" in {
        (transformedJson \ "directors" \ 0 \ "directorDetails" \ "firstName")
          .as[String].mustBe((expectedCompanyJson \ "directors" \ 0 \ "directorDetails" \ "firstName").as[String])
      }

      "transform the input json to user answers" in {
        transformedJson.mustBe(expectedCompanyJson)
      }
    }
  }

  "partnership" must {

    "map correctly into user answers" when {

      lazy val transformedJson = partnershipInputJson.transform(transformer.transformToUserAnswers).asOpt.value

      "we have partnership name" in {
        (transformedJson \ "businessName").as[String].mustBe("Acme Partnership")
      }

      "we have idType UTR we correctly map the UTR number" in {
        (transformedJson \ "utr").asOpt[String].mustBe(Some("0123456789"))
      }

      "we don't have an idtype of UTR" in {
        val inputWithIdTypeNino = partnershipInputJson.transform(
          updateJson(__ \ Symbol("psaSubscriptionDetails") \ Symbol("customerIdentificationDetails"), "idType", "NINO")).asOpt.value

        val idTransformJson = inputWithIdTypeNino.transform(transformer.transformToUserAnswers).asOpt.value

        (idTransformJson \ "partnershipDetails" \ "uniqueTaxReferenceNumber").asOpt[String].mustBe(None)
      }

      "we have a paye" in {
        (transformedJson \ "paye").as[String].mustBe("123AB45678")
      }

      "we have address line 1" in {
        (transformedJson \ "partnershipContactAddress" \ "addressLine1").as[String].mustBe("Flat 1")
      }

      "we have address line 2" in {
        (transformedJson \ "partnershipContactAddress" \ "addressLine2").as[String].mustBe("Waterloo House")
      }

      "we don't have address line 3" in {
        val jsonUpdated = partnershipInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("correspondenceAddressDetails")
          \ Symbol("line3")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "partnershipContactAddress" \ "addressLine3").asOpt[String].mustBe(None)
      }

      "we have address line 3" in {
        (transformedJson \ "partnershipContactAddress" \ "addressLine3").as[String].mustBe("Newcastle")
      }

      "we don't have address line 4" in {
        val jsonUpdated = partnershipInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("correspondenceAddressDetails")
          \ Symbol("line4")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "partnershipContactAddress" \ "addressLine4").asOpt[String].mustBe(None)
      }

      "we have address line 4" in {
        (transformedJson \ "partnershipContactAddress" \ "addressLine4").as[String].mustBe("Tyne and Wear")
      }

      "we don't have address postal code" in {
        val jsonUpdated = partnershipInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("correspondenceAddressDetails")
          \ Symbol("postalCode")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "partnershipContactAddress" \ "postalCode").asOpt[String].mustBe(None)
      }

      "we have address postal code" in {
        (transformedJson \ "partnershipContactAddress" \ "postcode").as[String].mustBe("NE1 4ER")
      }

      "we have address country code" in {
        (transformedJson \ "partnershipContactAddress" \ "country").as[String].mustBe("GB")
      }

      "we have telephone" in {
        (transformedJson \ "partnershipContactDetails" \ "phone").as[String].mustBe("0191 644595 ")
      }

      "we have email" in {
        (transformedJson \ "partnershipContactDetails" \ "email").as[String] mustBe "acme_partnership@email.com"
      }

      "we don't have email" in {
        val jsonUpdated = partnershipInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("correspondenceContactDetails")
          \ Symbol("email")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "partnershipContactDetails" \ "email").asOpt[String].mustBe(None)
      }

      "we have partnership address years" in {
        (transformedJson \ "partnershipAddressYears").as[String].mustBe("under_a_year")
      }

      "we have previous address line 1" in {
        (transformedJson \ "partnershipPreviousAddress" \ "addressLine1").as[String].mustBe("3 Other Place")
      }

      "we have previous address line 2" in {
        (transformedJson \ "partnershipPreviousAddress" \ "addressLine2").as[String].mustBe("Some District")
      }

      "we don't have previous address line 3" in {
        val jsonUpdated =
          partnershipInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("previousAddressDetails")
            \ Symbol("previousAddress") \ Symbol("line3")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "partnershipPreviousAddress" \ "addressLine3").asOpt[String].mustBe(None)
      }

      "we have previous address line 3" in {
        (transformedJson \ "partnershipPreviousAddress" \ "addressLine3").as[String].mustBe("Anytown")
      }

      "we don't have previous address line 4" in {
        val jsonUpdated = partnershipInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("previousAddressDetails")
          \ Symbol("previousAddress") \ Symbol("line4")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "partnershipPreviousAddress" \ "addressLine4").asOpt[String].mustBe(None)
      }

      "we have previous address line 4" in {
        (transformedJson \ "partnershipPreviousAddress" \ "addressLine4").as[String].mustBe("Somerset")
      }

      "we don't have previous address postal code" in {
        val jsonUpdated =
          partnershipInputJson.transform((__ \ Symbol("psaSubscriptionDetails") \ Symbol("previousAddressDetails")
            \ Symbol("previousAddress") \ Symbol("postalCode")).json.prune).asOpt.value
        val transformedJson = jsonUpdated.transform(transformer.transformToUserAnswers).asOpt.value
        (transformedJson \ "partnershipPreviousAddress" \ "postcode").asOpt[String].mustBe(None)
      }

      "we have previous address postal code" in {
        (transformedJson \ "partnershipPreviousAddress" \ "postcode").as[String].mustBe("MN21 EF")
      }

      "we have previous address country code" in {
        (transformedJson \ "partnershipPreviousAddress" \ "country").as[String].mustBe("GB")
      }

      "we have partners" in {
        (transformedJson \ "partners" \ 0 \ "partnerDetails" \ "firstName").as[String].mustBe(
          (expectedPartnershipJson \ "partners" \ 0 \ "partnerDetails" \ "firstName").as[String]
        )
      }

      "transform the input json to user answers" in {
        transformedJson.mustBe(expectedPartnershipJson)
      }
    }
  }
}

object CustomerIdentificationDetailsTypeTransformationSpec {

  val legalStatusTransformer = new LegalStatusTransformer
  val addressTransformer = new AddressTransformer(legalStatusTransformer)
  val directorOrPartnerTransformer = new DirectorOrPartnerTransformer(addressTransformer)
  val registrationInfoTransformer = new RegistrationInfoTransformer
  val payeAndVatTransformer = new PayeAndVatTransformer
  val adviserTransformer = new AdviserTransformer(addressTransformer)
  val individualTransformer = new IndividualTransformer(legalStatusTransformer)
  val transformer = new PSASubscriptionDetailsTransformer(addressTransformer,
    directorOrPartnerTransformer,
    legalStatusTransformer,
    registrationInfoTransformer,
    payeAndVatTransformer,
    adviserTransformer,
    individualTransformer)

  lazy val partnershipInputJson: JsValue = Json.parse(
    """
      {
   "processingDate":"2001-12-17T09:30:47Z",
   "psaSubscriptionDetails":{
     "customerIdentificationDetails":{
       "legalStatus":"Partnership",
       "idType":"UTR",
       "idNumber":"0123456789",
       "noIdentifier":false
     },
     "organisationOrPartnerDetails":{
       "name":"Acme Partnership",
       "vatRegistrationNumber":"123456789",
       "payeReference":"123AB45678"
     },
     "correspondenceAddressDetails":{
       "nonUKAddress":false,
       "line1":"Flat 1",
       "line2":"Waterloo House",
       "line3":"Newcastle",
       "line4":"Tyne and Wear",
       "postalCode":"NE1 4ER",
       "countryCode":"GB"
     },
     "correspondenceContactDetails":{
       "telephone":"0191 644595 ",
       "email":"acme_partnership@email.com"
     },
     "previousAddressDetails":{
       "isPreviousAddressLast12Month": true,
             "previousAddress": {
               "nonUKAddress": false,
               "line1": "3 Other Place",
               "line2": "Some District",
               "line3": "Anytown",
               "line4": "Somerset",
               "postalCode": "MN21 EF",
               "countryCode": "GB"
             }
     },
     "directorOrPartnerDetails":[
       {
                "sequenceId":"000",
                "entityType":"Partner",
                "title":"Mr",
                "firstName":"Bruce",
                "lastName":"Allen",
                "dateOfBirth":"1980-03-01",
                "nino":"JC000001A",
                "noNinoReason":"test",
                "utr":"0123456789",
                "noUtrReason":"test",
                "correspondenceCommonDetails":{
                  "addressDetails":{
                    "nonUKAddress":false,
                    "line1":"1 Partner Road",
                    "line2":"Clifton",
                    "line3":"York",
                    "line4":"Yorkshire",
                    "postalCode":"YO1 9EX",
                    "countryCode":"GB"
                  },
                  "contactDetails":{
                    "telephone":"0044-09876542312",
                    "mobileNumber":"0044-09876542312",
                    "fax":"0044-09876542312",
                    "email":"bruce_allen@test.com"
                  }
                },
                "previousAddressDetails":{
                  "isPreviousAddressLast12Month":true,
                  "previousAddress": {
                    "nonUKAddress": false,
                    "line1":"8 Pattinson Grove",
                    "line2":"Ryton",
                    "line3":"Tyne and Wear",
                    "line4":"Yorkshire",
                    "postalCode":"NE22 ARR",
                    "countryCode":"GB"
                  }
                }
             }
     ]
   }
 }
    """
  )

  lazy val expectedPartnershipJson: JsValue = Json.parse(
    """{
              "businessName" : "Acme Partnership",
              "utr" : "0123456789",
              "areYouInUK" : true,
              "registrationInfo" : {
                  "legalStatus" : "Partnership",
                  "sapNumber" : "",
                  "noIdentifier" : false,
                  "customerType" : "UK",
                  "idType" : "UTR",
                  "idNumber" : "0123456789"
              },
              "partnershipContactAddress" : {
                  "addressLine1" : "Flat 1",
                  "addressLine2" : "Waterloo House",
                  "addressLine3" : "Newcastle",
                  "addressLine4" : "Tyne and Wear",
                  "postcode" : "NE1 4ER",
                  "country" : "GB"
              },
              "partnershipAddressYears" : "under_a_year",
              "partnershipPreviousAddress" : {
                  "addressLine1" : "3 Other Place",
                  "addressLine2" : "Some District",
                  "addressLine3" : "Anytown",
                  "addressLine4" : "Somerset",
                  "postcode" : "MN21 EF",
                  "country" : "GB"
              },
              "partnershipContactDetails" : {
                  "email" : "acme_partnership@email.com",
                  "phone" : "0191 644595 "
              },
              "hasCrn": false,
              "hasVat": true,
              "hasPaye": true,
              "vat" : "123456789",
              "paye" : "123AB45678",
              "partners" : [
                {
                   "partnerDetails" : {
                       "firstName" : "Bruce",
                       "lastName" : "Allen"
                   },
                   "dateOfBirth" : "1980-03-01",
                   "hasNino": true,
                   "hasUtr": true,
                   "nino" : {
                        "value" : "JC000001A"
                    },
                   "utr" : {
                       "value" : "0123456789"
                   },
                   "partnerAddress" : {
                       "addressLine1" : "1 Partner Road",
                       "addressLine2" : "Clifton",
                       "addressLine3" : "York",
                       "addressLine4" : "Yorkshire",
                       "postcode" : "YO1 9EX",
                       "country" : "GB"
                   },
                   "partnerContactDetails" : {
                       "email" : "bruce_allen@test.com",
                       "phone" : "0044-09876542312"
                   },
                   "partnerAddressYears" : "under_a_year",
                   "partnerPreviousAddress" : {
                       "addressLine1" : "8 Pattinson Grove",
                       "addressLine2" : "Ryton",
                       "addressLine3" : "Tyne and Wear",
                       "addressLine4" : "Yorkshire",
                       "postcode" : "NE22 ARR",
                       "country" : "GB"
                   }
                }
              ],
              "declarationWorkingKnowledge" : true
          }"""
  )

  lazy val expectedCompanyJson: JsValue = Json.parse(
    """{
          "businessName" : "Global Pensions Ltd",
          "utr" : "0123456789",
          "areYouInUK" : true,
          "registrationInfo" : {
            "legalStatus" : "Limited Company",
            "sapNumber" : "",
            "noIdentifier" : false,
            "customerType" : "UK",
            "idType" : "UTR",
            "idNumber" : "0123456789"
          },
          "companyRegistrationNumber" : "AB123456",
          "hasCrn": true,
          "hasVat": true,
          "hasPaye": true,
          "vat" : "123456789",
          "paye" : "123AB45678",
          "companyContactAddress" : {
            "addressLine1" : "Flat 4",
            "addressLine2" : "Central Tower",
            "addressLine3" : "Telford",
            "addressLine4" : "Shropshire",
            "postcode" : "TF1 4ER",
            "country" : "GB"
          },
          "contactDetails" : {
             "email" : "global.pensions@test.com",
             "phone" : "0191 644595 "
           },
          "companyAddressYears" : "under_a_year",
          "companyPreviousAddress" : {
            "addressLine1" : "3 Other Place",
            "addressLine2" : "Some District",
            "addressLine3" : "Anytown",
            "addressLine4" : "Somerset",
            "postcode" : "MN21 EF",
            "country" : "GB"
          },
          "directors" : [
                    {
                      "directorDetails" : {
                        "firstName" : "Ann",
                         "middleName" : "Sarah",
                        "lastName" : "Baker"
                      },
                      "dateOfBirth" : "1980-03-01",
                      "hasNino": true,
                      "hasUtr": true,
                      "nino" : {
                         "value" : "JC000001A"
                       },
                      "utr" : {
                         "value" : "0123456789"
                       },
                      "directorAddress" : {
                        "addressLine1" : "1 Director Road",
                        "addressLine2" : "Clifton",
                        "addressLine3" : "York",
                        "addressLine4" : "Yorkshire",
                        "postcode" : "YO1 9EX",
                        "country" : "GB"
                      },
                      "directorContactDetails" : {
                        "phone" : "0044-09876542312",
                        "email" : "ann_baker@test.com"
                      },
                      "directorAddressYears" : "over_a_year"
                    }
         ],
         "declarationWorkingKnowledge": true
        }"""
  )

  lazy val companyInputJson: JsValue = Json.parse(
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
          "organisationOrPartnerDetails": {
            "name":"Global Pensions Ltd",
            "crnNumber":"AB123456",
            "vatRegistrationNumber":"123456789",
            "payeReference":"123AB45678"
          },
          "correspondenceContactDetails": {
            "telephone":"0191 644595 ",
            "email":"global.pensions@test.com"
          },
          "correspondenceAddressDetails": {
             "nonUKAddress": false,
             "line1": "Flat 4",
             "line2": "Central Tower",
             "line3": "Telford",
             "line4": "Shropshire",
             "postalCode": "TF1 4ER",
             "countryCode": "GB"
           },
          "previousAddressDetails": {
            "isPreviousAddressLast12Month": true,
            "previousAddress": {
              "nonUKAddress": false,
              "line1": "3 Other Place",
              "line2": "Some District",
              "line3": "Anytown",
              "line4": "Somerset",
              "postalCode": "MN21 EF",
              "countryCode": "GB"
            }
          },
          "directorOrPartnerDetails":[
                            {
                               "sequenceId":"000",
                               "entityType":"Director",
                               "title":"Mrs",
                               "firstName":"Ann",
                               "middleName":"Sarah",
                               "lastName":"Baker",
                               "dateOfBirth":"1980-03-01",
                               "nino":"JC000001A",
                               "noNinoReason" : "test",
                               "utr":"0123456789",
                               "noUtrReason" : "test",
                               "correspondenceCommonDetails":{
                                 "addressDetails":{
                                   "nonUKAddress":false,
                                   "line1":"1 Director Road",
                                   "line2":"Clifton",
                                   "line3":"York",
                                   "line4":"Yorkshire",
                                   "postalCode":"YO1 9EX",
                                   "countryCode":"GB"
                                 },
                                 "contactDetails":{
                                   "telephone":"0044-09876542312",
                                   "mobileNumber":"0044-09876542312",
                                   "fax":"0044-09876542312",
                                   "email":"ann_baker@test.com"
                                 }
                               },
                               "previousAddressDetails":{
                                 "isPreviousAddressLast12Month":false
                               }
                             }
          ]
        }
      }"""
  )

  lazy val individualInputJson: JsValue = Json.parse(
    """{
        "processingDate": "2017-12-17T09:30:47Z",
        "psaSubscriptionDetails": {
          "isPSASuspension": false,
          "customerIdentificationDetails": {
            "legalStatus": "Individual",
            "idType": "NINO",
            "idNumber": "AB123456C",
            "noIdentifier": false
          },
          "individualDetails": {
            "title": "Mr",
            "firstName": "John",
            "middleName": "Robert",
            "lastName": "Doe",
            "dateOfBirth": "1947-03-29"
          },
          "correspondenceAddressDetails": {
            "nonUKAddress": false,
            "line1": "Flat 4",
            "line2": "Central Tower",
            "line3": "Telford",
            "line4": "Shropshire",
            "postalCode": "TF1 4ER",
            "countryCode": "GB"
          },
          "correspondenceContactDetails": {
            "telephone": "0151 6551234 ",
            "email": "robert@test.com"
          },
          "previousAddressDetails": {
            "isPreviousAddressLast12Month": true,
            "previousAddress": {
              "nonUKAddress": false,
              "line1": "1 The Avenue",
              "line2": "Central Park",
              "line3": "Telford",
              "line4": "Shrop",
              "postalCode": "TF3 4DC",
              "countryCode": "GB"
            }
          },
          "declarationDetails": {
            "box1": true,
            "box2": true,
            "box3": true,
            "box4": true,
            "box6": true,
            "box7": true,
            "pensionAdvisorDetails": {
              "name": "Acme Pensions Ltd",
              "addressDetails": {
                "nonUKAddress": false,
                "line1": "10 London Road",
                "line2": "Oldpark",
                "line3": "Telford",
                "line4": "Shropshire",
                "postalCode": "TF2 4RR",
                "countryCode": "GB"
              },
              "contactDetails": {
                "telephone": "0044-0987654232",
                "mobileNumber": "0044-09876542335",
                "fax": "0044-098765423353",
                "email": "acme_pensions@test.com"
              }
            }
          }
        }
      }
      """)

  lazy val expectedIndividualJson: JsValue = Json.parse(
    """
     {
     "areYouInUK" : true,
     "registrationInfo" : {
       "legalStatus" : "Individual",
       "sapNumber" : "",
       "noIdentifier" : false,
       "customerType" : "UK",
       "idType" : "NINO",
       "idNumber" : "AB123456C"
     },
     "individualNino" : "AB123456C",
     "individualDetails" : {
       "firstName" : "John",
       "middleName": "Robert",
       "lastName" : "Doe"
     },
     "individualDateOfBirth" : "1947-03-29",
     "individualContactAddress" : {
       "addressLine1" : "Flat 4",
       "addressLine2" : "Central Tower",
       "addressLine3" : "Telford",
       "addressLine4" : "Shropshire",
       "postcode" : "TF1 4ER",
       "country" : "GB"
     },
     "individualContactDetails" : {
        "phone" : "0151 6551234 ",
        "email" : "robert@test.com"
      },
     "individualAddressYears" : "under_a_year",
     "individualPreviousAddress" : {
       "addressLine1" : "1 The Avenue",
       "addressLine2" : "Central Park",
       "addressLine3" : "Telford",
       "addressLine4" : "Shrop",
       "postcode" : "TF3 4DC",
       "country" : "GB"
     },
     "adviserName": "Acme Pensions Ltd",
     "adviserEmail" : "acme_pensions@test.com",
     "adviserPhone" : "0044-0987654232",
     "declarationWorkingKnowledge" : false,
     "adviserAddress" : {
             "addressLine1" : "10 London Road",
             "addressLine2" : "Oldpark",
             "addressLine3" : "Telford",
             "addressLine4" : "Shropshire",
             "postcode" : "TF2 4RR",
             "country" : "GB"
      }
   }
    """.stripMargin
  )

  def updateJson(path: JsPath, name: String, value: String): Reads[JsObject] = {
    path.json.update(__.read[JsObject].map(o => o ++ Json.obj(name -> value)))
  }
}
