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

package utils

import com.networknt.schema._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsNull, JsNumber, Json}

import java.text.MessageFormat
import java.util
import java.util.function.Supplier

class InvalidPayloadHandlerSpec extends AnyWordSpec with Matchers {
  def mockValidationMessage(messageType: String,
                            code: String,
                            evaluationPath: JsonNodePath,
                            schemaLocation: SchemaLocation,
                            instanceLocation: JsonNodePath,
                            property: String,
                            arguments: Array[Object],
                            details: util.Map[String, Object],
                            format: MessageFormat,
                            message: String,
                            messageSupplier: Supplier[String],
                            messageKey: String
                           ): ValidationMessage = {
    val vmBuilder = new ValidationMessage.Builder()
    vmBuilder
      .`type`(messageType)
      .code(code)
      .evaluationPath(evaluationPath)
      .schemaLocation(schemaLocation)
      .instanceLocation(instanceLocation)
      .property(property)
      .arguments(arguments)
      .details(details)
      .format(format)
      .message(message)
      .messageSupplier(messageSupplier)
      .messageKey(messageKey)
      .build()
  }
  def myFormatter(arg: String): String = {
    val format = arg.toLowerCase()
    format
  }
  val mockMessage = mockValidationMessage("enum",
    "", new JsonNodePath(PathType.JSON_PATH), new SchemaLocation(new AbsoluteIri("abc")), new JsonNodePath(PathType.JSON_PATH),
    "abcde", Array(Json.obj()), new util.HashMap[String, Object](), new MessageFormat("abc"), "abc", () => "", "abc")

  "valueFromJson" should {
        "return the correct value for a jsnull" in {
          val testJson = Json.obj("abc" -> JsNull)
          val result = InvalidPayloadHandlerImpl.valueFromJson(message = mockMessage, json = testJson)
          result mustBe Some("null")
        }
        "return the correct value for a jsnumber" in {
          val testJson = Json.obj("abc" -> JsNumber(22))
          val result = InvalidPayloadHandlerImpl.valueFromJson(message = mockMessage, json = testJson)
          result mustBe Some("99")
        }

        "return none for a non-valid type" in {
          val mockMessage = mockValidationMessage("blah",
            "", new JsonNodePath(PathType.JSON_PATH), new SchemaLocation(new AbsoluteIri("abc")), new JsonNodePath(PathType.JSON_PATH),
            "abcde", Array(Json.obj()), new util.HashMap[String, Object](), new MessageFormat("abc"), "abc", () => "", "abc")
          val testJson = Json.obj("abc" -> JsNumber(22))
          val result = InvalidPayloadHandlerImpl.valueFromJson(message = mockMessage, json = testJson)
          result mustBe None
        }
  }
}

