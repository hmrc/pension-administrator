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

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.{JsonSchemaFactory, SpecVersion, ValidationMessage}
import play.api.libs.json._

import scala.jdk.CollectionConverters.CollectionHasAsScala

class JSONPayloadSchemaValidator {

  def validateJsonPayload(jsonSchemaPath: String, data: JsValue): Set[ValidationFailure] = {
    val schemaUrl = getClass.getResourceAsStream(jsonSchemaPath)
    val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
    val schema = factory.getSchema(schemaUrl)
    val mapper = new ObjectMapper()
    val jsonNode = mapper.readTree(data.toString())

    val set = schema.validate(jsonNode).asScala.toSet

    set.map {
      message =>
        val value = valueFromJson(message, data)
        ValidationFailure(message.getType, message.getMessage, value)
    }
  }

  private def valueFromJson(message: ValidationMessage, json: JsValue): Option[String] = {
    message.getType match {
      case "enum" | "format" | "maximum" | "maxLength" | "minimum" | "minLength" | "pattern" | "type" =>
        (json \ message.getMessageKey).toEither match {
          case Right(jsValue) =>
            jsValue match {
              case JsBoolean(bool) => Some(bool.toString)
              case JsNull => Some("null")
              case JsNumber(n) => Some(depersonalise(n.toString))
              case JsString(s) => Some(depersonalise(s))
              case _ => None
            }
          case Left(_) => None
        }
      case _ => None
    }
  }


  private def depersonalise(value: String): String = {
    value
      .replaceAll("[a-zA-Z]", "x")
      .replaceAll("[0-9]", "9")
  }

}

