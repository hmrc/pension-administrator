/*
 * Copyright 2024 HM Revenue & Customs
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

import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import play.api.libs.json.{JsArray, JsObject, Json}

import java.time.LocalDate

trait PsaSubscriptionDetailsGenerators {
  val legalStatus: Gen[String] = Gen.oneOf("Individual", "Partnership", "Limited Company")
  val idType: Gen[Option[String]] = Gen.option(Gen.oneOf("NINO", "UTR"))

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

  val psaContactDetailsGenerator: Gen[JsObject] = for {
    telephone <- Gen.numStr
    email <- Gen.option(Gen.alphaStr)
  } yield {
    Json.obj(
      "telephone" -> telephone,
      "email" -> email
    )
  }

  val addressGenerator: Gen[JsObject] = for {
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

  val utrGenerator: Gen[String] = {
    val utrRange: Gen[String] = Gen.listOfN[Char](randomNumberFromRange(10, 13), Gen.numChar).map(_.mkString)
    randomNumberFromRange(1, 3) match {
      case 1 => utrRange
      case 2 => "k" + utrRange
      case 3 => utrRange.toString + "k"
    }
  }

  private def randomNumberFromRange(min: Int, max: Int): Int = Gen.chooseNum(min, max).sample.fold(min)(c => c)

  val dateGenerator: Gen[LocalDate] = for {
    day <- Gen.choose(1, 28)
    month <- Gen.choose(1, 12)
    year <- Gen.choose(2000, 2018)
  } yield LocalDate.of(year, month, day)


  val titlesGenerator: Gen[String] = Gen.oneOf("Mr", "Mrs", "Miss", "Ms", "Dr", "Sir", "Professor", "Lord")

  val individualGenerator: Gen[JsObject] = for {
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


  val previousAddressGenerator: Gen[JsObject] = for {
    isPreviousAddressLast12Month <- arbitrary[Boolean]
    previousAddress <- Gen.option(addressGenerator)
  } yield {
    Json.obj("isPreviousAddressLast12Month" -> isPreviousAddressLast12Month,
      "previousAddress" -> previousAddress)
  }

  val correspondenceCommonDetailsGenerator: Gen[JsObject] = for {
    addressDetails <- addressGenerator
    contactDetails <- Gen.option(psaContactDetailsGenerator)
  } yield {
    Json.obj(
      "addressDetails" -> addressDetails,
      "contactDetails" -> contactDetails
    )
  }


  val psaDirectorOrPartnerDetailsGenerator: Gen[JsObject] = for {
    entityType <- Gen.oneOf("Director", "Partner")
    title <- Gen.option(titlesGenerator)
    firstName <- Gen.alphaStr
    middleName <- Gen.option(Gen.alphaStr)
    lastName <- Gen.alphaStr
    dateOfBirth <- dateGenerator
    nino <- Gen.alphaUpperStr
    utr <- utrGenerator
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


  val psaSubscriptionDetailsGenerator: Gen[JsObject] = for {
    isPSASuspension <- arbitrary[Boolean]
    customerIdentificationDetails <- customerIdentificationDetailsGenerator
    organisationOrPartnerDetails <- Gen.option(orgOrPartnerDetailsGenerator)
    individualDetails <- Gen.option(individualGenerator)
    correspondenceAddressDetails <- addressGenerator
    correspondenceContactDetails <- psaContactDetailsGenerator
    previousAddressDetails <- previousAddressGenerator
    directorOrPartnerDetails <- Gen.option(directorsOrPartners)
    pensionAdvisorDetails <- pensionAdvisorGenerator
  } yield {
    Json.obj(
      "isPSASuspension" -> isPSASuspension,
      "customerIdentificationDetails" -> customerIdentificationDetails,
      "organisationOrPartnerDetails" -> organisationOrPartnerDetails,
      "individualDetails" -> individualDetails,
      "correspondenceAddressDetails" -> correspondenceAddressDetails,
      "correspondenceContactDetails" -> correspondenceContactDetails,
      "previousAddressDetails" -> previousAddressDetails,
      "directorOrpartnerDetails" -> directorOrPartnerDetails,
      "declarationDetails" -> Json.obj("pensionAdvisorDetails" -> pensionAdvisorDetails)
    )
  }

  val psaDetailsGenerator: Gen[JsObject] = psaSubscriptionDetailsGenerator.map(
    psaSubscriptionDetails => Json.obj("psaSubscriptionDetails" -> psaSubscriptionDetails))
}


