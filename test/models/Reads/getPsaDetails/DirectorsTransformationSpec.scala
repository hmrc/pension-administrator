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
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.JsonTransformations.PSASubscriptionDetailsTransformer

class DirectorsTransformationSpec extends WordSpec with MustMatchers with OptionValues {

  "A payload containing a director" must {
    "map correctly to a valid user answers director" when {

      def doNothing: Reads[JsObject] = __.json.put(Json.obj())

      val getNino: Reads[JsObject] = {
        (__ \ "nino").read[String].flatMap {
          _ =>
            (__ \ 'directorNino \ 'nino).json.copyFrom((__ \ 'nino).json.pick) and
              (__ \ 'directorNino \ 'hasNino).json.put(JsBoolean(true)) reduce
        } orElse {
          (__ \ 'directorNino \ 'reason).json.copyFrom((__ \ 'noNinoReason).json.pick) and
            (__ \ 'directorNino \ 'hasNino).json.put(JsBoolean(false)) reduce
        }
      }

      val getUtr: Reads[JsObject] = {
        (__ \ "utr").read[String].flatMap {
          _ =>
            (__ \ 'directorUtr \ 'utr).json.copyFrom((__ \ 'utr).json.pick) and
              (__ \ 'directorUtr \ 'hasUtr).json.put(JsBoolean(true)) reduce
        } orElse {
          (__ \ 'directorUtr \ 'reason).json.copyFrom((__ \ 'noUtrReason).json.pick) and
            (__ \ 'directorUtr \ 'hasUtr).json.put(JsBoolean(false)) reduce
        }
      }

      val getDirectorcontactDetails: Reads[JsObject] = {
        (__ \ 'directorContactDetails \ 'phone).json.copyFrom((__ \ 'correspondenceCommonDetails \ 'contactDetails \ 'telephone).json.pick) and
          (__ \ 'directorContactDetails \ 'email).json.copyFrom((__ \ 'correspondenceCommonDetails \ 'contactDetails \ 'email).json.pick) reduce
      }

      val getDirector = (__ \ 'directorDetails \ 'firstName).json.copyFrom((__ \ 'firstName).json.pick) and
        ((__ \ 'directorDetails \ 'middleName).json.copyFrom((__ \ 'middleName).json.pick) orElse doNothing) and
        (__ \ 'directorDetails \ 'lastName).json.copyFrom((__ \ 'lastName).json.pick) and
        (__ \ 'directorDetails \ 'dateOfBirth).json.copyFrom((__ \ 'dateOfBirth).json.pick) and
        getNino and
        getUtr and
        PSASubscriptionDetailsTransformer.getAddress(__ \ "directorAddress", __ \ "correspondenceCommonDetails" \ "addressDetails") and
        getDirectorcontactDetails reduce


      val getDirectors: Reads[JsArray] = __.read(Reads.seq(getDirector)).map(JsArray(_))


      lazy val transformedJson = desDirector.transform(getDirector).asOpt.value

      "We have director details" when {
        "We have a name" in {
          (transformedJson \ "directorDetails" \ "firstName").as[String] mustBe (userAnswersDirector \ "directorDetails" \ "firstName").as[String]
        }

        "We have a middle name" in {
          (transformedJson \ "directorDetails" \ "middleName").as[String] mustBe (userAnswersDirector \ "directorDetails" \ "middleName").as[String]
        }

        "We don't have a middle name" in {
          val inputJson = desDirector.as[JsObject] - "middleName"

          val transformedJson = inputJson.transform(getDirector).asOpt.value

          (transformedJson \ "directorDetails" \ "middleName").asOpt[String] mustBe None
        }

        "We have a last name" in {
          (transformedJson \ "directorDetails" \ "lastName").as[String] mustBe (userAnswersDirector \ "directorDetails" \ "lastName").as[String]
        }

        "We have a DOB" in {
          (transformedJson \ "directorDetails" \ "dateOfBirth").as[String] mustBe (userAnswersDirector \ "directorDetails" \ "dateOfBirth").as[String]
        }

        "We have a nino" in {
          (transformedJson \ "directorNino" \ "hasNino").as[Boolean] mustBe (userAnswersDirector \ "directorNino" \ "hasNino").as[Boolean]
          (transformedJson \ "directorNino" \ "nino").as[String] mustBe (userAnswersDirector \ "directorNino" \ "nino").as[String]
        }

        "We don't have a nino" in {
          val inputJson = desDirector.as[JsObject] - "nino"

          val transformedJson = inputJson.transform(getDirector).asOpt.value

          (transformedJson \ "directorNino" \ "hasNino").as[Boolean] mustBe false
          //TODO: reason is not mandatory but mandatory in our frontend. Potential issues.
          (transformedJson \ "directorNino" \ "reason").as[String] mustBe "test"
        }

        "We have a utr" in {
          (transformedJson \ "directorUtr" \ "hasUtr").as[Boolean] mustBe (userAnswersDirector \ "directorUtr" \ "hasUtr").as[Boolean]
          (transformedJson \ "directorUtr" \ "utr").as[String] mustBe (userAnswersDirector \ "directorUtr" \ "utr").as[String]
        }

        "We don't have a utr" in {
          val inputJson = desDirector.as[JsObject] - "utr"

          val transformedJson = inputJson.transform(getDirector).asOpt.value

          (transformedJson \ "directorUtr" \ "hasUtr").as[Boolean] mustBe false
          //TODO: reason is not mandatory but mandatory in our frontend. Potential issues.
          (transformedJson \ "directorUtr" \ "reason").as[String] mustBe "test"
        }

        //TODO: DES has director address details as not mandatory but we have it as mandatory in frontend (correspondenceCommonDetails wrapper). Potential issues.
        "We have an address" in {
          (transformedJson \ "directorAddress" \ "addressLine1").asOpt[String].value mustBe (userAnswersDirector \ "directorAddress" \ "addressLine1").asOpt[String].value
        }

        //TODO: Contact details is not mandatory in DES schema but mandatory in frontend. Potential issues.
        "We have a valid contact details" when {
          "with a telephone" in {
            (transformedJson \ "directorContactDetails" \ "phone").asOpt[String].value mustBe (userAnswersDirector \ "directorContactDetails" \ "phone").asOpt[String].value
          }

          "with an email" in {
            (transformedJson \ "directorContactDetails" \ "email").asOpt[String].value mustBe (userAnswersDirector \ "directorContactDetails" \ "email").asOpt[String].value
          }
        }

        "We have an array of directors" in {
          val directors = JsArray(Seq(desDirector, desDirector, desDirector, desDirector))

          val transformedJson = directors.transform(getDirectors).asOpt.value

          (transformedJson \ 0 \ "directorDetails" \ "firstName").as[String] mustBe (userAnswersDirector \ "directorDetails" \ "firstName").as[String]
          (transformedJson \ 1 \ "directorDetails" \ "firstName").as[String] mustBe (userAnswersDirector \ "directorDetails" \ "firstName").as[String]
          (transformedJson \ 2 \ "directorDetails" \ "firstName").as[String] mustBe (userAnswersDirector \ "directorDetails" \ "firstName").as[String]
          (transformedJson \ 3 \ "directorDetails" \ "firstName").as[String] mustBe (userAnswersDirector \ "directorDetails" \ "firstName").as[String]
        }
      }
    }
  }

  val userAnswersDirector: JsValue = Json.parse(
    """      {
                     "directorDetails" : {
                       "firstName" : "Ann",
                       "middleName" : "Sarah",
                       "lastName" : "Baker",
                       "dateOfBirth" : "1980-03-01",
                       "isDeleted" : false
                     },
                     "directorNino" : {
                       "hasNino" : true,
                       "nino" : "JC000001A"
                     },
                     "directorUtr" : {
                       "hasUtr" : true,
                       "utr" : "0123456789"
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
                                  "countryCode":"GB"
                                }
                              }
                            }""")

}