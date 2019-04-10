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
import utils.JsonTransformations.{AddressTransformer, DirectorOrPartnerTransformer, LegalStatusTransformer, PSASubscriptionDetailsTransformer}

class PartnersTransformationSpec extends WordSpec with MustMatchers with OptionValues {

  "A payload containing a partner" must {
    "map correctly to a valid user answers partner" when {

      def doNothing: Reads[JsObject] = __.json.put(Json.obj())

      lazy val transformedJson = desPartner.transform(directorOrPartnerTransformer.getDirectorOrPartner("partner")).asOpt.value

      "We have partner details" when {
        "We have a name" in {
          (transformedJson \ "partnerDetails" \ "firstName").as[String] mustBe (userAnswersPartner \ "partnerDetails" \ "firstName").as[String]
        }

        "We have a middle name" in {
          (transformedJson \ "partnerDetails" \ "middleName").as[String] mustBe (userAnswersPartner \ "partnerDetails" \ "middleName").as[String]
        }

        "We don't have a middle name" in {
          val inputJson = desPartner.as[JsObject] - "middleName"

          val transformedJson = inputJson.transform(directorOrPartnerTransformer.getDirectorOrPartner("partner")).asOpt.value

          (transformedJson \ "partnerDetails" \ "middleName").asOpt[String] mustBe None
        }

        "We have a last name" in {
          (transformedJson \ "partnerDetails" \ "lastName").as[String] mustBe (userAnswersPartner \ "partnerDetails" \ "lastName").as[String]
        }

        "We have a DOB" in {
          (transformedJson \ "partnerDetails" \ "dateOfBirth").as[String] mustBe (userAnswersPartner \ "partnerDetails" \ "dateOfBirth").as[String]
        }

        "We have a nino" in {
          (transformedJson \ "partnerNino" \ "hasNino").as[Boolean] mustBe (userAnswersPartner \ "partnerNino" \ "hasNino").as[Boolean]
          (transformedJson \ "partnerNino" \ "nino").as[String] mustBe (userAnswersPartner \ "partnerNino" \ "nino").as[String]
        }

        "We don't have a nino" in {

          val inputJson = desPartner.as[JsObject] - "nino"

          val transformedJson = inputJson.transform(directorOrPartnerTransformer.getDirectorOrPartner("partner")).asOpt.value

          (transformedJson \ "partnerNino" \ "hasNino").as[Boolean] mustBe false
          //TODO: reason is not mandatory but mandatory in our frontend. Potential issues.
          (transformedJson \ "partnerNino" \ "reason").as[String] mustBe "test"
        }

        "We have a utr" in {
          (transformedJson \ "partnerUtr" \ "hasUtr").as[Boolean] mustBe (userAnswersPartner \ "partnerUtr" \ "hasUtr").as[Boolean]
          (transformedJson \ "partnerUtr" \ "utr").as[String] mustBe (userAnswersPartner \ "partnerUtr" \ "utr").as[String]
        }

        "We don't have a utr" in {
          val inputJson = desPartner.as[JsObject] - "utr"

          val transformedJson = inputJson.transform(directorOrPartnerTransformer.getDirectorOrPartner("partner")).asOpt.value

          (transformedJson \ "partnerUtr" \ "hasUtr").as[Boolean] mustBe false
          //TODO: reason is not mandatory but mandatory in our frontend. Potential issues.
          (transformedJson \ "partnerUtr" \ "reason").as[String] mustBe "test"
        }

        //TODO: DES has partner address details as not mandatory but we have it as mandatory in frontend (correspondenceCommonDetails wrapper). Potential issues.
        "We have an address" in {
          (transformedJson \ "partnerAddress" \ "addressLine1").asOpt[String].value mustBe (userAnswersPartner \ "partnerAddress" \ "addressLine1").asOpt[String].value
        }

        //TODO: Contact details is not mandatory in DES schema but mandatory in frontend. Potential issues.
        "We have a valid contact details" when {
          "with a telephone" in {
            (transformedJson \ "partnerContactDetails" \ "phone").asOpt[String].value mustBe (userAnswersPartner \ "partnerContactDetails" \ "phone").asOpt[String].value
          }

          "with an email" in {
            (transformedJson \ "partnerContactDetails" \ "email").asOpt[String].value mustBe (userAnswersPartner \ "partnerContactDetails" \ "email").asOpt[String].value
          }
        }

        "We have a previous address flag" in {
          (transformedJson \ "partnerAddressYears").asOpt[String].value mustBe (userAnswersPartner \ "partnerAddressYears" ).asOpt[String].value
        }

        "We have a previous address" in {
          (transformedJson \ "partnerPreviousAddress" \ "country").asOpt[String].value mustBe (userAnswersPartner \ "partnerPreviousAddress" \ "country" ).asOpt[String].value
        }

        "We have a previous address flag as false and no previous address" in {
          val inputJson = desPartner.as[JsObject] - "previousAddressDetails" + ("previousAddressDetails" -> Json.obj("isPreviousAddressLast12Month" -> JsBoolean(false)))

          val transformedJson = inputJson.transform(directorOrPartnerTransformer.getDirectorOrPartner("partner")).asOpt.value

          (transformedJson \ "partnerAddressYears").asOpt[String].value mustBe "over_a_year"
          (transformedJson \ "partnerPreviousAddress").asOpt[JsObject] mustBe None
        }

        "We have an array of directors" in {
          val directors = JsArray(Seq(desPartner, desPartner, desPartner, desPartner))

          val transformedJson = directors.transform(directorOrPartnerTransformer.getDirectorsOrPartners("partner")).asOpt.value

          (transformedJson \ 0 \ "partnerDetails" \ "firstName").as[String] mustBe (userAnswersPartner \ "partnerDetails" \ "firstName").as[String]
          (transformedJson \ 1 \ "partnerDetails" \ "firstName").as[String] mustBe (userAnswersPartner \ "partnerDetails" \ "firstName").as[String]
          (transformedJson \ 2 \ "partnerDetails" \ "firstName").as[String] mustBe (userAnswersPartner \ "partnerDetails" \ "firstName").as[String]
          (transformedJson \ 3 \ "partnerDetails" \ "firstName").as[String] mustBe (userAnswersPartner \ "partnerDetails" \ "firstName").as[String]
        }

        "We have exactly than 10 directors" in {
          val partialPayload = Json.obj(
            "psaSubscriptionDetails" -> Json.obj(
              "customerIdentificationDetails" -> legalStatus,
              "numberOfDirectorsOrPartnersDetails" -> moreThanTenFlag,
              "directorOrPartnerDetails" -> JsArray(Seq.fill(10)(desPartner))
            )
          )

          val transformedJson = partialPayload.transform(directorOrPartnerTransformer.getDirectorsOrPartners).asOpt.value
          (transformedJson \ "moreThanTenPartners").as[Boolean] mustBe false
        }
      }
    }
  }

