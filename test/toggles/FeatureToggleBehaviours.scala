/*
 * Copyright 2019 HM Revenue & Customs
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

package toggles

import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder

class FeatureToggleBehaviours extends WordSpec with Matchers with GuiceOneAppPerTest {

  private def configuration(name: String, on: Option[Boolean]): Boolean = {

    val injector = new GuiceApplicationBuilder()
      .configure(on.fold ("features"->"")(b=> s"features.$name" -> b.toString)).build().injector

    injector.instanceOf[Configuration].getBoolean(s"features.$name").getOrElse(false)

  }

  def featureToggle(name: String, actualValue: Boolean): Unit = {

    "behave like a feature toggle" should {

      s"return true when $name is configured as true" in {
        configuration(name, Some(true)) shouldBe true
      }

      s"return false when $name is configured as false" in {
        configuration(name, Some(false)) shouldBe false
      }

      s"return false when $name is not configured" in {
        configuration(name, None) shouldBe false
      }

      s"return actual conf value" in {
        new GuiceApplicationBuilder().build().injector.instanceOf[Configuration].getBoolean(s"features.$name") shouldBe Some(actualValue)
      }

    }
  }
}
