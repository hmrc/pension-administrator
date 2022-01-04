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

import scala.annotation.tailrec

object FuzzyNameMatcher {

  def matches(name1: String, name2: String): Boolean = {
    fuzzify(name1).equals(fuzzify(name2))
  }

  def fuzzify(name: String): String = {

    strip(
      name
        .toUpperCase
        .replaceAll("[^a-zA-Z0-9 ]", ""),
      specialWords
    ).replaceAll("\\s", "")

  }

  @tailrec
  private def strip(name: String, specials: List[String]): String = {
    specials match {
      case Nil => name
      case h :: t => strip(name.replaceAll(s"\\b$h\\b", ""), t)
    }
  }

  val specialWords = List(
    "AND",
    "CCC",
    "CIC",
    "CO",
    "COMPANIES",
    "COMPANY",
    "CORP",
    "CORPORATION",
    "INC",
    "INCORPORATED",
    "LIMITED",
    "LLP",
    "LP",
    "LTD",
    "PARTNERSHIP",
    "PLC",
    "THE",
    "ULTD",
    "UNLIMITED",
    "UNLTD"
  )

}
