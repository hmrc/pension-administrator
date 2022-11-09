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

package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import play.api.mvc.PathBindable

class FeatureToggleSpec
  extends AnyFreeSpec
    with ScalaCheckPropertyChecks
    with Matchers
    with EitherValues {

  implicit private val arbitraryFeatureToggleName: Arbitrary[FeatureToggleName] = Arbitrary {
    Gen.oneOf(FeatureToggleName.toggles)
  }

  val toggleGen: Gen[FeatureToggle] = for {
    name <- arbitrary[FeatureToggleName]
    enabled <- arbitrary[Boolean]
  } yield FeatureToggle(name, enabled)

  "Should round trip through json correctly" in {
    forAll(toggleGen) {
      toggle =>
        Json.toJson(toggle).as[FeatureToggle] mustBe toggle
    }
  }

  "Feature flag name" - {

    val pathBindable = implicitly[PathBindable[FeatureToggleName]]

    "must round trip all valid values from json" in {

      forAll(arbitrary[FeatureToggleName]) {
        name =>
          Json.toJson(name).as[FeatureToggleName] mustBe name
      }
    }

    "must bind from a URL" in {

      forAll(arbitrary[FeatureToggleName]) {
        name =>
          pathBindable.bind("key", name.asString).value mustEqual name

      }
    }

    "must unbind to a URL" in {

      forAll(arbitrary[FeatureToggleName]) {
        name =>
          pathBindable.unbind("key", name) mustEqual name.asString
      }
    }
  }
}
