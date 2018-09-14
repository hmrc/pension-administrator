/*
 * Copyright 2018 HM Revenue & Customs
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

import org.joda.time.DateTime
import org.scalatest.{Matchers, WordSpec}

class DateUtilsSpec extends WordSpec with Matchers{
  // scalastyle:off magic.number
  "thirtyDaysFromNowInSeconds" should {
    "respond correctly for a date at 1 second after midnight" in {
      DateUtils.secondsFromDateToMidnightOnDay(DateTime.parse("2018-01-04T00:00:01Z"), 30) shouldBe 2678399
    }
    "respond correctly for a date around the middle of the day" in {
      DateUtils.secondsFromDateToMidnightOnDay(DateTime.parse("2018-01-04T12:30:01Z"), 30) shouldBe 2633399
    }
    "respond correctly for a date at 1 second to midnight" in {
      DateUtils.secondsFromDateToMidnightOnDay(DateTime.parse("2018-01-04T23:59:59Z"), 30) shouldBe 2592001
    }
  }
}
