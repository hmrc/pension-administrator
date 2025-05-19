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

package utils

import play.api.libs.json._
import scala.language.implicitConversions

class EnumUtils[E <: Enumeration](val en: E) {
  private def enumReads: Reads[en.Value] = new Reads[en.Value] {
    def reads(json: JsValue): JsResult[en.Value] = json match {
      case JsString(s) => {
        try {
          JsSuccess(en.withName(s))
        } catch {
          case _: NoSuchElementException => JsError(s"Enumeration expected of type: '${en.getClass}'," +
            s"but it does not appear to contain the value: '$s'")
        }
      }
      case _ => JsError("String value expected")
    }
  }

  implicit def enumWrites(): Writes[en.Value] = new Writes[en.Value] {
    def writes(v: en.Value): JsValue = JsString(v.toString)
  }

  implicit def enumFormat(): Format[en.Value] = {
    Format(enumReads, enumWrites())
  }
}
