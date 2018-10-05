/*
 * Copyright 2018 HM Revenue & Customs
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

package models.Reads.PsaSubscriptionDetails

import org.joda.time.LocalDate
import org.scalacheck.Gen
import org.scalacheck.Arbitrary._
import play.api.libs.json.{JsArray, JsBoolean, JsObject, Json}

trait PsaSubscriptionDetailsGenerators {
  val legalStatus: Gen[String] = Gen.oneOf("Individual","Partnership","Limited Company")
  val idType: Gen[Option[String]] = Gen.option(Gen.oneOf("NINO","UTR"))
  val booleanGen: Option[Boolean] = Gen.oneOf(true,false).sample

  val customerIdentificationDetailsGenerator: Gen[JsObject] = for {
    legalStatus <- legalStatus
    idType <- idType
    idNumber <- Gen.option(Gen.alphaStr)
    noIdentifier <- arbitrary[Boolean]
  } yield {
    Json.obj(
      "legalStatus" -> legalStatus,
      "idType" -> idType,
      "idNumber" -> idNumber,
      "noIdentifier" -> noIdentifier
    )
  }

  val orgOrPartnerDetailsGenerator: Gen[JsObject] = for {
    name <- Gen.alphaStr
    crnNumber <- Gen.option(Gen.alphaStr)
    vatRegistrationNumber <- Gen.option(Gen.alphaStr)
    payeReference <- Gen.option(Gen.alphaStr)
  } yield {
    Json.obj(
      "name" -> name,
      "crnNumber" -> crnNumber,
      "vatRegistrationNumber" -> vatRegistrationNumber,
      "payeReference" -> payeReference
    )
  }

  val psaContactDetailsGenerator : Gen[JsObject] = for {
    telephone <- Gen.numStr
    email <- Gen.option(Gen.alphaStr)
  } yield {
    Json.obj(
      "telephone" -> telephone,
      "email" -> email
    )
  }

  val addressGenerator : Gen[JsObject] = for {
    nonUkAddress <- arbitrary[Boolean]
    line1 <- Gen.alphaStr
    line2 <- Gen.alphaStr
    line3 <- Gen.option(Gen.alphaStr)
    line4 <- Gen.option(Gen.alphaStr)
    postalCode <- Gen.option(Gen.alphaStr)
    countryCode <- Gen.listOfN(2, Gen.alphaChar).map(_.mkString)
  } yield {
    Json.obj(
      "nonUKAddress" -> nonUkAddress,
      "line1" -> line1,
      "line2" -> line2,
      "line3" -> line3,
      "line4" -> line4,
      "postalCode" -> postalCode,
      "countryCode" -> countryCode
    )
  }

  val correspondenceDetailsGenerator: Gen[JsObject] = for {
    addressDetails <- addressGenerator
    contactDetails <- Gen.option(psaContactDetailsGenerator)
  } yield {
    Json.obj(
      "addressDetails" -> addressDetails,
      "contactDetails" -> contactDetails
    )
  }

  val dateGenerator: Gen[LocalDate] = for {
    day <- Gen.choose(1,28)
    month <-Gen.choose(1,12)
    year <-Gen.choose(2000,2018)
  } yield new LocalDate(year,month,day)


  val titlesGenerator: Gen[String] = Gen.oneOf("Mr","Mrs","Miss","Ms","Dr","Sir","Professor","Lord")

  val individualGenerator : Gen[JsObject] = for {
    title <- Gen.option(titlesGenerator)
    firstName <- Gen.alphaStr
    middleName <- Gen.option(Gen.alphaStr)
    lastName <- Gen.alphaStr
    dateOfBirth <- dateGenerator
  } yield {
    Json.obj(
      "title" -> title,
      "firstName" -> firstName,
      "middleName" -> middleName,
      "lastName" -> lastName,
      "dateOfBirth" -> dateOfBirth
    )
  }


  val previousAddressGenerator : Gen[JsObject] = for {
    isPreviousAddressLast12Month <- arbitrary[Boolean]
    previousAddress <- Gen.option(addressGenerator)
  } yield {
    Json.obj("isPreviousAddressLast12Month" -> isPreviousAddressLast12Month,
    "previousAddress" -> previousAddress)
  }

  val correspondenceCommonDetailsGenerator : Gen[JsObject] = for {
    addressDetails <- addressGenerator
    contactDetails <- Gen.option(psaContactDetailsGenerator)
  } yield {
    Json.obj(
      "addressDetails" -> addressDetails,
      "contactDetails" -> contactDetails
    )
  }


  val psaDirectorOrPartnerDetailsGenerator : Gen[JsObject] = for {
    entityType <- Gen.oneOf("Director","Partner")
    title <- Gen.option(titlesGenerator)
    firstName <- Gen.alphaStr
    middleName <- Gen.option(Gen.alphaStr)
    lastName <- Gen.alphaStr
    dateOfBirth <- dateGenerator
    nino <- Gen.alphaUpperStr
    utr <- Gen.alphaUpperStr
    previousAddressDetails <- previousAddressGenerator
    correspondenceCommonDetails <- Gen.option(correspondenceCommonDetailsGenerator)
  } yield {
    Json.obj(
    "entityType" -> entityType,
    "title" -> title,
    "firstName" -> firstName,
    "middleName" -> middleName,
    "lastName" -> lastName,
    "dateOfBirth" -> dateOfBirth,
    "nino" -> nino,
    "utr" -> utr,
    "previousAddressDetails" -> previousAddressDetails,
    "correspondenceCommonDetails" -> correspondenceCommonDetails)
  }

  val pensionAdvisorGenerator: Gen[JsObject] = for {
    name <- Gen.alphaStr
    addressDetails <- addressGenerator
    contactDetails <- Gen.option(psaContactDetailsGenerator)
  } yield {
    Json.obj(
      "name" -> name,
      "addressDetails" -> addressDetails,
      "contactDetails" -> contactDetails
    )
  }

  val directorsOrPartners = JsArray(Gen.listOf(psaDirectorOrPartnerDetailsGenerator).sample.get)


  val psaSubscriptionDetailsGenerator = Json.obj("psaSubscriptionDetails" -> Json.obj("isPSASuspension" -> booleanGen, "customerIdentificationDetails" -> customerIdentificationDetailsGenerator.sample,
    "organisationOrPartnerDetails" -> Gen.option(orgOrPartnerDetailsGenerator).sample,
  "individualDetails" -> Gen.option(individualGenerator).sample, "correspondenceAddressDetails" -> addressGenerator.sample,
  "correspondenceContactDetails" -> psaContactDetailsGenerator.sample,
    "previousAddressDetails" -> previousAddressGenerator.sample,
  "directorOrPartnerDetails" -> Gen.option(directorsOrPartners).sample,
  "declarationDetails" -> Json.obj("pensionAdvisorDetails" -> Gen.option(pensionAdvisorGenerator).sample)))

  val newPsaDirectorOrPartnerDetailsGenerator = for {
    entityType <- Gen.oneOf("Director","Partner")
    title <- Gen.option(titlesGenerator)
    firstName <- Gen.alphaStr
    middleName <- Gen.option(Gen.alphaStr)
    lastName <- Gen.alphaStr
    dateOfBirth <- dateGenerator
    nino <- Gen.alphaUpperStr
    utr <- Gen.alphaUpperStr
    previousAddressDetails <- for {
      bool <- arbitrary[Boolean]
      address <- Gen.option(addressGenerator)
    } yield {
      Json.obj(
        "isPreviousAddressLast12Month" -> bool,
        "previousAddress" -> address
      )
    }
    correspondenceCommonDetails <- for {
      address <- Gen.option(addressGenerator)
      contactDetails <- Gen.option(psaContactDetailsGenerator)
    } yield Json.obj(
      "addressDetails" -> address,
      "contactDetails" -> contactDetails
    )
  } yield Json.obj(
    "entityType" -> entityType,
    "title" -> title,
    "firstName" -> firstName,
    "middleName" -> middleName,
    "lastName" -> lastName,
    "dateOfBirth" -> dateGenerator.sample,
    "nino" -> Gen.alphaUpperStr.sample,
    "utr" -> Gen.alphaUpperStr.sample,
    "previousAddressDetails" -> Json.obj("isPreviousAddressLast12Month" -> booleanGen,
      "previousAddress" -> Gen.option(addressGenerator).sample),
    "correspondenceCommonDetails" -> Gen.option(Json.obj("addressDetails" -> addressGenerator.sample, "contactDetails" -> Gen.option(psaContactDetailsGenerator).sample)).sample)
}


