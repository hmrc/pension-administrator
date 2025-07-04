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
import utils.JsonTransformations.{AddressTransformer, DirectorOrPartnerTransformer, LegalStatusTransformer}

class DirectorsTransformationSpec extends AnyWordSpec with Matchers with OptionValues {

  val legalStatusTransformer = new LegalStatusTransformer()
  val addressTransformer = new AddressTransformer(legalStatusTransformer)
  val directorOrPartnerTransformer = new DirectorOrPartnerTransformer(addressTransformer)

  val moreThanTenFlag: JsValue = Json.parse(
    """{
                  "isMorethanTenDirectors":true
              }""")

  val legalStatus: JsValue = Json.parse(
    """{
                  "legalStatus":"Limited Company",
                  "idType":"UTR",
                  "idNumber":"0123456789",
                  "noIdentifier":false
             }""")

  val userAnswersDirector: JsValue = Json.parse(
    """      {
                     "directorDetails" : {
                       "firstName" : "Ann",
                       "middleName" : "Sarah",
                       "lastName" : "Baker",
                       "isDeleted" : false
                     },
                     "hasNino" : true,
                     "dateOfBirth" : "1980-03-01",
                     "nino" : {
                        "value" : "JC000001A"
                      },
                      "hasUtr": true,
                      "utr" : {
                        "value" : "0123456789"
                      },
                     "directorAddress" : {
                       "addressLine1" : "1 Director Road",
                       "addressLine2" : "Some District",
                       "addressLine3" : "Anytown",
                       "addressLine4" : "Somerset",
                       "postcode" : "ZZ1 1ZZ",
                       "country" : "GB"
                     },
                     "directorAddressYears" : "under_a_year",
                     "directorPreviousAddress" : {
                         "addressLine1" : "8 Pattinson Grove",
                         "addressLine2" : "Ryton",
                         "addressLine4" : "Tyne and Wear",
                         "postcode" : "NE22 ARR",
                         "country" : "ES"
                     },
                     "directorContactDetails" : {
                       "email" : "ann_baker@test.com",
                       "phone" : "0044-09876542312"
                     },
                     "isDirectorComplete" : true
                   }""")

  val desDirector: JsValue = Json.parse(
    """      {
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
                                "isPreviousAddressLast12Month":true,
                                "previousAddress": {
                                  "nonUKAddress": false,
                                  "line1":"1 Previous Road",
                                  "line2":"Clifton",
                                  "line3":"York",
                                  "line4":"Yorkshire",
                                  "postalCode":"YO1 9EX",
                                  "countryCode":"ES"
                                }
                              }
                            }""")

