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

import com.google.inject.Inject
import config.FeatureSwitchManagementService
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

class TestFeatureSwitchManagerController @Inject()(
                                                    fs: FeatureSwitchManagementService)(implicit val ec: ExecutionContext) extends FrontendController {

  def toggleOn(featureSwitch: String): Action[AnyContent] = Action {
    implicit request =>
      val result = fs.change(featureSwitch, newValue = true)
      if (result) NoContent else ExpectationFailed
  }

  def toggleOff(featureSwitch: String): Action[AnyContent] = Action {
    implicit request =>
      val result = fs.change(featureSwitch, newValue = false)
      if (result) ExpectationFailed else NoContent
  }

  def reset(featureSwitch: String): Action[AnyContent] = Action {
    implicit request =>
      fs.reset(featureSwitch)
      NoContent
  }
}
