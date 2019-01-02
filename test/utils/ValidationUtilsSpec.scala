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

package utils

import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsResultException, Json, OFormat}
import utils.validationUtils._

class ValidationUtilsSpec extends WordSpec with MustMatchers with OptionValues {

  case class TestDetails(first: String, second: Option[String])

  object TestDetails {
    implicit val formats = Json.format[TestDetails]
  }

  case class Test(testFirst: String, testLast: String, testDetails: Option[TestDetails])

  object Test {
    implicit val formats: OFormat[Test] = Json.format[Test]
  }

  "convertTo " must {
    "convert the jsValue to the appropriate type successfully" in {
      val json = Json.obj(
        "testFirst" -> "first",
        "testLast" -> "last",
        "testDetails" -> Json.obj(
          "first" -> "dummyFirst",
          "second" -> "optionalSecond"
        )
      )
      json.convertTo[Test] mustEqual Test("first", "last", Some(TestDetails("dummyFirst", Some("optionalSecond"))))
    }

    "throw the JsResultException if the jsValue cannot be converted to the required type" in {
      val json = Json.obj(
        "testFirst" -> "wrong"
      )
      intercept[JsResultException] {
        json.convertTo[Test]
      }
    }
  }

  "convertAsOpt " must {
    "convert the jsLookupResult to the appropriate type successfully if present" in {
      val json = Json.obj(
        "testFirst" -> "first",
        "testLast" -> "last",
        "testDetails" -> Json.obj(
          "first" -> "dummyFirst",
          "second" -> "optionalSecond"
        )
      )
      (json \ "testDetails").convertAsOpt[TestDetails].value mustEqual TestDetails("dummyFirst", Some("optionalSecond"))
    }

    "convert the jsLookupResult to None if not present" in {
      val json = Json.obj(
        "testFirst" -> "first",
        "testLast" -> "last"
      )
      (json \ "testDetails").convertAsOpt[TestDetails] mustEqual None
    }

    "throw the JsResultException if the jsLookupResult cannot be converted to the appropriate type " in {
      val json = Json.obj(
        "testFirst" -> "first",
        "testLast" -> "last",
        "testDetails" -> Json.obj(
          "second" -> "optionalSecond"
        )
      )
      intercept[JsResultException] {
        (json \ "testDetails").convertAsOpt[Test]
      }
    }
  }
}
