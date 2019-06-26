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

package utils

import base.SpecBase
import org.joda.time.DateTime

class DateUtilsSpec extends SpecBase {
  "dateTimeFromDateToMidnightOnDay" should {
    "work correctly" in {
      val result = DateUtils.dateTimeFromDateToMidnightOnDay(
        DateTime.parse("2015-07-02T08:26:39.035Z"),
        2
      )
      result.getMillis mustBe 1436050800000L
    }
  }
}
