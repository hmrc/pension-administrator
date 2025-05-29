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

package models.enumeration.binders

import models.enumeration.binders.EnumPathBinderSpec.DummyEnum
import org.scalatest.EitherValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EnumPathBinderSpec extends AnyWordSpec with Matchers with EitherValues {

  "EnumPathBinder" must {

    "bind to correct Enumeration" in {
      val actualResult = new EnumPathBinder(DummyEnum).pathBinder(DummyEnum).bind("test1", "test1")
      actualResult.value `mustBe` DummyEnum.test1
    }

    "not bind for incorrect Enumeration Value" in {
      val actualResult = new EnumPathBinder(DummyEnum).pathBinder(DummyEnum).bind("unknown", "unknown")
      actualResult.left.value `must` include("Unknown Enum Type")
    }

    "unbind to the correct Enumeration Value" in {
      val actualResult: String = new EnumPathBinder[DummyEnum.type](DummyEnum).pathBinder(DummyEnum).unbind("test1", DummyEnum.test1)
      actualResult `mustBe` DummyEnum.test1.toString
    }
  }
}

object EnumPathBinderSpec {

  object DummyEnum extends Enumeration {
    val test1, test2 = Value
  }

}
