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

package models

import models.PSTR.stringToPstr
import play.api.libs.json.Json

import scala.language.implicitConversions
import scala.util.matching.Regex

case class PSTR(id: String) {

  def apply(id: String): PSTR = stringToPstr(id)

}

object PSTR {

  val regexPSTR: Regex = "^[0-9]{8}[A-Z]{2}$".r

  implicit def pstrNumberToString(pstr: PSTR): String =
    pstr.id

  implicit def stringToPstr(pstr: String): PSTR = pstr match {
    case regexPSTR(_*) => PSTR(pstr)
    case _ => throw InvalidPstrNumberException()
  }

  case class InvalidPstrNumberException() extends Exception

  implicit val format = Json.format[PSTR]

}
