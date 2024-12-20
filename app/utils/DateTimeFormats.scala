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

import play.api.i18n.Lang

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField
import java.time.{Instant, ZoneId, ZoneOffset}
import java.util.Locale

object DateTimeFormats {

  private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  private val localisedDateTimeFormatters = Map(
    "en" -> dateTimeFormatter,
    "cy" -> dateTimeFormatter.withLocale(new Locale("cy"))
  )

  def dateTimeFormat()(implicit lang: Lang): DateTimeFormatter = {
    localisedDateTimeFormatters.getOrElse(lang.code, dateTimeFormatter)
  }

  val dateTimeHintFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d M yyyy")

  val fullDateTimeFormatter: DateTimeFormatter = {

    val lookup = new java.util.HashMap[java.lang.Long, String]()
    lookup.put(0L, "am")
    lookup.put(1L, "pm")

    new DateTimeFormatterBuilder()
      .appendPattern("h:mm")
      .appendText(ChronoField.AMPM_OF_DAY, lookup)
      .appendPattern(" z 'on' d MMMM yyyy")
      .toFormatter()
  }

  val EmailDateTimeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("h:mma z 'on' d MMMM yyyy")
    .withZone(ZoneId.of("GMT"))

  def formatInstant(instant: Instant, formatter: DateTimeFormatter): String =
    formatter.format(instant.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("Europe/London")))
}
