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

package models

import java.time.LocalDate

import play.api.libs.json.{JsBoolean, JsObject, JsString, Json}

trait Samples {

  val ukAddressSampleWithTwoLines = UkAddress("line1", Some("line2"), None, None, "GB", "NE1")
  val nonUkAddressSample = InternationalAddress("line1", Some("line2"), Some("line3"), Some("line4"), "IT", Some("NE1"))
  val ukAddressSample = UkAddress("line1", Some("line2"), Some("line3"), Some("line4"), "GB", "NE1")

}