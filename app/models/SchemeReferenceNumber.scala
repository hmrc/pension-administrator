/*
 * Copyright 2023 HM Revenue & Customs
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

import models.SchemeReferenceNumber.stringToSchemeReferenceNumber
import play.api.libs.json.Json

import scala.language.implicitConversions
import scala.util.matching.Regex

case class SchemeReferenceNumber(id: String) {

  def apply(id: String): SchemeReferenceNumber = stringToSchemeReferenceNumber(id)

}

object SchemeReferenceNumber {

  val regexSRN: Regex = "^S[0-9]{10}$".r

  implicit def schemeReferenceNumberToString(srn: SchemeReferenceNumber): String =
    srn.id

  implicit def stringToSchemeReferenceNumber(srn: String): SchemeReferenceNumber = srn match {
    case regexSRN(_*) => SchemeReferenceNumber(srn)
    case _ => throw InvalidSchemeReferenceNumberException()
  }

  case class InvalidSchemeReferenceNumberException() extends Exception

  implicit val format = Json.format[SchemeReferenceNumber]

}
