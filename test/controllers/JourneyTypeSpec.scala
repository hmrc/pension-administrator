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

package controllers

import models.enumeration.JourneyType
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.PathBindable
import utils.WireMockHelper

import scala.language.postfixOps

class JourneyTypeSpec
  extends PlaySpec
    with WireMockHelper
    with MockitoSugar {

  override protected def portConfigKeys: String = "microservice.services.pensions-scheme.port"

  val binder: PathBindable[JourneyType.Name] = implicitly

  "JourneyType PathBindable" should {
    List(
      ("PSA", JourneyType.PSA),
      ("PSAid", JourneyType.PSA),
      ("PSAInvite", JourneyType.INVITE),
      ("variation", JourneyType.VARIATION)
    ).foreach {
      (jtValue, jt) =>
        s"bind $jtValue to valid JourneyType - ${jt.toString}" in {
          binder.bind("journeyType", s"$jtValue").mustBe(Right(jt))
        }
    }

    "fail to bind invalid journey types" in {
      binder.bind("journeyType", "invalid").mustBe(Left("Invalid JourneyType"))
    }

    "unbind values correctly" in {
      binder.unbind("journeyType", JourneyType.PSA).mustBe("PSA")
      binder.unbind("journeyType", JourneyType.INVITE).mustBe("PSAInvite")
      binder.unbind("journeyType", JourneyType.VARIATION).mustBe("Variation")
    }
  }
}

