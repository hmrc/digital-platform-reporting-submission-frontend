/*
 * Copyright 2024 HM Revenue & Customs
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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Lang
import utils.DateTimeFormats.dateTimeFormat

import java.time.*

class DateTimeFormatsSpec extends AnyFreeSpec with Matchers {

  ".dateTimeFormat" - {

    "must format dates in English" in {
      val formatter = dateTimeFormat()(Lang("en"))
      val result = LocalDate.of(2023, 1, 1).format(formatter)
      result mustEqual "1 January 2023"
    }

    "must format dates in Welsh" in {
      val formatter = dateTimeFormat()(Lang("cy"))
      val result = LocalDate.of(2023, 1, 1).format(formatter)
      result mustEqual "1 Ionawr 2023"
    }

    "must default to English format" in {
      val formatter = dateTimeFormat()(Lang("de"))
      val result = LocalDate.of(2023, 1, 1).format(formatter)
      result mustEqual "1 January 2023"
    }
  }

  "fullDateTimeFormatter" - {

    "must format a BST date correctly" in {
      val dateTime = ZonedDateTime.of(2024, 6, 1, 13, 1, 0, 0, ZoneId.of("Europe/London"))
      DateTimeFormats.fullDateTimeFormatter.format(dateTime).replace("PM", "pm") mustEqual "1:01pm BST on 1 June 2024"
    }

    "must format a GMT date correctly" in {
      val dateTime = ZonedDateTime.of(2024, 1, 1, 13, 1, 0, 0, ZoneId.of("Europe/London"))
      DateTimeFormats.fullDateTimeFormatter.format(dateTime).replace("PM", "pm") mustEqual "1:01pm GMT on 1 January 2024"
    }
  }

  "formatInstant" - {

    "must correctly format an instant outside of BST" in {
      val dateTime = LocalDateTime.of(2024, 1, 1, 13, 1, 0, 0)
      val instant = dateTime.toInstant(ZoneOffset.UTC)
      DateTimeFormats.formatInstant(instant, DateTimeFormats.fullDateTimeFormatter).replace("PM", "pm") mustEqual "1:01pm GMT on 1 January 2024"
    }

    "must correctly format an instant during BST" in {
      val dateTime = LocalDateTime.of(2024, 6, 1, 13, 1, 0, 0)
      val instant = dateTime.toInstant(ZoneOffset.UTC)
      DateTimeFormats.formatInstant(instant, DateTimeFormats.fullDateTimeFormatter).replace("PM", "pm") mustEqual "2:01pm BST on 1 June 2024"
    }
  }
}
