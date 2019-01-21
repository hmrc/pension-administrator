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

class DirectorsTransformationSpec extends WordSpec with MustMatchers with OptionValues {

  "A payload containing a director" must {
    "map correctly to a valid user answers director" when {

      def doNothing: Reads[JsObject] = __.json.put(Json.obj())

      val getDirectors = (__ \ 'directorDetails \ 'firstName).json.copyFrom((__ \ 'firstName).json.pick) and
        ((__ \ 'directorDetails \ 'middleName).json.copyFrom((__ \ 'middleName).json.pick) orElse doNothing) and
        (__ \ 'directorDetails \ 'lastName).json.copyFrom((__ \ 'lastName).json.pick) and
        (__ \ 'directorDetails \ 'dateOfBirth).json.copyFrom((__ \ 'dateOfBirth).json.pick) reduce

      lazy val transformedJson = desDirector.transform(getDirectors).asOpt.value

      "We have director details" when {
        "We have a name" in {
          (transformedJson \ "directorDetails" \ "firstName").as[String] mustBe (userAnswersDirector \ "directorDetails" \ "firstName").as[String]
        }

        "We have a middle name" in {
          (transformedJson \ "directorDetails" \ "middleName").as[String] mustBe (userAnswersDirector \ "directorDetails" \ "middleName").as[String]
        }

        "We don't have a middle name" in {
          val inputJson = desDirector.as[JsObject] - "middleName"

          val transformedJson = inputJson.transform(getDirectors).asOpt.value

          (transformedJson \ "directorDetails" \ "middleName").asOpt[String] mustBe None
        }

        "We have a last name" in {
          (transformedJson \ "directorDetails" \ "lastName").as[String] mustBe (userAnswersDirector \ "directorDetails" \ "lastName").as[String]
        }

        "We have a DOB" in {
          (transformedJson \ "directorDetails" \ "dateOfBirth").as[String] mustBe (userAnswersDirector \ "directorDetails" \ "dateOfBirth").as[String]
        }
      }
    }
  }

  val userAnswersDirector = Json.parse(
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
                       "nino" : "CS700100A"
                     },
                     "directorUtr" : {
                       "hasUtr" : true,
                       "utr" : "1234567890"
                     },
                     "companyDirectorAddressList" : {
                       "addressLine1" : "2 Other Place",
                       "addressLine2" : "Some District",
                       "addressLine3" : "Anytown",
                       "addressLine4" : "Somerset",
                       "postalCode" : "ZZ1 1ZZ",
                       "countryCode" : "GB"
                     },
                     "directorAddress" : {
                       "addressLine1" : "2 Other Place",
                       "addressLine2" : "Some District",
                       "addressLine3" : "Anytown",
                       "addressLine4" : "Somerset",
                       "postcode" : "ZZ1 1ZZ",
                       "country" : "GB"
                     },
                     "directorAddressYears" : "under_a_year",
                     "directorPreviousAddressList" : {
                       "addressLine1" : "2 Other Place",
                       "addressLine2" : "Some District",
                       "addressLine3" : "Anytown",
                       "addressLine4" : "Somerset",
                       "postalCode" : "ZZ1 1ZZ",
                       "countryCode" : "GB"
                     },
                     "directorPreviousAddress" : {
                       "addressLine1" : "2 Other Place",
                       "addressLine2" : "Some District",
                       "addressLine3" : "Anytown",
                       "addressLine4" : "Somerset",
                       "postcode" : "ZZ1 1ZZ",
                       "country" : "GB"
                     },
                     "directorContactDetails" : {
                       "email" : "s@S.com",
                       "phone" : "436"
                     },
                     "isDirectorComplete" : true
                   }""")

  val desDirector = Json.parse(
    """      {
                              "sequenceId":"000",
                              "entityType":"Director",
                              "title":"Mrs",
                              "firstName":"Ann",
                              "middleName":"Sarah",
                              "lastName":"Baker",
                              "dateOfBirth":"1980-03-01",
                              "nino":"JC000001A",
                              "utr":"0123456789",
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
