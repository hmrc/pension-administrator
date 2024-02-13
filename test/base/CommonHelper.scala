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

package base

import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsDefined, JsValue}

trait CommonHelper extends Matchers{

  def testElementValue(jsValue: JsValue, elementName: String, expectedValue: String): Unit = {
    jsValue \ elementName match {
      case JsDefined(v) =>v.as[String] mustBe expectedValue
      case _ =>
        throw new RuntimeException("Element does not exist")
    }
  }

  def testElementValue(jsValue: JsValue, elementName: String, expectedValue: JsValue): Unit = {
    jsValue \ elementName match {
      case JsDefined(v) =>
        v.toString() mustBe expectedValue.toString()
      case _ =>
        throw new RuntimeException("Element does not exist")

    }
  }


}