  "A payload containing a director" must {
    "map correctly to a valid user answers director" when {
      lazy val transformedJson = desDirector.transform(directorOrPartnerTransformer.getDirectorOrPartner("director")).asOpt.value

      "We have director details" when {
        "We have a name" in {
          (transformedJson \ "directorDetails" \ "firstName").as[String].mustBe((userAnswersDirector \ "directorDetails" \ "firstName").as[String])
        }

        "We have a middle name" in {
          (transformedJson \ "directorDetails" \ "middleName").as[String].mustBe((userAnswersDirector \ "directorDetails" \ "middleName").as[String])
        }

        "We don't have a middle name" in {
          val inputJson = desDirector.as[JsObject] - "middleName"

          val transformedJson = inputJson.transform(directorOrPartnerTransformer.getDirectorOrPartner("director")).asOpt.value

          (transformedJson \ "directorDetails" \ "middleName").asOpt[String].mustBe(None)
        }

        "We have a last name" in {
          (transformedJson \ "directorDetails" \ "lastName").as[String].mustBe((userAnswersDirector \ "directorDetails" \ "lastName").as[String])
        }

        "We have a DOB" in {
          (transformedJson \ "dateOfBirth").as[String].mustBe((userAnswersDirector \ "dateOfBirth").as[String])
        }

        "We have a nino" in {
          (transformedJson \ "hasNino").as[Boolean].mustBe((userAnswersDirector \ "hasNino").as[Boolean])
          (transformedJson \ "nino" \ "value").as[String].mustBe((userAnswersDirector \ "nino" \ "value").as[String])
        }

        "We don't have nino but have a nino reason" in {
          val inputJson = desDirector.as[JsObject] - "nino"

          val transformedJson = inputJson.transform(directorOrPartnerTransformer.getDirectorOrPartner("director")).asOpt.value
          (transformedJson \ "hasNino").as[Boolean].mustBe(false)
          (transformedJson \ "noNinoReason").as[String].mustBe("test")
        }

        "We have a utr" in {
          (transformedJson \ "hasUtr").as[Boolean].mustBe((userAnswersDirector \ "hasUtr").as[Boolean])
          (transformedJson \ "utr" \ "value").as[String].mustBe((userAnswersDirector \ "utr" \ "value").as[String])
        }

        "We don't have a utr but have a reason" in {
          val inputJson = desDirector.as[JsObject] - "utr"

          val transformedJson = inputJson.transform(directorOrPartnerTransformer.getDirectorOrPartner("director")).asOpt.value
          (transformedJson \ "hasUtr").as[Boolean].mustBe(false)
          (transformedJson \ "noUtrReason").as[String].mustBe("test")
        }

        //TODO: DES has director address details as not mandatory but we have it as mandatory in frontend (correspondenceCommonDetails wrapper). Potential issues.
        "We have an address" in {
          (transformedJson \ "directorAddress" \ "addressLine1").asOpt[String].value.mustBe(
            (userAnswersDirector \ "directorAddress" \ "addressLine1").asOpt[String].value
          )
        }

        //TODO: Contact details is not mandatory in DES schema but mandatory in frontend. Potential issues.
        "We have a valid contact details" when {
          "with a telephone" in {
            (transformedJson \ "directorContactDetails" \ "phone").asOpt[String].value.mustBe(
              (userAnswersDirector \ "directorContactDetails" \ "phone").asOpt[String].value
            )
          }

          "with an email" in {
            (transformedJson \ "directorContactDetails" \ "email").asOpt[String].value.mustBe(
              (userAnswersDirector \ "directorContactDetails" \ "email").asOpt[String].value
            )
          }
        }

        "We have a previous address flag" in {
          (transformedJson \ "directorAddressYears").asOpt[String].value.mustBe(
            (userAnswersDirector \ "directorAddressYears").asOpt[String].value
          )
        }

        "We have a previous address" in {
          (transformedJson \ "directorPreviousAddress" \ "country").asOpt[String].value.mustBe(
            (userAnswersDirector \ "directorPreviousAddress" \ "country").asOpt[String].value
          )
        }

        "We have a previous address flag as false and no previous address" in {
          val inputJson = desDirector.as[JsObject] - "previousAddressDetails" + ("previousAddressDetails" ->
            Json.obj("isPreviousAddressLast12Month" -> JsBoolean(false)))

          val transformedJson = inputJson.transform(directorOrPartnerTransformer.getDirectorOrPartner("director")).asOpt.value

          (transformedJson \ "directorAddressYears").asOpt[String].value.mustBe("over_a_year")
          (transformedJson \ "directorPreviousAddress").asOpt[JsObject].mustBe(None)
        }

        "We have an array of directors" in {
          val directors = JsArray(Seq(desDirector, desDirector, desDirector, desDirector))

          val transformedJson = directors.transform(directorOrPartnerTransformer.getDirectorsOrPartners("director")).asOpt.value

          (transformedJson \ 0 \ "directorDetails" \ "firstName").as[String].mustBe((userAnswersDirector \ "directorDetails" \ "firstName").as[String])
          (transformedJson \ 1 \ "directorDetails" \ "firstName").as[String].mustBe((userAnswersDirector \ "directorDetails" \ "firstName").as[String])
          (transformedJson \ 2 \ "directorDetails" \ "firstName").as[String].mustBe((userAnswersDirector \ "directorDetails" \ "firstName").as[String])
          (transformedJson \ 3 \ "directorDetails" \ "firstName").as[String].mustBe((userAnswersDirector \ "directorDetails" \ "firstName").as[String])
        }

        "We have more than 10 directors" in {
          val partialPayload = Json.obj(
            "psaSubscriptionDetails" -> Json.obj(
              "customerIdentificationDetails" -> legalStatus,
                    "numberOfDirectorsOrPartnersDetails" -> moreThanTenFlag,
                    "directorOrPartnerDetails" -> JsArray(Seq.fill(10)(desDirector))
            )
          )

          val transformedJson = partialPayload.transform(directorOrPartnerTransformer.getDirectorsOrPartners).asOpt.value
          (transformedJson \ "moreThanTenDirectors").as[Boolean].mustBe(true)
        }
      }
    }
  }
}
