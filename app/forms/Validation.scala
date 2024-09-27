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

package forms

import scala.util.matching.Regex

object Validation {

  val textInputPattern: Regex = """[0-9A-Za-zÀ-ÅÇ-ÖØ-Ýà-åç-öø-ýÿĀ-ľŁ-ňŊ-őŔ-ſ'’ \-.,_&]+""".r.anchored
  val ukPostcodePattern: Regex = """[a-zA-Z]{1,2}[0-9][0-9a-zA-Z]? ?[0-9][a-zA-Z]{2}""".r.anchored
  val utrPattern: Regex = "[Kk]?(?:\\d{10}|\\d{13})[kK]?".r.anchored
  val chrnPattern: Regex = "(?:[A-Za-z]{2}|[Xx]) ?\\d{1,5}".r.anchored
  val crnPattern: Regex = "(?:[A-Za-z]{2}|\\d{2}) ?\\d{6}".r.anchored
  val emprefPattern: Regex = "\\d{3} ?/ ?[A-Za-z0-9]{5}".r.anchored
  val vrnPattern: Regex = "(?:GB)? ?\\d{9}".r.anchored
}
