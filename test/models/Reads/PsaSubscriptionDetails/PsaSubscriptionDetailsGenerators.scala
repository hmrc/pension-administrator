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
import play.api.libs.json.{JsBoolean, JsObject, Json}

trait PsaSubscriptionDetailsGenerators {
  val legalStatus: Gen[String] = Gen.oneOf("Individual","Partnership","Limited Company")
  val idType: Gen[Option[String]] = Gen.option(Gen.oneOf("NINO","UTR"))

  val customerIdentificationDetailsGenerator: JsObject = Json.obj("legalStatus" -> legalStatus.sample,
    "idType" -> idType.sample,
    "idNumber" -> Gen.option(Gen.alphaStr).sample,
    "noIdentifier" -> Gen.oneOf(true,false).sample)

  val orgOrPartnerDetailsGenerator  = Json.obj("name" -> Gen.alphaStr.sample,
    "crnNumber" -> Gen.option(Gen.alphaStr).sample,
    "vatRegistrationNumber" -> Gen.option(Gen.alphaStr).sample,
    "payeReference" -> Gen.option(Gen.alphaStr).sample)

  val contactDetails: JsObject = Json.obj("telephone" -> Gen.numStr.sample,
    "email" -> Gen.option(Gen.alphaStr).sample)

  val psaContactDetailsGenerator = Json.obj("telephone" -> Gen.numStr.sample,
    "email" -> Gen.option(Gen.alphaStr).sample)

  val address: JsObject = Json.obj("nonUKAddress" -> Gen.oneOf(JsBoolean(true),JsBoolean(false)).sample,
    "line1" -> Gen.alphaStr.sample,
    "line2" -> Gen.alphaStr.sample,
    "line3" -> Gen.option(Gen.alphaStr.sample).sample,
    "line4" -> Gen.option(Gen.alphaStr.sample).sample,
    "postalCode" -> Gen.option(Gen.alphaStr.sample).sample,
    "countryCode" -> Gen.alphaStr.sample)

  val correspondenceDetailsGenerator: JsObject = Json.obj("addressDetails" -> address,
    "contactDetails" -> Gen.option(contactDetails).sample)

  val date: Gen[LocalDate] = for {
    day <- Gen.choose(1,28)
    month <-Gen.choose(1,12)
    year <-Gen.choose(2000,2018)
  } yield new LocalDate(year,month,day)


  val titles: Gen[String] = Gen.oneOf("Mr","Mrs","Miss","Ms","Dr","Sir","Professor","Lord")
  val individualGenerator: JsObject = Json.obj("title" -> Gen.option(titles).sample,
    "firstName" -> Gen.alphaStr.sample,
    "middleName" -> Gen.option(Gen.alphaStr).sample,
    "lastName" -> Gen.alphaStr.sample,
    "dateOfBirth" -> date.sample)

  val psaDirectorOrPartnerDetailsGenerator = Json.obj("entityType" -> Gen.oneOf("Director","Partner").sample,
    "title" -> Gen.option(titles).sample,
    "firstName" -> Gen.alphaStr.sample,
    "middleName" -> Gen.option(Gen.alphaStr).sample,
    "lastName" -> Gen.alphaStr.sample,
    "dateOfBirth" -> date.sample,
    "nino" -> Gen.alphaUpperStr.sample,
    "utr" -> Gen.alphaUpperStr.sample,
    "previousAddressDetails" -> Json.obj("isPreviousAddressLast12Month" -> Gen.oneOf(true,false).sample,
      "previousAddress" -> Gen.option(address).sample),
    "correspondenceCommonDetails" -> Gen.option(Json.obj("addressDetails" -> address, "contactDetails" -> Gen.option(contactDetails).sample)).sample)

  val pensionAdvisorGenerator = Json.obj("name" -> Gen.alphaStr.sample, "addressDetails" -> address, "contactDetails" -> Gen.option(psaContactDetailsGenerator).sample)
}