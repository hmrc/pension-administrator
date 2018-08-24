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

package utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.ImplementedBy
import com.josephpconley.jsonpath.JSONPath
import com.networknt.schema.{JsonSchema, JsonSchemaFactory, ValidationMessage}
import javax.inject.Inject
import play.api.LoggerLike
import play.api.libs.json._

import scala.collection.JavaConverters._

@ImplementedBy(classOf[InvalidPayloadHandlerImpl])
trait InvalidPayloadHandler {

  def getFailures(schemaFileName: String, json: JsValue): Set[ValidationFailure]

  def logFailures(schemaFileName: String, json: JsValue): Unit

}

class InvalidPayloadHandlerImpl @Inject()(logger: LoggerLike) extends InvalidPayloadHandler {

  private[utils] def loadSchema(schemaFileName: String): JsonSchema = {
    val schemaUrl = getClass.getResource(schemaFileName)
    val factory = JsonSchemaFactory.getInstance()
    factory.getSchema(schemaUrl)
  }

  override def getFailures(schemaFileName: String, json: JsValue): Set[ValidationFailure] = {

    val schema = loadSchema(schemaFileName)
    getFailures(schema, json)

  }

  private[utils] def getFailures(schema: JsonSchema, json: JsValue): Set[ValidationFailure] = {

    val mapper = new ObjectMapper()
    val jsonNode = mapper.readTree(json.toString())

    val set = schema.validate(jsonNode).asScala.toSet

    set.map {
      message =>
        val value = valueFromJson(message, json)
        ValidationFailure(message.getType, message.getMessage, value)
    }

  }

  override def logFailures(schemaFileName: String, json: JsValue): Unit = {

    val schema = loadSchema(schemaFileName)
    logFailures(schema, json)

  }

  private[utils] def logFailures(schema: JsonSchema, json: JsValue): Unit = {

    val failures = getFailures(schema, json)
    val msg = new StringBuilder()

    msg.append("Invalid Paylod JSON Failures\n")
    msg.append(s"Failures: ${failures.size}\n")
    msg.append("\n")

    failures.foreach {
      failure =>
        msg.append(s"${failure.message}\n")
        msg.append(s"Type: ${failure.failureType}\n")
        msg.append(s"Value: ${failure.value.getOrElse("[none]")}\n")
        msg.append("\n")
    }

    logger.warn(msg.toString())

  }

  private def valueFromJson(message: ValidationMessage, json: JsValue): Option[String] = {
    message.getType match {
      case "enum" | "format" | "maximum" | "maxLength" | "minimum" | "minLength" | "pattern" | "type" =>
        JSONPath.query(message.getPath, json) match {
          case JsBoolean(bool) => Some(bool.toString)
          case JsNull => Some("null")
          case JsNumber(n) => Some(depersonalise(n.toString))
          case JsString(s) => Some(depersonalise(s))
          case _ => None
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

case class ValidationFailure(failureType: String, message: String, value: Option[String])
