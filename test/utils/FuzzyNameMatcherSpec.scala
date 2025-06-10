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

import org.scalacheck.{Gen, Shrink}
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import wolfendale.scalacheck.regexp.RegexpGen

class FuzzyNameMatcherSpec extends AnyFlatSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  import FuzzyNameMatcherSpec.*

  "fuzzify" should "uppercase all names" in {

    val gen = {
      for  {
        s <- validWordGen
      } yield FuzzifyTestCase(s, s.toUpperCase)
    }

    runFuzzifyTestCase(gen)

  }

  it should "remove all space" in {

    runFuzzifyTestCase(interleavedFuzzifyTestCaseGen(RegexpGen.from("\\s")))

  }

  it should "strip all characters except alphanumeric and space" in {

    runFuzzifyTestCase(interleavedFuzzifyTestCaseGen(punctuationGen))

  }

  it should "remove all special words" in {

    runFuzzifyTestCase(interleavedFuzzifyTestCaseGen(specialWordGen))

  }

  "matches" should "return true for two strings that fuzzify equally" in {

    FuzzyNameMatcher.matches("Abc 123 co. ltd", "abc 123").shouldBe(true)

  }

  it should "return false for two strings that do not fuzzify equally" in {

    FuzzyNameMatcher.matches("Abc 123 co. ltd", "def 123").shouldBe(false)

  }

  def runFuzzifyTestCase(gen: Gen[FuzzifyTestCase]): Assertion = {

    forAll(gen) {
      testCase =>
        FuzzyNameMatcher.fuzzify(testCase.name).shouldBe(testCase.fuzzified)
    }
  }
}

object FuzzyNameMatcherSpec {

  implicit val dontShrinkStrings: Shrink[String] = Shrink.shrinkAny

  case class FuzzifyTestCase(name: String, fuzzified: String)

  val validWordGen: Gen[String] =
    Gen.alphaNumStr.suchThat(s => !FuzzyNameMatcher.specialWords.contains(s.toUpperCase))

  val specialWordGen: Gen[String] = Gen.oneOf(FuzzyNameMatcher.specialWords)

  val punctuationGen: Gen[String] = RegexpGen.from("""[%&()*./:;<>?@\[\\\]^{}~]""")

  def interleavedFuzzifyTestCaseGen(invalids: Gen[String]): Gen[FuzzifyTestCase] = {
    for {
      keep <- Gen.listOf(validWordGen)
      strip <- Gen.listOfN(keep.length, invalids)
    } yield interleave(keep, strip)
  }

  def interleave(keep: List[String], strip: List[String]): FuzzifyTestCase = {
    FuzzifyTestCase(
      keep.zip(strip).map(tuple => tuple._1 + " " + tuple._2).mkString(" "),
      keep.mkString.replaceAll("\\s", "").toUpperCase
    )
  }
}
