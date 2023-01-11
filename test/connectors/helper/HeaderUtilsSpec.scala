/*
 * Copyright 2023 HM Revenue & Customs
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
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import repositories._

class HeaderUtilsSpec extends PlaySpec with Matchers with MockitoSugar {

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
      "return a CorrelationId of the correct size" in {
        val result = headerUtils.getCorrelationId
        result.length mustEqual headerUtils.maxLengthCorrelationId
      }
    }

    "call getCorrelationIdIF" must {
      "return a CorrelationId of the correct size" in {
        val result = headerUtils.getCorrelationIdIF
        result.length mustEqual headerUtils.maxLengthCorrelationIdIF
      }
    }
  }
}

object HeaderUtilsSpec {
  private val app = new GuiceApplicationBuilder().configure(
    "microservice.services.des-hod.env" -> "local",
    "microservice.services.des-hod.authorizationToken" -> "test-token"
  ).overrides(Seq(
    bind[MinimalDetailsCacheRepository].toInstance(mock[MinimalDetailsCacheRepository]),
    bind[ManagePensionsDataCacheRepository].toInstance(mock[ManagePensionsDataCacheRepository]),
    bind[SessionDataCacheRepository].toInstance(mock[SessionDataCacheRepository]),
    bind[PSADataCacheRepository].toInstance(mock[PSADataCacheRepository]),
    bind[InvitationsCacheRepository].toInstance(mock[InvitationsCacheRepository]),
    bind[AdminDataRepository].toInstance(mock[AdminDataRepository])
  )).build()
  private val injector = app.injector
  val appConfig: AppConfig = injector.instanceOf[AppConfig]
  val headerUtils = new HeaderUtils(appConfig)
}
