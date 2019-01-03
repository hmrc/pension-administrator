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

package config

import org.scalatestplus.play.PlaySpec
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Configuration, Environment}

class FeatureSwitchManagementServiceSpec extends PlaySpec {

  def injector(isHubEnabled: Boolean): Injector = new GuiceApplicationBuilder()
    .configure(
      "features.is-hub-enabled" -> isHubEnabled
    ).build().injector

  private val injectorWithToggleOn = injector(true)

  private val config = injectorWithToggleOn.instanceOf[Configuration]
  private val environment = injectorWithToggleOn.instanceOf[Environment]

  "Feature Switch Management Service " when {
    "dynamic switch is disabled" must {

      "get the feature toggle value from the config" in {
        val fs = new FeatureSwitchManagementServiceProductionImpl(
          config, environment
        )
        fs.get("is-hub-enabled") mustEqual true
      }

      "change will get the changed feature toggle value from the config" in {
        val fs = new FeatureSwitchManagementServiceProductionImpl(
          injector(false).instanceOf[Configuration],
          injector(false).instanceOf[Environment]
        )

        fs.change("is-hub-enabled", newValue = false) mustEqual false
      }
    }

    "dynamic switch is enabled" must {

      "get the feature toggle value first time from the config" in {
        val fs = new FeatureSwitchManagementServiceTestImpl(
          config, environment
        )
        fs.get("is-hub-enabled") mustEqual true
      }

      "return the toggle value without doing anything if toggle doesn't exist in config" in {
        val injector = new GuiceApplicationBuilder().build().injector
        val fs = new FeatureSwitchManagementServiceTestImpl(
          injector.instanceOf[Configuration], injector.instanceOf[Environment]
        )
        fs.change("is-hub-enabled", newValue = false) mustEqual true
      }

      "change the feature toggle value from true to false" in {
        val fs = new FeatureSwitchManagementServiceTestImpl(
          config, environment
        )
        fs.change("is-hub-enabled", newValue = false) mustEqual true
      }

      "change the feature toggle value from false to true" in {
        val fs = new FeatureSwitchManagementServiceTestImpl(
          config, environment
        )
        fs.change("is-hub-enabled", newValue = true) mustEqual true
      }

      "reset will remove the feature toggle from the memory and get it from the config" in {
        val fs = new FeatureSwitchManagementServiceTestImpl(
          injector(false).instanceOf[Configuration],
          injector(false).instanceOf[Environment]
        )
        fs.reset("is-hub-enabled")
        fs.get("is-hub-enabled") mustEqual false
      }
    }
  }
}
