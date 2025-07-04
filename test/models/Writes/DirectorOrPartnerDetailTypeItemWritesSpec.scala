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

package models.Writes

import base.CommonHelper
import models.{CorrespondenceCommonDetail, DirectorOrPartnerDetailTypeItem, PreviousAddressDetails, Samples}
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

class DirectorOrPartnerDetailTypeItemWritesSpec extends AnyWordSpec with Matchers with OptionValues with CommonHelper with Samples {
  "An object of a partner detail type item" should {
    Seq("Director", "Partner").foreach { personType =>
      s"Map $personType previousaddressdetails inner object as `previousaddressdetail`" when {
        "required" in {
          val result = Json.toJson(directorOrPartnerSample(personType).copy(
            previousAddressDetail = PreviousAddressDetails(true, Some(ukAddressSample))))(using DirectorOrPartnerDetailTypeItem.psaSubmissionWrites)

          result.toString().must(include("true,\"previousAddressDetail\":"))
        }
      }

      s"Map $personType details object " when {
        val result = Json.toJson(directorOrPartnerSample(personType).copy(
          previousAddressDetail = PreviousAddressDetails(true, Some(ukAddressSample))))(using DirectorOrPartnerDetailTypeItem.psaUpdateWrites)

        Seq(
          ("sequenceId", "000"),
          ("entityType", personType),
          ("firstName", "John"),
          ("middleName", "Does Does"),
          ("lastName", "Doe"),
          ("dateOfBirth", "2019-01-31"),
          ("nino", "SL211111A"),
          ("noNinoReason", "he can't find it"),
          ("utr", "123456789"),
          ("noUtrReason", "he can't find it")).foreach { testElement =>
  
          s"testing for element ${testElement._1} having value ${testElement._2}" in {
            testElementValue(result, elementName = testElement._1, expectedValue = testElement._2)
          }
        }
      }

      s"Map $personType details object " when {
        val result = Json.toJson(directorOrPartnerSample(personType).copy(
          previousAddressDetail = PreviousAddressDetails(true, Some(ukAddressSample))))(using DirectorOrPartnerDetailTypeItem.psaUpdateWrites)

        Seq(
          ("correspondenceCommonDetails", Json.toJson(correspondenceCommonDetails)(using CorrespondenceCommonDetail.psaUpdateWrites)))
          .foreach { testElement =>
          s"testing for element ${testElement._1} having value ${testElement._2}" in {
            testElementValue(result, elementName = testElement._1, expectedValue = testElement._2)
          }
        }
      }
    }
  }
}