  val legalStatusTransformer = new LegalStatusTransformer()
  val addressTransformer = new AddressTransformer(legalStatusTransformer)
  val directorOrPartnerTransformer = new DirectorOrPartnerTransformer(addressTransformer)

  val userAnswersPartner: JsValue = Json.parse(
"""{
            "partnerDetails" : {
                     "firstName" : "Bruce",
                     "middleName" : "John",
                     "lastName" : "Allen",
                     "dateOfBirth" : "1980-03-01",
                     "isDeleted" : false
                 },
                 "partnerNino" : {
                     "hasNino" : true,
                     "nino" : "JC000001A"
                 },
                 "partnerUtr" : {
                     "hasUtr" : true,
                     "utr" : "0123456789"
                 },
                 "partnerAddress" : {
                     "addressLine1" : "1 Partner Road",
                     "addressLine2" : "Clifton",
                     "addressLine3" : "York",
                     "addressLine4" : "Yorkshire",
                     "postcode" : "YO1 9EX",
                     "country" : "GB"
                 },
                 "partnerAddressYears" : "under_a_year",
                 "partnerPreviousAddress" : {
                     "addressLine1" : "8 Pattinson Grove",
                     "addressLine2" : "Ryton",
                     "addressLine3" : "Tyne and Wear",
                     "addressLine4" : "Yorkshire",
                     "postcode" : "NE22 ARR",
                     "country" : "GB"
                 },
                 "partnerContactDetails" : {
                     "email" : "bruce_allen@test.com",
                     "phone" : "0044-09876542312"
                 },
                 "isPartnerComplete" : true
        }""")

  val desPartner: JsValue = Json.parse(
    """{
               "sequenceId":"000",
               "entityType":"Partner",
               "title":"Mr",
               "firstName":"Bruce",
               "middleName":"John",
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
                   "postalCode":"NE2 ARR",
                   "countryCode":"GB"
                 }
               }
            }""")

  val moreThanTenFlag: JsValue = Json.parse(
    """{
                  "isMorethanTenPartners":false
              }""")

  val legalStatus: JsValue = Json.parse(
    """{
                  "legalStatus":"Partnership",
                  "idType":"UTR",
                  "idNumber":"0123456789",
                  "noIdentifier":false
             }""")

}
