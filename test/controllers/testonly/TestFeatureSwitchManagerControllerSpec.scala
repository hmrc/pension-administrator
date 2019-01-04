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

package controllers.testonly

import base.SpecBase
import config.FeatureSwitchManagementService
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class TestFeatureSwitchManagerControllerSpec extends SpecBase with MockitoSugar {

  private val fakeFeatureSwitchManagerService = mock[FeatureSwitchManagementService]

  def controller: TestFeatureSwitchManagerController =
    new TestFeatureSwitchManagerController(
      fakeFeatureSwitchManagerService
    )

  "TestFeatureSwitchManager Controller" when {

    "toggle on is called" must {
      "return No Content if the toggle value is changed successfully" in {
        when(fakeFeatureSwitchManagerService.change("test-toggle", newValue = true)).thenReturn(true)
        val result = controller.toggleOn("test-toggle")(fakeRequest)
        status(result) mustBe NO_CONTENT
      }

      "return Expectation Failed if changing the toggle value is failed" in {
        when(fakeFeatureSwitchManagerService.change("test-toggle", newValue = true)).thenReturn(false)
        val result = controller.toggleOn("test-toggle")(fakeRequest)
        status(result) mustBe EXPECTATION_FAILED
      }
    }

    "toggle off is called" must {
      "return No Content if the toggle value is changed successfully" in {
        when(fakeFeatureSwitchManagerService.change("test-toggle", newValue = false)).thenReturn(true)
        val result = controller.toggleOff("test-toggle")(fakeRequest)
        status(result) mustBe NO_CONTENT
      }

      "return Expectation Failed if changing the toggle value is failed" in {
        when(fakeFeatureSwitchManagerService.change("test-toggle", newValue = false)).thenReturn(false)
        val result = controller.toggleOff("test-toggle")(fakeRequest)
        status(result) mustBe EXPECTATION_FAILED
      }
    }

    "return No Content when reset is done successfully" in {
      val result = controller.reset("test-toggle")(fakeRequest)
      status(result) mustBe NO_CONTENT
    }
  }
}
