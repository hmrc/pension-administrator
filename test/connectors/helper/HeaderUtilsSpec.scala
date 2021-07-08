/*
 * Copyright 2021 HM Revenue & Customs
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

package connectors.helper

import config.AppConfig
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder

class HeaderUtilsSpec extends PlaySpec with MustMatchers {

  import HeaderUtilsSpec._

  "HeaderUtils" when {

    "call desHeader" must {

      "return all the appropriate headers" in {
        val result = headerUtils.desHeaderWithoutCorrelationId
        result mustEqual Seq("Environment" -> "local", "Authorization" -> "Bearer test-token",
          "Content-Type" -> "application/json")
      }
    }

    "call getCorrelationId" must {

      "return the correct CorrelationId when the request Id is more than 32 characters" in {
        val requestId = Some("govuk-tax-4725c811-9251-4c06-9b8f-f1d84659b2dfe")
        val result = headerUtils.getCorrelationId(requestId)
        result mustEqual "4725c81192514c069b8ff1d84659b2df"
      }

      "return the correct CorrelationId when the request Id is less than 32 characters" in {
        val requestId = Some("govuk-tax-4725c811-9251-4c06-9b8f-f1")
        val result = headerUtils.getCorrelationId(requestId)
        result mustEqual "4725c81192514c069b8ff1"
      }

      "return the correct CorrelationId when the request Id does not have gov-uk-tax or -" in {
        val requestId = Some("4725c81192514c069b8ff1")
        val result = headerUtils.getCorrelationId(requestId)
        result mustEqual "4725c81192514c069b8ff1"
      }
    }
  }
}

object HeaderUtilsSpec {
  private val app = new GuiceApplicationBuilder().configure(
    "microservice.services.des-hod.env" -> "local",
    "microservice.services.des-hod.authorizationToken" -> "test-token"
  ).build()
  private val injector = app.injector
  val appConfig: AppConfig = injector.instanceOf[AppConfig]
  val headerUtils = new HeaderUtils(appConfig)
}
