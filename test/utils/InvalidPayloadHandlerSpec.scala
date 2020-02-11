/*
 * Copyright 2020 HM Revenue & Customs
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

import java.text.MessageFormat

import base.SpecBase
import com.networknt.schema.{ErrorMessageType, ValidationMessage}
import play.api.libs.json.{JsNull, JsNumber, JsValue, Json}

class InvalidPayloadHandlerSpec extends SpecBase {
  private val emt = new ErrorMessageType {
    override def getErrorCode: String = ""
    override def getMessageFormat: MessageFormat = new MessageFormat("")
  }

  private val vm = ValidationMessage.of("enum", emt, "$.abc")

  "valueFromJson" should {
    "return the correct value for a jsnull" in {
      val testJson = Json.obj("abc" -> JsNull)
      val result = InvalidPayloadHandlerImpl.valueFromJson(message = vm , json = testJson)
      result mustBe Some("null")
    }
    "return the correct value for a jsnumber" in {
      val testJson = Json.obj("abc" -> JsNumber(22))
      val result = InvalidPayloadHandlerImpl.valueFromJson(message = vm , json = testJson)
      result mustBe Some("99")
    }

    "return none for a non-valid type" in {
      val vm = ValidationMessage.of("blah", emt, "$.abc")
      val testJson = Json.obj("abc" -> JsNumber(22))
      val result = InvalidPayloadHandlerImpl.valueFromJson(message = vm , json = testJson)
      result mustBe None
    }
  }
}
