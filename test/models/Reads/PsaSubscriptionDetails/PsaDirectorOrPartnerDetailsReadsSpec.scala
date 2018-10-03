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

import models.Samples
import org.joda.time.LocalDate
import org.scalacheck.Gen
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsPath, Json, Reads, Writes}
import play.api.libs.functional.syntax._

class PsaDirectorOrPartnerDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "A payload containing details for a Director or a Partner" should {
    "parse correctly to a PsaDirectorOrPartnerDetails object" when {

      val date: Gen[LocalDate] = for {
        day <- Gen.choose(1,28)
        month <-Gen.choose(1,12)
        year <-Gen.choose(2000,2018)
      } yield new LocalDate(year,month,day)

      val input = Json.obj("entityType" -> Gen.oneOf("Director","Partner").sample,
      "title" -> Gen.option(Gen.oneOf("Mr","Mrs","Miss","Ms","Dr","Sir","Professor","Lord")).sample,
      "firstName" -> Gen.alphaStr.sample,
      "middleName" -> Gen.option(Gen.alphaStr).sample,
      "lastName" -> Gen.alphaStr.sample,
      "dateOfBirth" -> date.sample,
      "nino" -> Gen.alphaUpperStr.sample,
      "utr" -> Gen.alphaUpperStr.sample)

      val output = input.as[PsaDirectorOrPartnerDetails]

      "we have an entity type" in {
        output.detailsForDirectorOrPartner mustBe (input \ "entityType").as[String]
      }

      "we have an optional title" in {
        output.title mustBe (input \ "title").asOpt[String]
      }

      "we have a first name" in {
        output.firstName mustBe (input \ "firstName").as[String]
      }

      "we have an optional middle name" in {
        output.middleName mustBe (input \ "middleName").asOpt[String]
      }

      "we have a surname" in {
        output.lastName mustBe (input \ "lastName").as[String]
      }

      "we have a dob" in {
        output.dateOfBirth.toString() mustBe (input \ "dateOfBirth").as[String]
      }

      "we have an optional nino" in {
        output.nino mustBe (input \ "nino").asOpt[String]
      }

      "we have an optional utr" in {
        output.utr mustBe (input \ "utr").asOpt[String]
      }
    }
  }
}

case class PsaDirectorOrPartnerDetails(detailsForDirectorOrPartner: String,
                                       title: Option[String],
                                       firstName: String,
                                       middleName: Option[String],
                                       lastName: String,
                                       dateOfBirth: LocalDate,
                                       nino: Option[String],
                                       utr: Option[String])

object PsaDirectorOrPartnerDetails {
  implicit val writes : Writes[PsaDirectorOrPartnerDetails] = Json.writes[PsaDirectorOrPartnerDetails]
  implicit val reads : Reads[PsaDirectorOrPartnerDetails] = (
    (JsPath \ "entityType").read[String] and
      (JsPath \ "title").readNullable[String] and
      (JsPath \ "firstName").read[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "dateOfBirth").read[LocalDate] and
      (JsPath \ "nino").readNullable[String] and
      (JsPath \ "utr").readNullable[String]
  )((entityType,title,name,middleName,surname,dob,nino,utr) => PsaDirectorOrPartnerDetails(entityType,title,name,middleName,surname,dob,nino,utr))
}


