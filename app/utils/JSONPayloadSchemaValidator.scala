/*
 * Copyright 2022 HM Revenue & Customs
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

import com.eclipsesource.schema.{JsonSource, SchemaValidator}
import play.api.libs.json._

import java.io.{File, FileInputStream}
import com.eclipsesource.schema.drafts.Version7._
import scala.collection.Seq


object ErrorDetailsExtractor {

  def getErrors(error: Seq[(JsPath, Seq[JsonValidationError])]): String = {
    val message = new StringBuilder("")
    error.flatMap(_._2).foldLeft(message){
      (stringBuilder, validationErrors) =>
        val json = Json.parse(validationErrors.args.mkString)
        val jsonTransformer = (__ \ 'schemaPath).json.pick
        val errorPath = json.transform(jsonTransformer)
        stringBuilder.append((errorPath.getOrElse(JsNull), validationErrors.message))
    }
    message.toString()
  }
}


class JSONPayloadSchemaValidator {
  private val basePath = System.getProperty("user.dir")

  def validateJsonPayload(jsonSchemaPath: String, data: JsValue): JsResult[JsValue] = {
    implicit val validator: SchemaValidator = SchemaValidator()
    val initialFile = new File(s"$basePath/conf/$jsonSchemaPath")
    val targetStream = new FileInputStream(initialFile)
    val jsonSchema = JsonSource.schemaFromStream(targetStream).get
    validator.validate(jsonSchema, data)
  }

}
