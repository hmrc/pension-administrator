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
import play.api.Logger
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.{BackendController, FrontendController}

import scala.concurrent.ExecutionContext

class TestFeatureSwitchManagerController @Inject()(
                                                    fs: FeatureSwitchManagementService,
                                                    cc: ControllerComponents)(implicit val ec: ExecutionContext) extends BackendController(cc) {

  def toggleOn(featureSwitch: String): Action[AnyContent] = Action {
    implicit request =>
      val result = fs.change(featureSwitch, newValue = true)
      if (result){
        Logger.debug(s"[Pension-Administrator][ToggleOnSuccess] - ${featureSwitch}")
        NoContent
      } else{
        Logger.debug(s"[Pension-Administrator][ToggleOnFailed] - ${featureSwitch}")
        ExpectationFailed
      }
  }

  def toggleOff(featureSwitch: String): Action[AnyContent] = Action {
    implicit request =>
      val result = fs.change(featureSwitch, newValue = false)
      if (result){
        Logger.debug(s"[Pension-Administrator][ToggleOffSuccess] - ${featureSwitch}")
        NoContent
      } else{
        Logger.debug(s"[Pension-Administrator][ToggleOffFailed] - ${featureSwitch}")
        ExpectationFailed
      }
  }

  def reset(featureSwitch: String): Action[AnyContent] = Action {
    implicit request =>
      fs.reset(featureSwitch)
      Logger.debug(s"[Pension-Administrator][ToggleResetSuccess] - ${featureSwitch}")
      NoContent
  }
}
